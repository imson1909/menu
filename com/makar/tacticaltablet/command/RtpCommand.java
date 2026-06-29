package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.game.teleport.SafeTeleport;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class RtpCommand {
   private static final int DEFAULT_TEST_POINTS = 50;

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("ttrtp").requires(source -> source.m_6761_(2)))
            .then(
               ((LiteralArgumentBuilder)Commands.m_82127_("testpoints").executes(context -> testPoints((CommandSourceStack)context.getSource(), 50)))
                  .then(
                     Commands.m_82129_("count", IntegerArgumentType.integer(1, 500))
                        .executes(context -> testPoints((CommandSourceStack)context.getSource(), IntegerArgumentType.getInteger(context, "count")))
                  )
            )
      );
   }

   private static int testPoints(CommandSourceStack source, int count) {
      SafeTeleport.TestResult result = SafeTeleport.testPoints(source.m_81377_(), count);
      source.m_288197_(() -> Component.m_237113_("[WAR] Тест точек RTP:"), false);
      source.m_288197_(() -> Component.m_237113_("- запрошено: " + result.requested()), false);
      source.m_288197_(() -> Component.m_237113_("- найдено безопасных: " + result.valid()), false);
      source.m_288197_(() -> Component.m_237113_("- попыток: " + result.attempts()), false);
      source.m_288197_(() -> Component.m_237113_("- размер границы: " + format(result.borderSize())), false);
      source.m_288197_(() -> Component.m_237113_("- отступ от границы: " + format(result.margin())), false);
      source.m_288197_(() -> Component.m_237113_("- точек в пуле сейчас: " + SafeTeleport.getPreparedCount()), false);
      if (!result.samples().isEmpty()) {
         source.m_288197_(
            () -> Component.m_237113_("- примеры точек: " + result.samples().stream().map(RtpCommand::format).collect(Collectors.joining(", "))), false
         );
      }

      if (result.valid() < result.requested()) {
         source.m_81352_(Component.m_237113_("[WAR] RTP не нашёл достаточно безопасных точек. Проверь границу мира и поверхность карты."));
      }

      return result.valid();
   }

   private static String format(BlockPos pos) {
      return pos.m_123341_() + " " + pos.m_123342_() + " " + pos.m_123343_();
   }

   private static String format(double value) {
      return String.format(Locale.ROOT, "%.1f", value);
   }
}
