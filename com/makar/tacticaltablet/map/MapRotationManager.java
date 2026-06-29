package com.makar.tacticaltablet.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.makar.tacticaltablet.core.TacticalTabletMod;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public class MapRotationManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final String ROTATION_DIRECTORY = "map_rotation";
   private static final String CONFIG_FILE = "config.json";
   private static final String STATE_FILE = "state.json";
   private static final String ARM_FILE = "rotate_on_shutdown.flag";
   private static final String DEFAULT_MAP_POOL = "map_pool";
   private static final String TEMP_WORLD_DIRECTORY = ".tacticaltablet_world_next";
   private static final String PREVIOUS_WORLD_PREFIX = ".tacticaltablet_world_previous_";
   private static final int DATA_VERSION = 1;
   private static final Random RANDOM = new Random();
   private static Path serverRoot;
   private static Path worldRoot;
   private static Path rotationRoot;
   private static Path configPath;
   private static Path statePath;
   private static Path armPath;
   private static MapRotationManager.RotationConfig config;
   private static MapRotationManager.RotationState state;

   private MapRotationManager() {
   }

   public static synchronized void onServerStarted(MinecraftServer server) {
      init(server);
      ensureStateSaved();
      TacticalTabletMod.LOGGER
         .info(
            "Tactical Tablet map rotation initialized. Enabled={}, mapsFolder={}, armed={}, nextMap={}",
            new Object[]{config.enabled, config.mapsFolder, isArmed(), getNextMapName()}
         );
   }

   public static synchronized void onServerStopped(MinecraftServer server) {
      init(server);
      if (!config.enabled) {
         TacticalTabletMod.LOGGER.info("Tactical Tablet map rotation skipped: disabled in config.");
      } else if (!config.rotateEveryShutdown && !isArmed()) {
         TacticalTabletMod.LOGGER.info("Tactical Tablet map rotation skipped: not armed for this shutdown.");
      } else {
         try {
            rotate(server);
            disarm();
         } catch (IOException | RuntimeException exception) {
            rememberError("Rotation failed: " + exception.getMessage());
            TacticalTabletMod.LOGGER.error("Tactical Tablet map rotation failed", exception);
         }
      }
   }

   public static synchronized MapRotationManager.RotationStatus getStatus(MinecraftServer server) {
      init(server);
      List<MapRotationManager.MapCandidate> maps = listCandidates();
      return new MapRotationManager.RotationStatus(
         config.enabled,
         config.rotateEveryShutdown,
         isArmed(),
         serverRoot,
         worldRoot,
         getMapsRoot(),
         maps.stream().map(MapRotationManager.MapCandidate::displayName).toList(),
         state.currentMap,
         getNextMapName(),
         state.lastRotation,
         state.lastError
      );
   }

   public static synchronized void reload(MinecraftServer server) {
      resetRuntime();
      init(server);
   }

   public static synchronized void resetRuntime() {
      serverRoot = null;
      worldRoot = null;
      rotationRoot = null;
      configPath = null;
      statePath = null;
      armPath = null;
      config = null;
      state = null;
   }

   public static synchronized void arm(MinecraftServer server) throws IOException {
      init(server);
      Files.createDirectories(rotationRoot);
      Files.writeString(armPath, Instant.now().toString(), StandardCharsets.UTF_8);
   }

   public static synchronized void disarm(MinecraftServer server) throws IOException {
      init(server);
      disarm();
   }

   public static synchronized boolean isArmed(MinecraftServer server) {
      init(server);
      return isArmed();
   }

   public static synchronized void setNextMap(MinecraftServer server, String mapName) throws IOException {
      init(server);
      String normalized = normalizeMapName(mapName);
      MapRotationManager.MapCandidate candidate = findCandidate(normalized);
      if (candidate == null) {
         throw new IOException("Map not found in " + getMapsRoot() + ": " + mapName);
      }

      state.nextMapOverride = candidate.displayName();
      state.lastError = "";
      writeJsonAtomically(statePath, state);
   }

   public static synchronized void clearNextMapOverride(MinecraftServer server) throws IOException {
      init(server);
      state.nextMapOverride = "";
      writeJsonAtomically(statePath, state);
   }

   public static synchronized List<String> listMapNames(MinecraftServer server) {
      init(server);
      return listCandidates().stream().map(MapRotationManager.MapCandidate::displayName).toList();
   }

   private static void rotate(MinecraftServer server) throws IOException {
      List<MapRotationManager.MapCandidate> maps = listCandidates();
      if (maps.isEmpty()) {
         throw new IOException("No valid maps found in " + getMapsRoot());
      }

      MapRotationManager.MapCandidate selected = selectNextMap(maps);
      Path tempWorld = serverRoot.resolve(".tacticaltablet_world_next");
      Path previousWorld = serverRoot.resolve(".tacticaltablet_world_previous_" + System.currentTimeMillis());
      TacticalTabletMod.LOGGER.info("Preparing next Tactical Tablet map: {}", selected.displayName());
      deleteRecursively(tempWorld);
      prepareWorld(selected, tempWorld);
      validateWorld(tempWorld);
      ensureSafeWorldPath(worldRoot);

      try {
         if (Files.exists(worldRoot)) {
            moveDirectory(worldRoot, previousWorld);
         }

         moveDirectory(tempWorld, worldRoot);
         finishRotation(selected, maps);
         TacticalTabletMod.LOGGER.info("Tactical Tablet map rotation complete. New map: {}", selected.displayName());
      } catch (IOException exception) {
         restorePreviousWorld(previousWorld);
         throw exception;
      } finally {
         deleteRecursively(tempWorld);
         deleteRecursively(previousWorld);
      }
   }

   private static MapRotationManager.MapCandidate selectNextMap(List<MapRotationManager.MapCandidate> maps) throws IOException {
      if (state.nextMapOverride != null && !state.nextMapOverride.isBlank()) {
         MapRotationManager.MapCandidate override = findCandidate(state.nextMapOverride, maps);
         if (override == null) {
            throw new IOException("Configured next map no longer exists: " + state.nextMapOverride);
         } else {
            return override;
         }
      } else {
         if ("random".equalsIgnoreCase(config.rotationMode)) {
            return maps.get(RANDOM.nextInt(maps.size()));
         }

         int index = Math.floorMod(state.nextIndex, maps.size());
         return maps.get(index);
      }
   }

   private static void finishRotation(MapRotationManager.MapCandidate selected, List<MapRotationManager.MapCandidate> maps) throws IOException {
      int selectedIndex = maps.indexOf(selected);
      state.dataVersion = 1;
      state.currentMap = selected.displayName();
      state.nextMapOverride = "";
      state.lastRotation = Instant.now().toString();
      state.lastError = "";
      if (selectedIndex >= 0) {
         state.nextIndex = Math.floorMod(selectedIndex + 1, maps.size());
      } else {
         state.nextIndex = 0;
      }

      writeJsonAtomically(statePath, state);
   }

   private static void rememberError(String message) {
      if (state != null && statePath != null) {
         state.lastError = message == null ? "" : message;

         try {
            writeJsonAtomically(statePath, state);
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to write Tactical Tablet map rotation error state", exception);
         }
      }
   }

   private static void prepareWorld(MapRotationManager.MapCandidate candidate, Path target) throws IOException {
      Files.createDirectories(target);
      if (candidate.zip()) {
         extractZipMap(candidate.path(), target);
      } else {
         Path sourceRoot = resolveDirectoryMapRoot(candidate.path());
         if (sourceRoot == null) {
            throw new IOException("Invalid map folder, level.dat was not found: " + candidate.path());
         }

         copyDirectory(sourceRoot, target);
      }
   }

   private static void extractZipMap(Path zipPath, Path target) throws IOException {
      String rootPrefix = findZipMapRootPrefix(zipPath);
      if (rootPrefix == null) {
         throw new IOException("Invalid map zip, level.dat was not found: " + zipPath);
      }

      ZipEntry entry;
      try (ZipInputStream input = new ZipInputStream(Files.newInputStream(zipPath))) {
         while ((entry = input.getNextEntry()) != null) {
            String name = entry.getName().replace('\\', '/');
            if (!rootPrefix.isEmpty()) {
               if (!name.startsWith(rootPrefix)) {
                  continue;
               }

               name = name.substring(rootPrefix.length());
            }

            if (!name.isBlank()) {
               Path output = target.resolve(name).normalize();
               if (!output.startsWith(target)) {
                  throw new IOException("Blocked unsafe zip entry: " + entry.getName());
               }

               if (entry.isDirectory()) {
                  Files.createDirectories(output);
               } else {
                  Path parent = output.getParent();
                  if (parent != null) {
                     Files.createDirectories(parent);
                  }

                  Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
               }

               input.closeEntry();
            }
         }
      }
   }

   private static List<MapRotationManager.MapCandidate> listCandidates() {
      Path mapsRoot = getMapsRoot();
      List<MapRotationManager.MapCandidate> maps = new ArrayList<>();
      if (!Files.isDirectory(mapsRoot)) {
         return maps;
      }

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(mapsRoot)) {
         for (Path candidate : stream) {
            if (Files.isDirectory(candidate) && resolveDirectoryMapRoot(candidate) != null) {
               maps.add(new MapRotationManager.MapCandidate(candidate, candidate.getFileName().toString(), false));
            } else if (config.allowZipMaps && Files.isRegularFile(candidate) && isZip(candidate) && findZipMapRootPrefix(candidate) != null) {
               maps.add(new MapRotationManager.MapCandidate(candidate, stripZipExtension(candidate.getFileName().toString()), true));
            }
         }
      } catch (IOException exception) {
         TacticalTabletMod.LOGGER.error("Failed to scan map pool {}", mapsRoot, exception);
      }

      maps.sort(Comparator.comparing(MapRotationManager.MapCandidate::displayName, String.CASE_INSENSITIVE_ORDER));
      return maps;
   }

   private static MapRotationManager.MapCandidate findCandidate(String mapName) {
      return findCandidate(mapName, listCandidates());
   }

   private static MapRotationManager.MapCandidate findCandidate(String mapName, List<MapRotationManager.MapCandidate> maps) {
      String normalized = normalizeMapName(mapName);

      for (MapRotationManager.MapCandidate candidate : maps) {
         if (normalizeMapName(candidate.displayName()).equals(normalized) || normalizeMapName(candidate.path().getFileName().toString()).equals(normalized)) {
            return candidate;
         }
      }

      return null;
   }

   private static Path resolveDirectoryMapRoot(Path candidate) {
      if (Files.isRegularFile(candidate.resolve("level.dat"))) {
         return candidate;
      }

      if (config.unwrapSingleWorldFolder && Files.isDirectory(candidate)) {
         try (DirectoryStream<Path> stream = Files.newDirectoryStream(candidate)) {
            List<Path> childDirectories = new ArrayList<>();

            for (Path child : stream) {
               if (Files.isDirectory(child)) {
                  childDirectories.add(child);
               }
            }

            if (childDirectories.size() == 1) {
               Path nested = childDirectories.get(0);
               if (Files.isRegularFile(nested.resolve("level.dat"))) {
                  return nested;
               }
            }
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.warn("Failed to inspect map folder {}", candidate, exception);
         }

         return null;
      } else {
         return null;
      }
   }

   private static String findZipMapRootPrefix(Path zipPath) {
      Set<String> topLevelFolders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      boolean rootHasLevelDat = false;

      ZipEntry entry;
      try (ZipInputStream input = new ZipInputStream(Files.newInputStream(zipPath))) {
         for (; (entry = input.getNextEntry()) != null; input.closeEntry()) {
            String name = entry.getName().replace('\\', '/');
            if (name.equals("level.dat")) {
               rootHasLevelDat = true;
            } else {
               int slash = name.indexOf(47);
               if (slash > 0) {
                  topLevelFolders.add(name.substring(0, slash));
               }
            }
         }
      } catch (IOException exception) {
         TacticalTabletMod.LOGGER.warn("Failed to inspect map zip {}", zipPath, exception);
         return null;
      }

      if (rootHasLevelDat) {
         return "";
      }

      if (topLevelFolders.size() != 1) {
         return null;
      }

      String folder = topLevelFolders.iterator().next();
      String levelDat = folder + "/level.dat";

      ZipEntry entryx;
      try (ZipInputStream input = new ZipInputStream(Files.newInputStream(zipPath))) {
         while ((entryx = input.getNextEntry()) != null) {
            if (entryx.getName().replace('\\', '/').equals(levelDat)) {
               input.closeEntry();
               return folder + "/";
            }

            input.closeEntry();
         }
      } catch (IOException exception) {
         TacticalTabletMod.LOGGER.warn("Failed to inspect map zip {}", zipPath, exception);
      }

      return null;
   }

   private static void copyDirectory(final Path source, final Path target) throws IOException {
      Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
         public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
            Path relative = source.relativize(directory);
            Files.createDirectories(target.resolve(relative));
            return FileVisitResult.CONTINUE;
         }

         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path relative = source.relativize(file);
            Files.copy(file, target.resolve(relative), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return FileVisitResult.CONTINUE;
         }
      });
   }

   private static void moveDirectory(Path source, Path target) throws IOException {
      try {
         Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException exception) {
         Files.move(source, target);
      } catch (FileAlreadyExistsException exception) {
         deleteRecursively(target);
         Files.move(source, target);
      }
   }

   private static void restorePreviousWorld(Path previousWorld) {
      if (Files.exists(previousWorld)) {
         try {
            if (Files.exists(worldRoot)) {
               deleteRecursively(worldRoot);
            }

            moveDirectory(previousWorld, worldRoot);
            TacticalTabletMod.LOGGER.warn("Restored previous world after failed map rotation.");
         } catch (IOException restoreException) {
            TacticalTabletMod.LOGGER.error("Failed to restore previous world after map rotation failure", restoreException);
         }
      }
   }

   private static void deleteRecursively(Path path) throws IOException {
      if (path != null && Files.exists(path)) {
         Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
               Files.deleteIfExists(file);
               return FileVisitResult.CONTINUE;
            }

            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
               if (exception != null) {
                  throw exception;
               }

               Files.deleteIfExists(directory);
               return FileVisitResult.CONTINUE;
            }
         });
      }
   }

   private static void validateWorld(Path path) throws IOException {
      if (!Files.isRegularFile(path.resolve("level.dat"))) {
         throw new IOException("Prepared world is missing level.dat: " + path);
      }
   }

   private static void ensureSafeWorldPath(Path path) throws IOException {
      Path normalized = path.toAbsolutePath().normalize();
      if (!Objects.equals(normalized.getParent(), serverRoot)) {
         throw new IOException("Refusing to rotate unsafe world path: " + normalized);
      } else {
         String name = normalized.getFileName().toString().toLowerCase(Locale.ROOT);
         if (name.equals("tacticaltablet_data")
            || name.equals("map_rotation")
            || name.equals(config.mapsFolder.toLowerCase(Locale.ROOT))
            || name.equals("mods")
            || name.equals("config")) {
            throw new IOException("Refusing to rotate protected folder: " + normalized);
         }
      }
   }

   private static void init(MinecraftServer server) {
      if (server == null) {
         throw new IllegalStateException("MinecraftServer is required for map rotation.");
      }

      if (serverRoot == null || worldRoot == null || config == null || state == null) {
         worldRoot = server.m_129843_(LevelResource.f_78182_).toAbsolutePath().normalize();
         serverRoot = worldRoot.getParent();
         if (serverRoot == null) {
            serverRoot = worldRoot;
         }

         rotationRoot = serverRoot.resolve("map_rotation");
         configPath = rotationRoot.resolve("config.json");
         statePath = rotationRoot.resolve("state.json");
         armPath = rotationRoot.resolve("rotate_on_shutdown.flag");

         try {
            Files.createDirectories(rotationRoot);
            config = readOrCreateConfig();
            state = readOrCreateState();
            Files.createDirectories(getMapsRoot());
            writeJsonAtomically(configPath, config);
            writeJsonAtomically(statePath, state);
         } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize Tactical Tablet map rotation.", exception);
         }
      }
   }

   private static MapRotationManager.RotationConfig readOrCreateConfig() throws IOException {
      if (!Files.exists(configPath)) {
         return new MapRotationManager.RotationConfig();
      }

      try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
         MapRotationManager.RotationConfig loaded = (MapRotationManager.RotationConfig)GSON.fromJson(reader, MapRotationManager.RotationConfig.class);
         return loaded == null ? new MapRotationManager.RotationConfig() : normalizeConfig(loaded);
      } catch (JsonSyntaxException exception) {
         Path broken = configPath.resolveSibling("config.json.broken-" + System.currentTimeMillis());
         Files.move(configPath, broken, StandardCopyOption.REPLACE_EXISTING);
         TacticalTabletMod.LOGGER.error("Broken map rotation config moved to {}", broken, exception);
         return new MapRotationManager.RotationConfig();
      }
   }

   private static MapRotationManager.RotationState readOrCreateState() throws IOException {
      if (!Files.exists(statePath)) {
         return new MapRotationManager.RotationState();
      }

      try (Reader reader = Files.newBufferedReader(statePath, StandardCharsets.UTF_8)) {
         MapRotationManager.RotationState loaded = (MapRotationManager.RotationState)GSON.fromJson(reader, MapRotationManager.RotationState.class);
         return loaded == null ? new MapRotationManager.RotationState() : normalizeState(loaded);
      } catch (JsonSyntaxException exception) {
         Path broken = statePath.resolveSibling("state.json.broken-" + System.currentTimeMillis());
         Files.move(statePath, broken, StandardCopyOption.REPLACE_EXISTING);
         TacticalTabletMod.LOGGER.error("Broken map rotation state moved to {}", broken, exception);
         return new MapRotationManager.RotationState();
      }
   }

   private static MapRotationManager.RotationConfig normalizeConfig(MapRotationManager.RotationConfig value) {
      value.dataVersion = 1;
      if (value.mapsFolder == null || value.mapsFolder.isBlank()) {
         value.mapsFolder = "map_pool";
      }

      if (value.rotationMode == null || value.rotationMode.isBlank()) {
         value.rotationMode = "round_robin";
      }

      String normalizedMode = value.rotationMode.toLowerCase(Locale.ROOT);
      if (!normalizedMode.equals("round_robin") && !normalizedMode.equals("random")) {
         value.rotationMode = "round_robin";
      }

      return value;
   }

   private static MapRotationManager.RotationState normalizeState(MapRotationManager.RotationState value) {
      value.dataVersion = 1;
      value.currentMap = nullToEmpty(value.currentMap);
      value.nextMapOverride = nullToEmpty(value.nextMapOverride);
      value.lastRotation = nullToEmpty(value.lastRotation);
      value.lastError = nullToEmpty(value.lastError);
      value.nextIndex = Math.max(0, value.nextIndex);
      return value;
   }

   private static void ensureStateSaved() {
      try {
         writeJsonAtomically(statePath, state);
      } catch (IOException exception) {
         TacticalTabletMod.LOGGER.error("Failed to save map rotation state", exception);
      }
   }

   private static void disarm() throws IOException {
      Files.deleteIfExists(armPath);
   }

   private static boolean isArmed() {
      return Files.exists(armPath);
   }

   private static String getNextMapName() {
      List<MapRotationManager.MapCandidate> maps = listCandidates();
      if (maps.isEmpty()) {
         return "";
      }

      try {
         return selectNextMap(maps).displayName();
      } catch (IOException exception) {
         return state.nextMapOverride == null ? "" : state.nextMapOverride;
      }
   }

   private static Path getMapsRoot() {
      return serverRoot.resolve(config.mapsFolder).normalize();
   }

   private static boolean isZip(Path path) {
      return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
   }

   private static String stripZipExtension(String name) {
      return name.toLowerCase(Locale.ROOT).endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
   }

   private static String normalizeMapName(String name) {
      return stripZipExtension(nullToEmpty(name).trim()).toLowerCase(Locale.ROOT);
   }

   private static String nullToEmpty(String value) {
      return value == null ? "" : value;
   }

   private static void writeJsonAtomically(Path file, Object value) throws IOException {
      Files.createDirectories(file.getParent());
      Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

      try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
         GSON.toJson(value, writer);
      }

      try {
         Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException exception) {
         Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private record MapCandidate(Path path, String displayName, boolean zip) {
   }

   private static final class RotationConfig {
      int dataVersion = 1;
      boolean enabled = true;
      boolean rotateEveryShutdown = false;
      String mapsFolder = "map_pool";
      String rotationMode = "round_robin";
      boolean allowZipMaps = true;
      boolean unwrapSingleWorldFolder = true;
   }

   private static final class RotationState {
      int dataVersion = 1;
      int nextIndex = 0;
      String currentMap = "";
      String nextMapOverride = "";
      String lastRotation = "";
      String lastError = "";
   }

   public record RotationStatus(
      boolean enabled,
      boolean rotateEveryShutdown,
      boolean armed,
      Path serverRoot,
      Path worldRoot,
      Path mapsRoot,
      List<String> maps,
      String currentMap,
      String nextMap,
      String lastRotation,
      String lastError
   ) {
   }
}
