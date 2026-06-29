package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.corpse.CorpseTestManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class CorpseTestCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("ttcorpse").requires(source -> source.m_6761_(2)))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("selfloot")
                           .executes(ctx -> status((CommandSourceStack)ctx.getSource())))
                        .then(Commands.m_82127_("on").executes(ctx -> setSelfLoot((CommandSourceStack)ctx.getSource(), true))))
                     .then(Commands.m_82127_("off").executes(ctx -> setSelfLoot((CommandSourceStack)ctx.getSource(), false))))
                  .then(Commands.m_82127_("toggle").executes(ctx -> setSelfLoot((CommandSourceStack)ctx.getSource(), CorpseTestManager.toggleOwnCorpseLoot())))
            )
      );
   }

   private static int setSelfLoot(CommandSourceStack source, boolean enabled) {
      CorpseTestManager.setOwnCorpseLootEnabled(enabled);
      source.m_288197_(() -> Component.m_237113_("[WAR] Тест лута своего трупа: " + (enabled ? "включён" : "выключен")), true);
      return enabled ? 1 : 0;
   }

   private static int status(CommandSourceStack source) {
      boolean enabled = CorpseTestManager.canLootOwnCorpses();
      source.m_288197_(() -> Component.m_237113_("[WAR] Тест лута своего трупа: " + (enabled ? "включён" : "выключен")), false);
      return enabled ? 1 : 0;
   }
}
