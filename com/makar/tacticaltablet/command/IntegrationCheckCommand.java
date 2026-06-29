package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.diagnostics.IntegrationChecks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class IntegrationCheckCommand {
   private IntegrationCheckCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("ttcheck").requires(source -> source.m_6761_(2)))
            .executes(context -> run((CommandSourceStack)context.getSource()))
      );
   }

   private static int run(CommandSourceStack source) {
      int failed = 0;
      source.m_288197_(() -> Component.m_237113_("[TT] Проверки интеграций:"), false);

      for (IntegrationChecks.Result result : IntegrationChecks.run(source.m_81377_())) {
         if (!result.passed()) {
            failed++;
         }

         String status = result.passed() ? "ОК" : "ОШИБКА";
         source.m_288197_(() -> Component.m_237113_("[TT] [" + status + "] " + result.name() + " - " + result.details()), false);
      }

      int finalFailed = failed;
      source.m_288197_(() -> Component.m_237113_("[TT] Проверки завершены. Ошибок: " + finalFailed), false);
      return failed == 0 ? 1 : 0;
   }
}
