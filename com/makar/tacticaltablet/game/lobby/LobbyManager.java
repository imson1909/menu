package com.makar.tacticaltablet.game.lobby;

import com.makar.tacticaltablet.airdrop.AirdropManager;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.contract.ContractManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.respawn.RtpTimerManager;
import com.makar.tacticaltablet.game.team.TeamMatchManager;
import com.makar.tacticaltablet.inventory.InventoryManager;
import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class LobbyManager {
   private static final ResourceLocation LOBBY_SPAWN_TEMPLATE = new ResourceLocation("lobby", "spawn");
   private static final BlockPos LOBBY_SPAWN_ORIGIN = new BlockPos(-10, 64, -10);
   private static final double LOBBY_PLAYER_Y = 69.0;
   private static boolean platformReady = false;

   public static void moveToLobby(ServerPlayer player) {
      if (player != null) {
         ServerLevel lobby = GameStateManager.getLobbyLevel(player.f_8924_);
         if (lobby == null) {
            player.m_213846_(Component.m_237113_("[WAR] Измерение лобби lobby:lobby не найдено. Проверь датапак."));
         } else {
            if (!platformReady) {
               ensureLobbyPlatform(lobby);
               placeLobbySpawn(lobby);
               platformReady = true;
            }

            relaxLobbyBorder(lobby);
            RtpTimerManager.cancel(player);
            boolean preserveTeamMatchState = GameStateManager.isRunning(player.f_8924_)
               && GameStateManager.getCurrentMode().isTeamMode()
               && TeamMatchManager.getTeam(player) != null
               && LivesManager.canContinueMatch(player);
            if (!preserveTeamMatchState) {
               PlayerTabletState.reset(player);
            }

            player.m_143403_(GameType.SURVIVAL);
            player.m_20137_("war.playing");
            InventoryManager.clearInventory(player);
            boolean matchRunning = GameStateManager.isRunning(player.f_8924_);
            boolean canUseTabletNow = GameStateManager.isTabletAvailableInLobby(player.f_8924_) && LivesManager.canContinueMatch(player);
            if (matchRunning && canUseTabletNow) {
               player.m_20049_("in_lobby");
            } else {
               player.m_20137_("in_lobby");
            }

            player.m_5489_(lobby);
            player.m_8999_(lobby, 0.5, 69.0, 0.5, player.m_146908_(), player.m_146909_());
            player.m_7292_(new MobEffectInstance(MobEffects.f_19606_, 100, 255, false, false, false));
            if (canUseTabletNow) {
               InventoryManager.giveFreshTablet(player);
               AirdropManager.giveCompassToJoiningPlayer(player);
               if (matchRunning) {
                  RtpTimerManager.start(player);
               } else {
                  sync(player);
               }
            } else {
               sync(player);
            }
         }
      }
   }

   private static void ensureLobbyPlatform(ServerLevel lobby) {
      BlockPos centerFloor = new BlockPos(0, 65, 0);
      if (lobby.m_8055_(centerFloor).m_60795_()) {
         for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
               BlockPos pos = new BlockPos(x, 65, z);
               if (lobby.m_8055_(pos).m_60795_()) {
                  lobby.m_7731_(pos, Blocks.f_50470_.m_49966_(), 3);
               }
            }
         }
      }

      BlockPos lightPos = new BlockPos(0, 66, 0);
      if (lobby.m_8055_(lightPos).m_60795_()) {
         lobby.m_7731_(lightPos, (BlockState)Blocks.f_152480_.m_49966_().m_61124_(LightBlock.f_153657_, 15), 3);
      }
   }

   private static void placeLobbySpawn(ServerLevel lobby) {
      Optional<StructureTemplate> template = lobby.m_215082_().m_230407_(LOBBY_SPAWN_TEMPLATE);
      if (!template.isEmpty()) {
         template.get().m_230328_(lobby, LOBBY_SPAWN_ORIGIN, LOBBY_SPAWN_ORIGIN, new StructurePlaceSettings(), RandomSource.m_216327_(), 3);
      }
   }

   public static void keepLobbyWeatherClear(MinecraftServer server) {
      if (server != null) {
         ServerLevel lobby = GameStateManager.getLobbyLevel(server);
         if (lobby != null) {
            clearWeather(lobby);
            relaxLobbyBorder(lobby);
         }

         ServerLevel overworld = server.m_129880_(Level.f_46428_);
         if (overworld != null) {
            clearWeather(overworld);
         }
      }
   }

   private static void clearWeather(ServerLevel level) {
      level.m_8606_(12000, 0, false, false);
      ((BooleanValue)level.m_46469_().m_46170_(GameRules.f_46150_)).m_46246_(false, level.m_7654_());
   }

   private static void relaxLobbyBorder(ServerLevel lobby) {
      WorldBorder border = lobby.m_6857_();
      border.m_61949_(0.0, 0.0);
      border.m_61917_(5.999997E7F);
      border.m_61939_(5.999997E7F);
      border.m_61947_(0.0);
      border.m_61952_(0);
      border.m_61944_(0);
   }

   public static void giveTabletIfMissing(ServerPlayer player) {
      InventoryManager.giveTabletIfMissing(player);
   }

   public static void sync(ServerPlayer player) {
      if (player != null) {
         PacketHandler.sendToPlayer(player, ClassXPManager.createStatePacket(player));
         ContractManager.syncSelection(player);
      }
   }
}
