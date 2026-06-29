package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.progression.ClassXPManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DebugXPCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("ttxp").requires(source -> source.m_6761_(2)))
            .then(Commands.m_82129_("class", StringArgumentType.word()).then(Commands.m_82129_("amount", IntegerArgumentType.integer(1)).executes(ctx -> {
               ServerPlayer player = ((CommandSourceStack)ctx.getSource()).m_81375_();
               String clazz = StringArgumentType.getString(ctx, "class");
               int amount = IntegerArgumentType.getInteger(ctx, "amount");
               int awarded = ClassXPManager.addXP(player, clazz, amount);
               ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_("Добавлено " + awarded + " опыта классу " + clazz), false);
               return 1;
            })))
      );
   }
}
