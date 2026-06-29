package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.integration.online.OnlineWebhookConfig;
import com.makar.tacticaltablet.integration.online.OnlineWebhookService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class OnlineWebhookCommand {
   private OnlineWebhookCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                              "onlinewebhook"
                           )
                           .requires(source -> source.m_6761_(2)))
                        .executes(ctx -> status((CommandSourceStack)ctx.getSource())))
                     .then(Commands.m_82127_("status").executes(ctx -> status((CommandSourceStack)ctx.getSource()))))
                  .then(Commands.m_82127_("reload").executes(ctx -> reload((CommandSourceStack)ctx.getSource()))))
               .then(Commands.m_82127_("send-now").executes(ctx -> sendNow((CommandSourceStack)ctx.getSource()))))
            .then(Commands.m_82127_("clear-message").executes(ctx -> clearMessage((CommandSourceStack)ctx.getSource())))
      );
   }

   private static int status(CommandSourceStack source) {
      OnlineWebhookConfig config = OnlineWebhookConfig.get(source.m_81377_());
      source.m_288197_(() -> Component.m_237113_("Онлайн-вебхук тактического планшета:"), false);
      source.m_288197_(() -> Component.m_237113_("- включён: " + config.isEnabled()), false);
      source.m_288197_(() -> Component.m_237113_("- настроен: " + config.hasWebhook()), false);
      source.m_288197_(() -> Component.m_237113_("- интервал обновления, сек.: " + config.getUpdateIntervalSeconds()), false);
      source.m_288197_(() -> Component.m_237113_("- интервал рестарта, мин.: " + config.getRestartIntervalMinutes()), false);
      source.m_288197_(() -> Component.m_237113_("- id сообщения: " + (config.hasMessageId() ? "сохранён" : "-")), false);
      return config.hasWebhook() ? 1 : 0;
   }

   private static int reload(CommandSourceStack source) {
      OnlineWebhookConfig.reload(source.m_81377_());
      source.m_288197_(() -> Component.m_237113_("Конфиг онлайн-вебхука перезагружен."), true);
      return 1;
   }

   private static int sendNow(CommandSourceStack source) {
      boolean queued = OnlineWebhookService.sendUpdateNow(source.m_81377_());
      if (!queued) {
         source.m_81352_(Component.m_237113_("Онлайн-вебхук выключен, не настроен или уже занят."));
         return 0;
      } else {
         source.m_288197_(() -> Component.m_237113_("Обновление онлайн-вебхука поставлено в очередь."), true);
         return 1;
      }
   }

   private static int clearMessage(CommandSourceStack source) {
      OnlineWebhookConfig.clearMessageId();
      source.m_288197_(() -> Component.m_237113_("ID сообщения онлайн-вебхука очищен. Следующее обновление создаст новое сообщение Discord."), true);
      return 1;
   }
}
