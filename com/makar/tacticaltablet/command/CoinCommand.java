package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CoinCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                           "ttcoins"
                        )
                        .requires(source -> source.m_6761_(2)))
                     .then(Commands.m_82127_("get").then(Commands.m_82129_("target", EntityArgument.m_91466_()).executes(ctx -> {
                        ServerPlayer target = EntityArgument.m_91474_(ctx, "target");
                        int coins = PlayerProgressManager.getCoins(target);
                        ((CommandSourceStack)ctx.getSource())
                           .m_288197_(() -> Component.m_237113_(target.m_36316_().getName() + ": " + coins + " монет."), false);
                        return coins;
                     }))))
                  .then(
                     Commands.m_82127_("give")
                        .then(
                           Commands.m_82129_("targets", EntityArgument.m_91470_())
                              .then(
                                 Commands.m_82129_("amount", IntegerArgumentType.integer(1))
                                    .executes(
                                       ctx -> {
                                          Collection<ServerPlayer> targets = EntityArgument.m_91477_(ctx, "targets");
                                          int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                          for (ServerPlayer target : targets) {
                                             PlayerProgressManager.addCoins(target, amount);
                                             PlayerProgressManager.savePlayer(target);
                                             ClassXPManager.sync(target);
                                          }

                                          ((CommandSourceStack)ctx.getSource())
                                             .m_288197_(() -> Component.m_237113_("Добавлено " + amount + " монет игрокам: " + targets.size() + "."), true);
                                          return targets.size();
                                       }
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.m_82127_("remove")
                     .then(
                        Commands.m_82129_("targets", EntityArgument.m_91470_())
                           .then(
                              Commands.m_82129_("amount", IntegerArgumentType.integer(1))
                                 .executes(
                                    ctx -> {
                                       Collection<ServerPlayer> targets = EntityArgument.m_91477_(ctx, "targets");
                                       int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                       for (ServerPlayer target : targets) {
                                          PlayerProgressManager.addCoins(target, -amount);
                                          PlayerProgressManager.savePlayer(target);
                                          ClassXPManager.sync(target);
                                       }

                                       ((CommandSourceStack)ctx.getSource())
                                          .m_288197_(() -> Component.m_237113_("Снято " + amount + " монет у игроков: " + targets.size() + "."), true);
                                       return targets.size();
                                    }
                                 )
                           )
                     )
               ))
            .then(
               Commands.m_82127_("set")
                  .then(
                     Commands.m_82129_("targets", EntityArgument.m_91470_())
                        .then(
                           Commands.m_82129_("amount", IntegerArgumentType.integer(0))
                              .executes(
                                 ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.m_91477_(ctx, "targets");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                    for (ServerPlayer target : targets) {
                                       PlayerProgressManager.setCoins(target, amount);
                                       PlayerProgressManager.savePlayer(target);
                                       ClassXPManager.sync(target);
                                    }

                                    ((CommandSourceStack)ctx.getSource())
                                       .m_288197_(() -> Component.m_237113_("Установлено " + amount + " монет для игроков: " + targets.size() + "."), true);
                                    return targets.size();
                                 }
                              )
                        )
                  )
            )
      );
   }
}
