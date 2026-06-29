package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.game.lobby.LobbyManager;
import com.makar.tacticaltablet.progression.ClassCooldownManager;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ResetCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("ttreset")
                     .requires(source -> source.m_6761_(2)))
                  .executes(ctx -> {
                     ServerPlayer player = ((CommandSourceStack)ctx.getSource()).m_81375_();
                     resetPlayer(player);
                     ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_("Планшет сброшен для себя"), false);
                     return 1;
                  }))
               .then(Commands.m_82129_("target", EntityArgument.m_91466_()).executes(ctx -> {
                  ServerPlayer target = EntityArgument.m_91474_(ctx, "target");
                  resetPlayer(target);
                  ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_("Планшет сброшен: " + target.m_7755_().getString()), false);
                  return 1;
               })))
            .then(Commands.m_82127_("all").executes(ctx -> {
               Collection<ServerPlayer> players = ((CommandSourceStack)ctx.getSource()).m_81377_().m_6846_().m_11314_();

               for (ServerPlayer player : players) {
                  resetPlayer(player);
               }

               ClassCooldownManager.resetAll();
               ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_("Планшеты сброшены для всех игроков"), true);
               return players.size();
            }))
      );
   }

   private static void resetPlayer(ServerPlayer player) {
      ClassCooldownManager.reset(player);
      PlayerTabletState.reset(player);
      LobbyManager.sync(player);
   }
}
