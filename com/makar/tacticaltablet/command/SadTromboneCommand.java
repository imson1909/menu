package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SadTromboneCommand {
   private SadTromboneCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("ttsadthrombone").requires(source -> source.m_6761_(2)))
               .executes(context -> setPrivilege((CommandSourceStack)context.getSource(), ((CommandSourceStack)context.getSource()).m_81375_(), true)))
            .then(
               ((RequiredArgumentBuilder)Commands.m_82129_("target", EntityArgument.m_91466_())
                     .executes(context -> setPrivilege((CommandSourceStack)context.getSource(), EntityArgument.m_91474_(context, "target"), true)))
                  .then(
                     Commands.m_82129_("enabled", BoolArgumentType.bool())
                        .executes(
                           context -> setPrivilege(
                              (CommandSourceStack)context.getSource(), EntityArgument.m_91474_(context, "target"), BoolArgumentType.getBool(context, "enabled")
                           )
                        )
                  )
            )
      );
   }

   private static int setPrivilege(CommandSourceStack source, ServerPlayer target, boolean enabled) {
      boolean changed = PlayerProgressManager.isSadTromboneKillsEnabled(target) != enabled;
      PlayerProgressManager.setSadTromboneKillsEnabled(target, enabled);
      String playerName = target.m_36316_().getName();
      String state = enabled ? "включена" : "выключена";
      source.m_288197_(() -> Component.m_237113_("[WAR] Привилегия sad_thrombone для убийств игрока " + playerName + " " + state + "."), true);
      if (changed && source.m_81373_() != target) {
         target.m_213846_(Component.m_237113_("[WAR] Администратор " + (enabled ? "включил" : "выключил") + " вам sad_thrombone для убитых вами игроков."));
      }

      return changed ? 1 : 0;
   }
}
