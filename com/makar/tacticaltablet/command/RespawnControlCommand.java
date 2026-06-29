package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.game.respawn.RespawnControlManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class RespawnControlCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("ttrespawns")
                     .requires(source -> source.m_6761_(2)))
                  .then(Commands.m_82127_("disable").executes(context -> disable((CommandSourceStack)context.getSource()))))
               .then(Commands.m_82127_("enable").executes(context -> enable((CommandSourceStack)context.getSource()))))
            .then(Commands.m_82127_("status").executes(context -> status((CommandSourceStack)context.getSource())))
      );
   }

   private static int disable(CommandSourceStack source) {
      RespawnControlManager.disableRespawns(source.m_81377_());
      source.m_288197_(() -> Component.m_237113_("[WAR] Возрождения отключены."), true);
      return 1;
   }

   private static int enable(CommandSourceStack source) {
      RespawnControlManager.enableRespawns(source.m_81377_());
      source.m_288197_(() -> Component.m_237113_("[WAR] Возрождения включены."), true);
      return 1;
   }

   private static int status(CommandSourceStack source) {
      source.m_288197_(() -> Component.m_237113_("[WAR] Возрождения отключены: " + RespawnControlManager.areRespawnsDisabled()), false);
      return 1;
   }
}
