package com.makar.tacticaltablet.integration.online;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.makar.tacticaltablet.core.TacticalTabletMod;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class OnlineWebhookConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final String CONFIG_FILE = "tacticaltablet_online_webhook.json";
   private static OnlineWebhookConfig current;
   private static Path configPath;
   private boolean enabled = true;
   private String webhookUrl = "";
   private String displayName = "DeluxeWarfare Онлайн";
   private int updateIntervalSeconds = 60;
   private int restartIntervalMinutes = 60;
   private boolean showPlayerNames = true;
   private int maxDisplayedPlayers = 30;
   private String messageId = "";

   private OnlineWebhookConfig() {
   }

   public static synchronized OnlineWebhookConfig get(MinecraftServer server) {
      if (current == null) {
         reload(server);
      }

      return current;
   }

   public static synchronized OnlineWebhookConfig reload(MinecraftServer server) {
      Path serverRoot = resolveServerRoot(server);
      Path configRoot = serverRoot.resolve("config");
      configPath = configRoot.resolve("tacticaltablet_online_webhook.json");
      OnlineWebhookConfig config = null;

      try {
         Files.createDirectories(configRoot);
         if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
               config = (OnlineWebhookConfig)GSON.fromJson(reader, OnlineWebhookConfig.class);
            }
         }

         if (config == null) {
            config = new OnlineWebhookConfig();
            write(configPath, config);
         } else if (config.normalize()) {
            write(configPath, config);
         }
      } catch (JsonSyntaxException exception) {
         TacticalTabletMod.LOGGER.error("Online webhook config is invalid: {}", configPath, exception);
         config = new OnlineWebhookConfig();
      } catch (IOException exception) {
         TacticalTabletMod.LOGGER.error("Failed to load online webhook config: {}", configPath, exception);
         config = new OnlineWebhookConfig();
      }

      config.normalize();
      current = config;
      return current;
   }

   public static synchronized void saveCurrent() {
      if (current != null && configPath != null) {
         try {
            current.normalize();
            write(configPath, current);
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to save online webhook config: {}", configPath, exception);
         }
      }
   }

   public static synchronized void setMessageId(String messageId) {
      if (current != null) {
         current.messageId = messageId == null ? "" : messageId.trim();
         saveCurrent();
      }
   }

   public static synchronized void clearMessageId() {
      setMessageId("");
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public String getWebhookUrl() {
      return this.webhookUrl == null ? "" : this.webhookUrl.trim();
   }

   public String getDisplayName() {
      String value = this.displayName == null ? "" : this.displayName.trim();
      return value.isBlank() ? "DeluxeWarfare Онлайн" : value;
   }

   public int getUpdateIntervalSeconds() {
      return this.updateIntervalSeconds;
   }

   public int getRestartIntervalMinutes() {
      return this.restartIntervalMinutes;
   }

   public boolean isShowPlayerNames() {
      return this.showPlayerNames;
   }

   public int getMaxDisplayedPlayers() {
      return this.maxDisplayedPlayers;
   }

   public String getMessageId() {
      return this.messageId == null ? "" : this.messageId.trim();
   }

   public boolean hasWebhook() {
      return !this.getWebhookUrl().isBlank();
   }

   public boolean hasMessageId() {
      return !this.getMessageId().isBlank();
   }

   private boolean normalize() {
      boolean changed = false;
      if (this.webhookUrl == null) {
         this.webhookUrl = "";
         changed = true;
      }

      if (this.displayName == null || this.displayName.trim().isBlank()) {
         this.displayName = "DeluxeWarfare Онлайн";
         changed = true;
      }

      if (this.messageId == null) {
         this.messageId = "";
         changed = true;
      }

      int normalizedInterval = Math.max(15, Math.min(3600, this.updateIntervalSeconds));
      if (normalizedInterval != this.updateIntervalSeconds) {
         this.updateIntervalSeconds = normalizedInterval;
         changed = true;
      }

      int normalizedRestartInterval = Math.max(0, Math.min(1440, this.restartIntervalMinutes));
      if (normalizedRestartInterval != this.restartIntervalMinutes) {
         this.restartIntervalMinutes = normalizedRestartInterval;
         changed = true;
      }

      int normalizedMaxPlayers = Math.max(0, Math.min(100, this.maxDisplayedPlayers));
      if (normalizedMaxPlayers != this.maxDisplayedPlayers) {
         this.maxDisplayedPlayers = normalizedMaxPlayers;
         changed = true;
      }

      return changed;
   }

   private static void write(Path file, OnlineWebhookConfig config) throws IOException {
      try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
         GSON.toJson(config, writer);
      }
   }

   private static Path resolveServerRoot(MinecraftServer server) {
      if (server == null) {
         return Path.of(".").toAbsolutePath().normalize();
      }

      Path worldRoot = server.m_129843_(LevelResource.f_78182_).toAbsolutePath().normalize();
      Path serverRoot = worldRoot.getParent();
      return serverRoot == null ? worldRoot : serverRoot;
   }
}
