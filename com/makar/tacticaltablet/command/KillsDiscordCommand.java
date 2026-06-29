package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.integration.discord.DiscordConfig;
import com.makar.tacticaltablet.integration.discord.DiscordLeaderboardService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class KillsDiscordCommand {
   private KillsDiscordCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("killsdiscord").requires(source -> source.m_6761_(2)))
               .executes(ctx -> send((CommandSourceStack)ctx.getSource())))
            .then(Commands.m_82127_("reload").executes(ctx -> {
               DiscordConfig.reload(((CommandSourceStack)ctx.getSource()).m_81377_());
               ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_("Конфиг рейтинга Discord перезагружен."), false);
               return 1;
            }))
      );
   }

   private static int send(CommandSourceStack source) {
      DiscordConfig config = DiscordConfig.get(source.m_81377_());
      if (!config.hasWebhook()) {
         source.m_81352_(Component.m_237113_("Discord-вебхук не настроен. Укажи webhookUrl в config/tacticaltablet_discord.json."));
         return 0;
      } else {
         DiscordLeaderboardService.sendOverallLeaderboard(source.m_81377_());
         source.m_288197_(() -> Component.m_237113_("Отправка рейтинга убийств в Discord поставлена в очередь."), true);
         return 1;
      }
   }
}
