package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.progression.ClassXPManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class XpBoostCommand {
   private XpBoostCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("ttxpboost").requires(source -> source.m_6761_(2)))
               .executes(context -> setBoost((CommandSourceStack)context.getSource(), ((CommandSourceStack)context.getSource()).m_81375_(), true)))
            .then(
               ((RequiredArgumentBuilder)Commands.m_82129_("target", EntityArgument.m_91466_())
                     .executes(context -> setBoost((CommandSourceStack)context.getSource(), EntityArgument.m_91474_(context, "target"), true)))
                  .then(
                     Commands.m_82129_("enabled", BoolArgumentType.bool())
                        .executes(
                           context -> setBoost(
                              (CommandSourceStack)context.getSource(), EntityArgument.m_91474_(context, "target"), BoolArgumentType.getBool(context, "enabled")
                           )
                        )
                  )
            )
      );
   }

   private static int setBoost(CommandSourceStack source, ServerPlayer target, boolean enabled) {
      boolean changed = ClassXPManager.isXpBoostEnabled(target) != enabled;
      ClassXPManager.setXpBoostEnabled(target, enabled);
      String playerName = target.m_36316_().getName();
      String state = enabled ? "включён" : "выключен";
      source.m_288197_(() -> Component.m_237113_("[WAR] Двойной опыт для " + playerName + " " + state + "."), true);
      if (changed && source.m_81373_() != target) {
         target.m_213846_(Component.m_237113_("[WAR] Администратор " + (enabled ? "включил" : "выключил") + " вам двойной опыт."));
      }

      return changed ? 1 : 0;
   }
}
