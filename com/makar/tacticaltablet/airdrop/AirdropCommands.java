package com.makar.tacticaltablet.airdrop;

import com.makar.tacticaltablet.airdrop.loot.AirdropLootGenerator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class AirdropCommands {
   private AirdropCommands() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                                 "ttairdrop"
                              )
                              .requires(source -> source.m_6761_(2)))
                           .then(Commands.m_82127_("start").executes(context -> start((CommandSourceStack)context.getSource(), false))))
                        .then(Commands.m_82127_("start_now").executes(context -> start((CommandSourceStack)context.getSource(), true))))
                     .then(Commands.m_82127_("cancel").executes(context -> cancel((CommandSourceStack)context.getSource()))))
                  .then(Commands.m_82127_("status").executes(context -> status((CommandSourceStack)context.getSource(), false))))
               .then(Commands.m_82127_("debug").executes(context -> status((CommandSourceStack)context.getSource(), true))))
            .then(Commands.m_82127_("reload_loot").executes(context -> reloadLoot((CommandSourceStack)context.getSource())))
      );
   }

   private static int start(CommandSourceStack source, boolean instant) {
      ServerLevel level = source.m_81372_();
      if (AirdropManager.hasActiveAirdrop()) {
         source.m_81352_(Component.m_237113_("[СБРОС] Сброс уже активен."));
         return 0;
      } else {
         AirdropManager.start(level, instant);
         source.m_288197_(() -> Component.m_237113_("[СБРОС] Запуск запрошен."), true);
         return 1;
      }
   }

   private static int cancel(CommandSourceStack source) {
      if (!AirdropManager.hasActiveAirdrop()) {
         source.m_81352_(Component.m_237113_("[СБРОС] Активного сброса нет."));
         return 0;
      } else {
         AirdropManager.cancel(source.m_81372_());
         source.m_288197_(() -> Component.m_237113_("[СБРОС] Сброс отменён."), true);
         return 1;
      }
   }

   private static int status(CommandSourceStack source, boolean debug) {
      AirdropData data = AirdropManager.getActiveAirdrop();
      if (data == null) {
         source.m_288197_(() -> Component.m_237113_("[СБРОС] состояние=нет"), false);
         return 1;
      }

      source.m_288197_(() -> Component.m_237113_("[СБРОС] состояние=" + stateName(data.state)), false);
      source.m_288197_(() -> Component.m_237113_("- реальная точка: " + format(data.realDropPos)), false);
      source.m_288197_(() -> Component.m_237113_("- точка компаса: " + format(data.compassTargetPos)), false);
      source.m_288197_(() -> Component.m_237113_("- тиков до сброса: " + data.ticksUntilDrop), false);
      source.m_288197_(() -> Component.m_237113_("- тиков после приземления: " + data.ticksSinceLanded), false);
      if (debug) {
         source.m_288197_(() -> Component.m_237113_("- ID: " + data.id), false);
         source.m_288197_(() -> Component.m_237113_("- измерение: " + data.dimension.m_135782_()), false);
         source.m_288197_(() -> Component.m_237113_("- открыл: " + (data.openedBy == null ? "-" : data.openedBy)), false);
         source.m_288197_(() -> Component.m_237113_("- позиция сундука: " + format(data.chestPos)), false);
         source.m_288197_(() -> Component.m_237113_("- тиков после открытия: " + data.ticksSinceOpened), false);
         source.m_288197_(() -> Component.m_237113_("- зелёный дым: " + data.greenSmoke), false);
         source.m_288197_(() -> Component.m_237113_("- текущая высота ящика: " + String.format(Locale.ROOT, "%.2f", data.currentCrateY)), false);
      }

      return 1;
   }

   private static int reloadLoot(CommandSourceStack source) {
      int count = AirdropLootGenerator.reloadLoot();
      source.m_288197_(() -> Component.m_237113_("[СБРОС] Конфиг лута перезагружен. Валидных записей: " + count), true);
      return count;
   }

   private static String stateName(AirdropState state) {
      return switch (state) {
         case NONE -> "нет";
         case ANNOUNCED -> "объявлен";
         case FALLING -> "падает";
         case LANDED -> "приземлился";
         case OPENED -> "открыт";
         case EXPIRED -> "завершён";
      };
   }

   private static String format(BlockPos pos) {
      return pos == null ? "-" : pos.m_123341_() + " " + pos.m_123342_() + " " + pos.m_123343_();
   }
}
