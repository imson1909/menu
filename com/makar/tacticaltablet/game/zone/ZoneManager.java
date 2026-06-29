package com.makar.tacticaltablet.game.zone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.respawn.RespawnControlManager;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.LevelResource;

public final class ZoneManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final String CONFIG_FILE = "tacticaltablet_zone.json";
   private static final double DEFAULT_CENTER_X = 0.0;
   private static final double DEFAULT_CENTER_Z = 0.0;
   private static final int DEFAULT_RANDOM_RADIUS = 100;
   private static final int MAX_RANDOM_RADIUS = 10000;
   private static final Random RANDOM = new Random();
   private static final int BOSS_BAR_VISIBLE_SECONDS = 8;
   private static final ZoneManager.Phase[] PHASES = new ZoneManager.Phase[]{
      new ZoneManager.Phase(1, 360.0, 1, 180, false, "Фаза 1: зона 360 блоков.", "Фаза 1 - зона 360 блоков"),
      new ZoneManager.Phase(2, 260.0, 150, 210, false, "Фаза 2: зона сужается до 260 блоков.", "Фаза 2 - зона сужается до 260"),
      new ZoneManager.Phase(3, 180.0, 150, 210, false, "Фаза 3: зона сужается до 180 блоков.", "Фаза 3 - зона сужается до 180"),
      new ZoneManager.Phase(4, 110.0, 120, 180, false, "Фаза 4: зона сужается до 110 блоков.", "Фаза 4 - зона сужается до 110"),
      new ZoneManager.Phase(5, 50.0, 90, 150, true, "Фаза 5: возрождения отключены, зона сужается до 50 блоков.", "Фаза 5 - зона сужается до 50"),
      new ZoneManager.Phase(6, 1.0, 90, Integer.MAX_VALUE, true, "Финальная зона: граница сужается до 1 блока.", "Финальная зона - сужение до 1 блока")
   };
   private static int currentPhaseIndex = -1;
   private static int secondsLeft = 0;
   private static int bossBarSecondsLeft = 0;
   private static ServerBossEvent zoneBossBar;

   private ZoneManager() {
   }

   public static void start(MinecraftServer server) {
      currentPhaseIndex = -1;
      secondsLeft = 0;
      ZoneManager.ZoneSettings settings = loadSettings(server);
      applyConfiguredCenter(server, settings, true);
      applyPhase(server, 0);
   }

   public static void reset(MinecraftServer server) {
      currentPhaseIndex = -1;
      secondsLeft = 0;
      bossBarSecondsLeft = 0;
      hideBossBar();
      ServerLevel overworld = GameStateManager.getOverworld(server);
      if (overworld != null) {
         ZoneManager.ZoneSettings settings = loadSettings(server);
         WorldBorder border = overworld.m_6857_();
         border.m_61949_(settings.zoneCenterX, settings.zoneCenterZ);
         border.m_61917_(360.0);
         border.m_61939_(0.0);
         border.m_61947_(2.0);
         border.m_61952_(8);
         border.m_61944_(15);
      }
   }

   public static void tick(MinecraftServer server) {
      if (server != null && currentPhaseIndex >= 0) {
         syncBossBarPlayers(server);
         tickBossBar();
         if (secondsLeft != Integer.MAX_VALUE) {
            if (secondsLeft > 0) {
               secondsLeft--;
            } else {
               int nextPhase = currentPhaseIndex + 1;
               if (nextPhase < PHASES.length) {
                  applyPhase(server, nextPhase);
               }
            }
         }
      }
   }

   private static void applyPhase(MinecraftServer server, int phaseIndex) {
      ServerLevel overworld = GameStateManager.getOverworld(server);
      if (overworld != null && phaseIndex >= 0 && phaseIndex < PHASES.length) {
         ZoneManager.Phase phase = PHASES[phaseIndex];
         WorldBorder border = overworld.m_6857_();
         double currentSize = border.m_61959_();
         border.m_61939_(0.0);
         border.m_61947_(2.0);
         border.m_61952_(8);
         border.m_61944_(15);
         if (phase.transitionSeconds <= 1) {
            border.m_61917_(phase.size);
         } else {
            border.m_61919_(currentSize, phase.size, phase.transitionSeconds * 1000L);
         }

         currentPhaseIndex = phaseIndex;
         secondsLeft = phase.durationSeconds;
         if (phase.disableRespawns) {
            RespawnControlManager.disableRespawns(server);
         }

         showBossBar(server, phase);
         broadcast(server, "[WAR] " + phase.message);
         TacticalTabletMod.LOGGER
            .info(
               "Tactical Tablet zone phase {} started: targetSize={}, transition={}s, duration={}s",
               new Object[]{phase.number, phase.size, phase.transitionSeconds, phase.durationSeconds}
            );
      }
   }

   private static void showBossBar(MinecraftServer server, ZoneManager.Phase phase) {
      MutableComponent title = Component.m_237113_(phase.bossBarText);
      if (zoneBossBar == null) {
         zoneBossBar = new ServerBossEvent(title, BossBarColor.YELLOW, BossBarOverlay.PROGRESS);
      } else {
         zoneBossBar.m_6456_(title);
         zoneBossBar.m_6451_(phase.number >= 5 ? BossBarColor.RED : BossBarColor.YELLOW);
      }

      zoneBossBar.m_142711_(1.0F);
      zoneBossBar.m_8321_(true);
      bossBarSecondsLeft = 8;
      syncBossBarPlayers(server);
   }

   private static void syncBossBarPlayers(MinecraftServer server) {
      if (zoneBossBar != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            zoneBossBar.m_6543_(player);
         }
      }
   }

   private static void tickBossBar() {
      if (zoneBossBar != null && zoneBossBar.m_8323_()) {
         if (bossBarSecondsLeft <= 0) {
            zoneBossBar.m_8321_(false);
            zoneBossBar.m_7706_();
         } else {
            bossBarSecondsLeft--;
            float progress = Math.max(0.0F, Math.min(1.0F, bossBarSecondsLeft / 8.0F));
            zoneBossBar.m_142711_(progress);
         }
      }
   }

   private static void hideBossBar() {
      if (zoneBossBar != null) {
         bossBarSecondsLeft = 0;
         zoneBossBar.m_7706_();
         zoneBossBar.m_8321_(false);
      }
   }

   private static void applyConfiguredCenter(MinecraftServer server, ZoneManager.ZoneSettings settings, boolean randomize) {
      ServerLevel overworld = GameStateManager.getOverworld(server);
      if (overworld != null) {
         double x = settings.zoneCenterX;
         double z = settings.zoneCenterZ;
         if (randomize && settings.zoneRandomRadius > 0) {
            x += RANDOM.nextInt(settings.zoneRandomRadius * 2 + 1) - settings.zoneRandomRadius;
            z += RANDOM.nextInt(settings.zoneRandomRadius * 2 + 1) - settings.zoneRandomRadius;
         }

         WorldBorder border = overworld.m_6857_();
         border.m_61949_(x, z);
         TacticalTabletMod.LOGGER
            .info(
               "Tactical Tablet zone center selected by map config: baseX={}, baseZ={}, radius={}, finalX={}, finalZ={}",
               new Object[]{settings.zoneCenterX, settings.zoneCenterZ, settings.zoneRandomRadius, border.m_6347_(), border.m_6345_()}
            );
      }
   }

   private static ZoneManager.ZoneSettings loadSettings(MinecraftServer server) {
      ZoneManager.ZoneSettings defaults = ZoneManager.ZoneSettings.defaults();
      if (server == null) {
         return defaults;
      }

      Path configPath = server.m_129843_(LevelResource.f_78182_).toAbsolutePath().normalize().resolve("tacticaltablet_zone.json");
      if (!Files.exists(configPath)) {
         writeDefaultConfig(configPath, defaults);
         return defaults;
      }

      try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
         ZoneManager.ZoneSettings loaded = (ZoneManager.ZoneSettings)GSON.fromJson(reader, ZoneManager.ZoneSettings.class);
         return normalize(loaded);
      } catch (IOException | JsonSyntaxException exception) {
         TacticalTabletMod.LOGGER.error("Failed to read Tactical Tablet zone config at {}. Falling back to default center.", configPath, exception);
         return defaults;
      }
   }

   private static ZoneManager.ZoneSettings normalize(ZoneManager.ZoneSettings value) {
      ZoneManager.ZoneSettings defaults = ZoneManager.ZoneSettings.defaults();
      if (value == null) {
         return defaults;
      }

      if (value.zoneCenterX == null) {
         value.zoneCenterX = defaults.zoneCenterX;
      }

      if (value.zoneCenterZ == null) {
         value.zoneCenterZ = defaults.zoneCenterZ;
      }

      if (value.zoneRandomRadius == null) {
         value.zoneRandomRadius = defaults.zoneRandomRadius;
      }

      value.zoneRandomRadius = Math.max(0, Math.min(10000, value.zoneRandomRadius));
      return value;
   }

   private static void writeDefaultConfig(Path configPath, ZoneManager.ZoneSettings defaults) {
      try {
         Files.createDirectories(configPath.getParent());

         try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            GSON.toJson(defaults, writer);
         }

         TacticalTabletMod.LOGGER.info("Created default Tactical Tablet zone config at {}", configPath);
      } catch (IOException exception) {
         TacticalTabletMod.LOGGER.warn("Could not create default Tactical Tablet zone config at {}", configPath, exception);
      }
   }

   private static void broadcast(MinecraftServer server, String message) {
      Component component = Component.m_237113_(message);

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         player.m_213846_(component);
      }
   }

   private record Phase(int number, double size, int transitionSeconds, int durationSeconds, boolean disableRespawns, String message, String bossBarText) {
   }

   private static final class ZoneSettings {
      Double zoneCenterX;
      Double zoneCenterZ;
      Integer zoneRandomRadius;

      private static ZoneManager.ZoneSettings defaults() {
         ZoneManager.ZoneSettings settings = new ZoneManager.ZoneSettings();
         settings.zoneCenterX = 0.0;
         settings.zoneCenterZ = 0.0;
         settings.zoneRandomRadius = 100;
         return settings;
      }
   }
}
