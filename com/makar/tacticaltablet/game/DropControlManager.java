package com.makar.tacticaltablet.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;

public final class DropControlManager {
   private static final boolean BLOCK_DROPS_ENABLED = false;

   private DropControlManager() {
   }

   public static void enforceGameRules(MinecraftServer server) {
      if (server != null) {
         for (ServerLevel level : server.m_129785_()) {
            ((BooleanValue)level.m_46469_().m_46170_(GameRules.f_46136_)).m_46246_(false, server);
         }
      }
   }
}
