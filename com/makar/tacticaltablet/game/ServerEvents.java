package com.makar.tacticaltablet.game;

import com.makar.tacticaltablet.admin.TestModeManager;
import com.makar.tacticaltablet.airdrop.AirdropManager;
import com.makar.tacticaltablet.anticheat.AntiCheatManager;
import com.makar.tacticaltablet.anticheat.MovementAntiCheat;
import com.makar.tacticaltablet.anticheat.Severity;
import com.makar.tacticaltablet.anticheat.ViolationType;
import com.makar.tacticaltablet.client.NameTagManager;
import com.makar.tacticaltablet.corpse.CorpseLootManager;
import com.makar.tacticaltablet.game.clanwar.ClanWarManager;
import com.makar.tacticaltablet.game.contract.ContractManager;
import com.makar.tacticaltablet.game.extraction.ExtractionPointManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.lobby.LobbyManager;
import com.makar.tacticaltablet.game.respawn.DeathTransitionManager;
import com.makar.tacticaltablet.game.respawn.RtpTimerManager;
import com.makar.tacticaltablet.game.team.TeamId;
import com.makar.tacticaltablet.game.team.TeamMatchManager;
import com.makar.tacticaltablet.game.teleport.SafeTeleport;
import com.makar.tacticaltablet.game.zone.ZoneManager;
import com.makar.tacticaltablet.integration.discord.DiscordLeaderboardService;
import com.makar.tacticaltablet.integration.discord.DiscordWebhookClient;
import com.makar.tacticaltablet.integration.discord.LeaderboardScheduler;
import com.makar.tacticaltablet.integration.online.OnlineWebhookService;
import com.makar.tacticaltablet.inventory.InventoryGuard;
import com.makar.tacticaltablet.inventory.InventoryLockEvents;
import com.makar.tacticaltablet.inventory.InventoryManager;
import com.makar.tacticaltablet.map.MapRotationManager;
import com.makar.tacticaltablet.progression.ClassCooldownManager;
import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.progression.PassiveClassXPManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.makar.tacticaltablet.progression.XpNotifier;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import com.makar.tacticaltablet.tablet.net.TabletPacket;
import com.makar.tacticaltablet.voice.VoiceChatTeamManager;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerEvents {
   private static final double MAX_MELEE_REACH = 5.5;
   private static final int TEAM_KILL_BAN_THRESHOLD = 3;
   private static final long TEAM_KILL_BAN_MILLIS = 900000L;
   private static int utilityTickCounter = 0;

   @SubscribeEvent
   public static void onPlayerJoin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         PlayerProgressManager.loadPlayer(player);
         TeamMatchManager.rememberPlayer(player);
         NameTagManager.applyToAll(player.f_8924_);
         if (LivesManager.ensureEliminatedIfOutOfLives(player)) {
            TeamMatchManager.applyScoreboardTeams(player.f_8924_);
            ClassXPManager.sync(player);
            player.f_8924_.execute(() -> GameStateManager.checkForMatchEnd(player.f_8924_));
            return;
         }

         if (GameStateManager.isRunning(player.f_8924_)
            && GameStateManager.getMatchPhase() == MatchPhase.RUNNING
            && GameStateManager.getCurrentMode().isTeamMode()) {
            if (MapSetManager.isClanWarSet() && !ClanWarManager.hasClan(player)) {
               player.m_20137_("war.eliminated");
               player.m_20137_("war.playing");
               player.m_20137_("in_lobby");
               InventoryManager.clearInventory(player);
               player.m_143403_(GameType.SPECTATOR);
               ClanWarManager.showNeedClan(player);
               MapSetManager.sync(player, MapSetManager.isVoting());
               ClassXPManager.sync(player);
               return;
            }

            TeamId team = MapSetManager.isClanWarSet()
               ? TeamMatchManager.assignClanWarPlayer(player.f_8924_, player)
               : TeamMatchManager.assignLateJoiner(player.f_8924_, player, GameStateManager.getCurrentMode());
            if (team != null) {
               LivesManager.ensureStarted(player);
               VoiceChatTeamManager.assignPlayerToVoiceGroup(player);
               player.m_213846_(Component.m_237113_("[WAR] Вы присоединены к команде " + team.displayName() + ".").m_130940_(team.chatColor()));
            }
         }

         LobbyManager.moveToLobby(player);
         MapSetManager.sync(player, MapSetManager.isVoting());
         ContractManager.ensureTracker(player);
         ContractManager.giveSelectionTrackerIfAvailable(player);
         TeamMatchManager.applyScoreboardTeams(player.f_8924_);
         ClassXPManager.sync(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerRespawn(PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         PlayerProgressManager.loadPlayer(player);
         if (DeathTransitionManager.begin(player)) {
            return;
         }

         if (LivesManager.ensureEliminatedIfOutOfLives(player)) {
            ClassXPManager.sync(player);
            return;
         }

         LobbyManager.moveToLobby(player);
         ExtractionPointManager.onPlayerRespawn(player);
         VoiceChatTeamManager.assignPlayerToVoiceGroup(player);
         ClassXPManager.sync(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerGameModeChange(PlayerChangeGameModeEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (event.getNewGameMode() == GameType.SPECTATOR) {
            if (GameStateManager.isRunning(player.f_8924_)) {
               if (GameStateManager.getCurrentMode().isTeamMode()) {
                  VoiceChatTeamManager.removePlayerFromVoiceGroup(player);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerClone(Clone event) {
      if (event.getEntity() instanceof ServerPlayer newPlayer) {
         if (event.getOriginal() instanceof ServerPlayer oldPlayer) {
            PlayerProgressManager.loadPlayer(newPlayer);

            for (String tag : oldPlayer.m_19880_()) {
               if (tag.equals("war.lives_init") || tag.equals("war.eliminated")) {
                  newPlayer.m_20049_(tag);
               }
            }

            PlayerTabletState.reset(newPlayer);
         }
      }
   }

   @SubscribeEvent
   public static void onLivingHurt(LivingHurtEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (GameStateManager.getMatchPhase() == MatchPhase.POST_GAME) {
            event.setCanceled(true);
            event.setAmount(0.0F);
         } else {
            checkCombatReach(player, event.getSource());
            Set<String> tags = player.m_19880_();
            boolean inLobby = GameStateManager.isInLobby(player) || tags.contains("in_lobby");
            boolean playing = tags.contains("war.playing");
            if (inLobby && !playing) {
               event.setCanceled(true);
               event.setAmount(0.0F);
            } else if (event.getSource().m_7639_() instanceof ServerPlayer attacker) {
               if (!attacker.m_20148_().equals(player.m_20148_())) {
                  if (attacker.m_19880_().contains("war.playing") && playing) {
                     String sourceText = safeLower(event.getSource().m_19385_())
                        + " "
                        + entityId(event.getSource().m_7640_())
                        + " "
                        + entityId(event.getSource().m_7639_());
                     boolean friendlyDamage = GameStateManager.getCurrentMode().isTeamMode() && TeamMatchManager.areTeammates(attacker, player);
                     if (friendlyDamage) {
                        if (isTeamTrapDamage(sourceText)) {
                           event.setCanceled(true);
                           event.setAmount(0.0F);
                        }
                     } else {
                        if (event.getAmount() > 0.0F) {
                           DiscordLeaderboardService.recordMatchDamage(attacker, event.getAmount());
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLivingDrops(LivingDropsEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && player.m_19880_().contains("war.playing")) {
         event.getDrops().clear();
      }
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         PlayerProgressManager.tick(event.getServer());
         LeaderboardScheduler.tick(event);
         OnlineWebhookService.tick(event);
         PassiveClassXPManager.tick(event.getServer());
         RtpTimerManager.tick(event.getServer());
         ContractManager.tick(event.getServer());
         ExtractionPointManager.tick(event.getServer());
         DeathTransitionManager.tick(event.getServer());
         SpectatorCameraManager.onServerTick(event.getServer());
         GameStateManager.onServerTick(event.getServer());
         InventoryGuard.tick(event.getServer());
         MovementAntiCheat.tick(event.getServer());
         if (++utilityTickCounter >= 100) {
            utilityTickCounter = 0;
            LobbyManager.keepLobbyWeatherClear(event.getServer());
            NameTagManager.applyToAll(event.getServer());
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         boolean runningMatchParticipant = GameStateManager.isRunning(player.f_8924_)
            && GameStateManager.getMatchPhase() == MatchPhase.RUNNING
            && LivesManager.isAliveParticipant(player);
         PlayerProgressManager.savePlayer(player);
         DeathTransitionManager.clear(player);
         ContractManager.onPlayerDisconnect(player);
         ExtractionPointManager.onPlayerDeathOrLogout(player);
         if (runningMatchParticipant) {
            LivesManager.handleDeath(player);
            PlayerTabletState.reset(player);
         } else {
            RtpTimerManager.cancel(player);
            PassiveClassXPManager.clear(player);
            PlayerTabletState.reset(player);
         }

         TabletPacket.reset(player);
         AntiCheatManager.reset(player);
         MovementAntiCheat.reset(player);
         if (!GameStateManager.getCurrentMode().isTeamMode()) {
            NameTagManager.remove(player);
         }

         if (!runningMatchParticipant) {
            player.m_20137_("war.playing");
            player.m_20137_("in_lobby");
         }

         PlayerProgressManager.unloadPlayer(player);
         player.f_8924_.execute(() -> GameStateManager.checkForMatchEnd(player.f_8924_));
      }
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer victim) {
         SpectatorCameraManager.onPlayerDeath(victim);
         if (GameStateManager.isRunning(victim.f_8924_) && GameStateManager.getCurrentMode().isTeamMode()) {
            VoiceChatTeamManager.removePlayerFromVoiceGroup(victim);
         }

         DamageSource source = event.getSource();
         Set<String> victimTags = victim.m_19880_();
         boolean victimWasPlaying = victimTags.contains("war.playing");
         boolean victimWasInLobby = victimTags.contains("in_lobby") || GameStateManager.isInLobby(victim);
         if (!victimWasInLobby || victimWasPlaying) {
            if (victimWasPlaying) {
               DeathTransitionManager.recordDeath(victim, source);
               CorpseLootManager.createCorpse(victim);
               PlayerProgressManager.addDeath(victim);
               DiscordLeaderboardService.recordMatchDeath(victim);
               LivesManager.handleDeath(victim);
               ContractManager.onPlayerKilled(victim, source.m_7639_() instanceof ServerPlayer killer ? killer : null);
               ExtractionPointManager.onPlayerDeathOrLogout(victim);
               ClassXPManager.syncAll(victim.f_8924_);
               GameStateManager.checkForMatchEnd(victim.f_8924_);
            }

            if (source.m_7639_() instanceof ServerPlayer killer) {
               if (killer.m_19880_().contains("war.playing")) {
                  if (victimWasPlaying) {
                     if (!killer.m_20148_().equals(victim.m_20148_())) {
                        Entity direct = source.m_7640_();
                        if (direct instanceof Projectile projectile) {
                           Entity owner = projectile.m_19749_();
                           if (owner != null && owner.m_20148_().equals(victim.m_20148_())) {
                              return;
                           }
                        }

                        if (GameStateManager.getCurrentMode().isTeamMode() && TeamMatchManager.areTeammates(killer, victim)) {
                           int teamKills = DiscordLeaderboardService.recordTeamKill(killer.f_8924_, killer);
                           killer.m_213846_(Component.m_237113_("[WAR] Тимкилл не засчитан. Тимкиллы за сет: " + teamKills + "/3."));
                           if (teamKills >= 3) {
                              banTeamKiller(killer, teamKills);
                           }
                        } else {
                           PlayerProgressManager.addKill(killer);
                           DiscordLeaderboardService.recordMatchKill(killer);
                           PlayerProgressManager.addCoins(killer, 2);
                           ClassXPManager.sync(killer);
                           String clazz = PlayerTabletState.getSelectedClass(killer);
                           if (clazz != null && !clazz.isBlank()) {
                              ServerEvents.XPResult result = calculateXP(killer, victim, source, direct);
                              if (result.xp > 0) {
                                 int awardedXp = ClassXPManager.addXP(killer, clazz, result.xp);
                                 XpNotifier.send(killer, awardedXp, result.reason);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void banTeamKiller(ServerPlayer player, int teamKills) {
      Date createdAt = new Date();
      Date expiresAt = new Date(createdAt.getTime() + 900000L);
      String reason = "Тимкилл: " + teamKills + " убийства союзников за сет.";
      player.f_8924_.m_6846_().m_11295_().m_11381_(new UserBanListEntry(player.m_36316_(), createdAt, "TacticalTablet", expiresAt, reason));
      player.f_8906_.m_9942_(Component.m_237113_("[WAR] Бан на 15 минут за тимкиллы: " + teamKills + "/3."));
   }

   @SubscribeEvent
   public static void onServerStarted(ServerStartedEvent event) {
      ((BooleanValue)event.getServer().m_129900_().m_46170_(GameRules.f_46153_)).m_46246_(false, event.getServer());
      ((BooleanValue)event.getServer().m_129900_().m_46170_(GameRules.f_46156_)).m_46246_(true, event.getServer());
      DropControlManager.enforceGameRules(event.getServer());
      GameStateManager.resetRuntime(event.getServer());
      GameStateManager.validateRuntimeRequirements(event.getServer());
      MapRotationManager.onServerStarted(event.getServer());
      MapSetManager.onServerStarted(event.getServer());
      DiscordLeaderboardService.init(event.getServer());
      LeaderboardScheduler.onServerStarted(event.getServer());
      OnlineWebhookService.onServerStarted(event.getServer());
      ZoneManager.reset(event.getServer());
      ExtractionPointManager.reset(event.getServer());
      DeathTransitionManager.clearAll();
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      MapRotationManager.onServerStopped(event.getServer());
      MapSetManager.onServerStopped();
      MapRotationManager.resetRuntime();
      OnlineWebhookService.onServerStopped();
      LeaderboardScheduler.reset();
      DiscordLeaderboardService.resetMatch();
      DiscordWebhookClient.shutdown();
      GameStateManager.resetRuntime(event.getServer());
      VoiceChatTeamManager.shutdown(event.getServer());
      TestModeManager.reset();
      AirdropManager.resetRuntime(GameStateManager.getOverworld(event.getServer()));
      ContractManager.reset(event.getServer());
      ExtractionPointManager.reset(event.getServer());
      DeathTransitionManager.clearAll();
      RtpTimerManager.clearAll();
      PassiveClassXPManager.clearAll();
      ClassCooldownManager.resetAll();
      SafeTeleport.clearPool();
      PlayerTabletState.resetAll();
      TabletPacket.resetAll();
      AntiCheatManager.resetAll();
      MovementAntiCheat.resetAll();
      InventoryLockEvents.resetTracking();
      PlayerProgressManager.resetStorage();
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      PlayerProgressManager.saveAll();
   }

   private static ServerEvents.XPResult calculateXP(ServerPlayer killer, ServerPlayer victim, DamageSource source, Entity direct) {
      String msgId = safeLower(source.m_19385_());
      String directId = entityId(direct);
      String sourceEntityId = entityId(source.m_7639_());
      String sourceText = msgId + " " + directId + " " + sourceEntityId;
      if (isMineDamage(sourceText)) {
         return new ServerEvents.XPResult(16, "мина");
      } else if (isPhosphorusMortarDamage(sourceText)) {
         return new ServerEvents.XPResult(25, "phosphorus mortar");
      } else if (isMortarDamage(sourceText)) {
         return new ServerEvents.XPResult(22, "миномёт");
      } else if (isGrenadeDamage(sourceText)) {
         return new ServerEvents.XPResult(16, "граната");
      } else if (isExplosionDamage(sourceText)) {
         return new ServerEvents.XPResult(14, "взрыв");
      } else if (isLongRangeKill(killer, victim) && isRangedDamage(sourceText, direct)) {
         return new ServerEvents.XPResult(15, "дальнее убийство");
      } else if (isFirearmDamage(sourceText, direct)) {
         return new ServerEvents.XPResult(12, "огнестрел");
      } else {
         return isMeleeDamage(killer, msgId, sourceText, direct) ? new ServerEvents.XPResult(25, "ближний бой") : new ServerEvents.XPResult(10, "убийство");
      }
   }

   private static void checkCombatReach(ServerPlayer victim, DamageSource source) {
      if (victim != null && source != null) {
         if (source.m_7639_() instanceof ServerPlayer attacker) {
            if (!attacker.m_20148_().equals(victim.m_20148_())) {
               if (attacker.m_19880_().contains("war.playing")) {
                  if (victim.m_19880_().contains("war.playing")) {
                     Entity direct = source.m_7640_();
                     if (!(direct instanceof Projectile)) {
                        String msgId = safeLower(source.m_19385_());
                        String sourceText = msgId + " " + entityId(direct) + " " + entityId(source.m_7639_());
                        if (isMeleeDamage(attacker, msgId, sourceText, direct)) {
                           double distance = attacker.m_20270_(victim);
                           if (!(distance <= 5.5)) {
                              AntiCheatManager.record(
                                 attacker,
                                 ViolationType.COMBAT_REACH,
                                 Severity.HIGH,
                                 "target="
                                    + victim.m_36316_().getName()
                                    + " distance="
                                    + String.format(Locale.ROOT, "%.2f", distance)
                                    + " max="
                                    + String.format(Locale.ROOT, "%.2f", 5.5)
                              );
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isLongRangeKill(ServerPlayer killer, ServerPlayer victim) {
      if (killer != null && victim != null) {
         return !killer.m_9236_().m_46472_().equals(victim.m_9236_().m_46472_()) ? false : killer.m_20280_(victim) >= 6400.0;
      } else {
         return false;
      }
   }

   private static boolean isMineDamage(String sourceText) {
      return containsAny(
         sourceText,
         "claymore",
         "m18a1",
         "m18_a1",
         "blu43",
         "blu_43",
         "blu-43",
         "dragontooth",
         "dragon_tooth",
         "landmine",
         "land_mine",
         "anti_personnel_mine",
         "anti_tank_mine",
         ":mine",
         "_mine",
         " mine"
      );
   }

   private static boolean isTeamTrapDamage(String sourceText) {
      return isMineDamage(sourceText) || containsAny(sourceText, "trap", "claymore", "barbed", "wire", "spike");
   }

   private static boolean isMortarDamage(String sourceText) {
      return containsAny(sourceText, "mortar", "mortar_shell");
   }

   private static boolean isPhosphorusMortarDamage(String sourceText) {
      return isMortarDamage(sourceText)
         && containsAny(sourceText, "phosphor", "phosphorus", "white_phosphorus", "whitephosphorus", "white-phosphorus", "wp_shell", "wp_mortar", "wpmortar");
   }

   private static boolean isGrenadeDamage(String sourceText) {
      return containsAny(sourceText, "grenade", "m67", "frag");
   }

   private static boolean isExplosionDamage(String sourceText) {
      return containsAny(sourceText, "explosion", "explode", "superbwarfare");
   }

   private static boolean isRangedDamage(String sourceText, Entity direct) {
      return direct instanceof Projectile || isFirearmDamage(sourceText, direct);
   }

   private static boolean isFirearmDamage(String sourceText, Entity direct) {
      return direct instanceof Projectile
         || containsAny(sourceText, "tacz", "bullet", "projectile", "firearm", "gun", "rifle", "sniper", "m700", "arrow", "bolt");
   }

   private static boolean isMeleeDamage(ServerPlayer killer, String msgId, String sourceText, Entity direct) {
      if (killer == null) {
         return false;
      } else if (direct != null && direct.m_20148_().equals(killer.m_20148_())) {
         return true;
      } else {
         return direct == null && containsAny(msgId, "player", "mob", "melee", "crowbar", "buttstock", "butt_stock", "punch", "sword", "knife", "bayonet")
            ? true
            : containsAny(sourceText, "melee", "crowbar", "buttstock", "butt_stock", "punch", "sword", "knife", "bayonet");
      }
   }

   private static String entityId(Entity entity) {
      return entity == null ? "" : safeLower(entity.m_6095_().toString());
   }

   private static String safeLower(String value) {
      return value == null ? "" : value.toLowerCase(Locale.ROOT);
   }

   private static boolean containsAny(String value, String... needles) {
      if (value != null && !value.isEmpty()) {
         for (String needle : needles) {
            if (value.contains(needle)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private record XPResult(int xp, String reason) {
   }
}
