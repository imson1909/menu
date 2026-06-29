package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.admin.TestModeManager;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.MatchMode;
import com.makar.tacticaltablet.game.clanwar.ClanWarManager;
import com.makar.tacticaltablet.game.contract.ContractManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TestModeCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                                             "tttest"
                                          )
                                          .requires(source -> source.m_6761_(2)))
                                       .executes(ctx -> sendStatus((CommandSourceStack)ctx.getSource())))
                                    .then(
                                       ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("solo")
                                                   .executes(ctx -> sendStatus((CommandSourceStack)ctx.getSource())))
                                                .then(Commands.m_82127_("on").executes(ctx -> setSoloMode((CommandSourceStack)ctx.getSource(), true))))
                                             .then(Commands.m_82127_("off").executes(ctx -> setSoloMode((CommandSourceStack)ctx.getSource(), false))))
                                          .then(
                                             Commands.m_82127_("toggle")
                                                .executes(ctx -> setSoloMode((CommandSourceStack)ctx.getSource(), !TestModeManager.isSoloStartEnabled()))
                                          )
                                    ))
                                 .then(Commands.m_82127_("start").executes(ctx -> {
                                    TestModeManager.setSoloStartEnabled(true);
                                    GameStateManager.startGame(((CommandSourceStack)ctx.getSource()).m_81377_());
                                    ((CommandSourceStack)ctx.getSource())
                                       .m_288197_(() -> Component.m_237113_("[WAR] Соло-тест включён, матч принудительно запущен."), true);
                                    return 1;
                                 })))
                              .then(Commands.m_82127_("stop").executes(ctx -> stopMatch((CommandSourceStack)ctx.getSource()))))
                           .then(
                              ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("clanwar")
                                             .executes(ctx -> sendClanWarStatus((CommandSourceStack)ctx.getSource())))
                                          .then(Commands.m_82127_("start").executes(ctx -> startClanWarDebug((CommandSourceStack)ctx.getSource(), true))))
                                       .then(Commands.m_82127_("wait").executes(ctx -> startClanWarDebug((CommandSourceStack)ctx.getSource(), false))))
                                    .then(
                                       ((LiteralArgumentBuilder)Commands.m_82127_("solo")
                                             .then(Commands.m_82127_("on").executes(ctx -> setClanWarSoloDebug((CommandSourceStack)ctx.getSource(), true))))
                                          .then(Commands.m_82127_("off").executes(ctx -> setClanWarSoloDebug((CommandSourceStack)ctx.getSource(), false)))
                                    ))
                                 .then(Commands.m_82127_("status").executes(ctx -> sendClanWarStatus((CommandSourceStack)ctx.getSource())))
                           ))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("lowplayers")
                                    .executes(ctx -> sendStatus((CommandSourceStack)ctx.getSource())))
                                 .then(Commands.m_82127_("on").executes(ctx -> setLowPlayerTeamTests((CommandSourceStack)ctx.getSource(), true))))
                              .then(Commands.m_82127_("off").executes(ctx -> setLowPlayerTeamTests((CommandSourceStack)ctx.getSource(), false)))
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.m_82127_("vote")
                              .then(Commands.m_82127_("start").executes(ctx -> startDebugVote((CommandSourceStack)ctx.getSource()))))
                           .then(Commands.m_82127_("stop").executes(ctx -> stopDebugVote((CommandSourceStack)ctx.getSource())))
                     ))
                  .then(Commands.m_82127_("mapvote").executes(ctx -> startDebugMapVote((CommandSourceStack)ctx.getSource()))))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("teamselect")
                           .then(Commands.m_82127_("duo").executes(ctx -> startDebugTeamSelect((CommandSourceStack)ctx.getSource(), MatchMode.DUO))))
                        .then(Commands.m_82127_("trio").executes(ctx -> startDebugTeamSelect((CommandSourceStack)ctx.getSource(), MatchMode.TRIO))))
                     .then(Commands.m_82127_("squads").executes(ctx -> startDebugTeamSelect((CommandSourceStack)ctx.getSource(), MatchMode.SQUADS)))
               ))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                                       "contract"
                                    )
                                    .executes(ctx -> sendContractStatus((CommandSourceStack)ctx.getSource())))
                                 .then(
                                    ((LiteralArgumentBuilder)Commands.m_82127_("solo")
                                          .then(Commands.m_82127_("on").executes(ctx -> setContractSoloDebug((CommandSourceStack)ctx.getSource(), true))))
                                       .then(Commands.m_82127_("off").executes(ctx -> setContractSoloDebug((CommandSourceStack)ctx.getSource(), false)))
                                 ))
                              .then(Commands.m_82127_("start").executes(ctx -> startContractSelection((CommandSourceStack)ctx.getSource()))))
                           .then(Commands.m_82127_("tracker").executes(ctx -> openContractTracker((CommandSourceStack)ctx.getSource()))))
                        .then(Commands.m_82127_("self").executes(ctx -> createSelfContract((CommandSourceStack)ctx.getSource()))))
                     .then(Commands.m_82127_("stand").executes(ctx -> createArmorStandContract((CommandSourceStack)ctx.getSource()))))
                  .then(
                     Commands.m_82127_("cooldown")
                        .then(
                           ((LiteralArgumentBuilder)Commands.m_82127_("reset")
                                 .then(Commands.m_82127_("all").executes(ctx -> resetContractCooldowns((CommandSourceStack)ctx.getSource()))))
                              .then(
                                 Commands.m_82129_("player", EntityArgument.m_91470_())
                                    .executes(ctx -> resetContractCooldown((CommandSourceStack)ctx.getSource(), EntityArgument.m_91477_(ctx, "player")))
                              )
                        )
                  )
            )
      );
   }

   private static int setSoloMode(CommandSourceStack source, boolean enabled) {
      TestModeManager.setSoloStartEnabled(enabled);
      source.m_288197_(() -> Component.m_237113_("[WAR] " + TestModeManager.getStatusText()), true);
      return enabled ? 1 : 0;
   }

   private static int setLowPlayerTeamTests(CommandSourceStack source, boolean enabled) {
      TestModeManager.setLowPlayerTeamTestsEnabled(enabled);
      source.m_288197_(() -> Component.m_237113_("[WAR] " + TestModeManager.getStatusText()), true);
      return enabled ? 1 : 0;
   }

   private static int startDebugVote(CommandSourceStack source) {
      TestModeManager.setLowPlayerTeamTestsEnabled(true);
      boolean started = GameStateManager.forceStartVoting(source.m_81377_());
      source.m_288197_(
         () -> Component.m_237113_(
            started
               ? "[WAR] Отладочное голосование началось. Командные тесты с малым числом игроков включены."
               : "[WAR] Нельзя запустить отладочное голосование во время матча."
         ),
         true
      );
      return started ? 1 : 0;
   }

   private static int startDebugMapVote(CommandSourceStack source) {
      TestModeManager.setSoloStartEnabled(true);
      boolean started = GameStateManager.forceStartMapVoting(source.m_81377_());
      if (started) {
         source.m_288197_(
            () -> Component.m_237113_(
               "[WAR] Запущено полное отладочное голосование за карту. После 60 секунд сервер выберет карту, подготовит ротацию и остановится."
            ),
            true
         );
         return 1;
      } else {
         source.m_81352_(Component.m_237113_("[WAR] Нельзя начать голосование за карту во время активного матча."));
         return 0;
      }
   }

   private static int stopDebugVote(CommandSourceStack source) {
      GameStateManager.forceStopMatch(source.m_81377_());
      TestModeManager.setLowPlayerTeamTestsEnabled(false);
      source.m_288197_(() -> Component.m_237113_("[WAR] Отладочное голосование остановлено. Командные тесты с малым числом игроков выключены."), true);
      return 1;
   }

   private static int startDebugTeamSelect(CommandSourceStack source, MatchMode mode) {
      TestModeManager.setLowPlayerTeamTestsEnabled(true);
      boolean started = GameStateManager.forceStartTeamSelect(source.m_81377_(), mode);
      source.m_288197_(
         () -> Component.m_237113_(
            started
               ? "[WAR] Отладочный выбор команды начался: " + mode.displayName() + ". Командные тесты с малым числом игроков включены."
               : "[WAR] Нельзя запустить отладочный выбор команды во время матча."
         ),
         true
      );
      return started ? 1 : 0;
   }

   private static int stopMatch(CommandSourceStack source) {
      boolean stopped = GameStateManager.forceStopMatch(source.m_81377_());
      TestModeManager.setLowPlayerTeamTestsEnabled(false);
      source.m_288197_(
         () -> Component.m_237113_(
            stopped
               ? "[WAR] Матч принудительно остановлен. Тестовый режим малого числа игроков выключен."
               : "[WAR] Состояние матча сброшено. Тестовый режим малого числа игроков выключен."
         ),
         true
      );
      return stopped ? 1 : 0;
   }

   private static int startClanWarDebug(CommandSourceStack source, boolean skipWait) {
      TestModeManager.setSoloStartEnabled(true);
      TestModeManager.setLowPlayerTeamTestsEnabled(true);
      ClanWarManager.setSoloDebugEnabled(true);
      MapSetManager.setDebugClanWarSet(source.m_81377_(), true);
      boolean started = GameStateManager.forceStartClanWar(source.m_81377_(), skipWait);
      source.m_288197_(
         () -> Component.m_237113_(
            started
               ? "[WAR] Clan-war debug started. solo=true, lowplayers=true, skipWait=" + skipWait + "."
               : "[WAR] Clan-war debug cannot start during an active match."
         ),
         true
      );
      return started ? 1 : 0;
   }

   private static int setClanWarSoloDebug(CommandSourceStack source, boolean enabled) {
      ClanWarManager.setSoloDebugEnabled(enabled);
      source.m_288197_(() -> Component.m_237113_("[WAR] Clan-war solo debug: " + (enabled ? "on" : "off") + "."), true);
      return enabled ? 1 : 0;
   }

   private static int sendClanWarStatus(CommandSourceStack source) {
      source.m_288197_(
         () -> Component.m_237113_(
            "[WAR] Clan-war currentSet="
               + MapSetManager.isClanWarSet()
               + ", soloDebug="
               + ClanWarManager.isSoloDebugEnabled()
               + ", soloStart="
               + TestModeManager.isSoloStartEnabled()
               + ", lowplayers="
               + TestModeManager.isLowPlayerTeamTestsEnabled()
               + "."
         ),
         false
      );
      return 1;
   }

   private static int setContractSoloDebug(CommandSourceStack source, boolean enabled) {
      ContractManager.setSoloDebugEnabled(enabled);
      source.m_288197_(() -> Component.m_237113_("[WAR] Тест контрактов в соло: " + (enabled ? "включён" : "выключен") + "."), true);
      return enabled ? 1 : 0;
   }

   private static int startContractSelection(CommandSourceStack source) {
      ContractManager.setSoloDebugEnabled(true);
      ContractManager.forceStartSelection(source.m_81377_());
      source.m_288197_(() -> Component.m_237113_("[WAR] Выбор контрактов принудительно открыт. Соло-тест контрактов включён."), true);
      return 1;
   }

   private static int openContractTracker(CommandSourceStack source) {
      if (source.m_81373_() instanceof ServerPlayer player) {
         ContractManager.ensureTracker(player);
         ContractManager.onTrackerUsed(player);
         return 1;
      } else {
         source.m_81352_(Component.m_237113_("[WAR] Команду tracker нужно выполнять игроком."));
         return 0;
      }
   }

   private static int createSelfContract(CommandSourceStack source) {
      if (source.m_81373_() instanceof ServerPlayer player) {
         ContractManager.setSoloDebugEnabled(true);
         boolean created = ContractManager.createDebugSelfContract(player);
         source.m_288197_(
            () -> Component.m_237113_(
               created ? "[WAR] Тестовый контракт на себя создан. Это только для проверки GUI." : "[WAR] Не удалось создать тестовый контракт."
            ),
            true
         );
         return created ? 1 : 0;
      } else {
         source.m_81352_(Component.m_237113_("[WAR] Команду self нужно выполнять игроком."));
         return 0;
      }
   }

   private static int createArmorStandContract(CommandSourceStack source) {
      if (source.m_81373_() instanceof ServerPlayer player) {
         ContractManager.setSoloDebugEnabled(true);
         boolean created = ContractManager.createDebugArmorStandContract(player);
         source.m_288197_(
            () -> Component.m_237113_(created ? "[WAR] Тестовый стенд контракта создан в случайной точке зоны." : "[WAR] Не удалось создать тестовый стенд."),
            true
         );
         return created ? 1 : 0;
      } else {
         source.m_81352_(Component.m_237113_("[WAR] Команду stand нужно выполнять игроком."));
         return 0;
      }
   }

   private static int resetContractCooldowns(CommandSourceStack source) {
      ContractManager.resetPickCooldowns(source.m_81377_());
      source.m_288197_(
         () -> Component.m_237113_("[WAR] Перезарядка выбора контракта сброшена для всех. Сейчас кд контрактов отключён: 1 матч = 1 контракт."), true
      );
      return 1;
   }

   private static int resetContractCooldown(CommandSourceStack source, Collection<ServerPlayer> players) {
      int count = 0;

      for (ServerPlayer player : players) {
         ContractManager.resetPickCooldown(player);
         count++;
      }

      int result = count;
      source.m_288197_(
         () -> Component.m_237113_("[WAR] Перезарядка выбора контракта сброшена для игроков: " + result + ". Сейчас кд контрактов отключён."), true
      );
      return result;
   }

   private static int sendContractStatus(CommandSourceStack source) {
      source.m_288197_(
         () -> Component.m_237113_("[WAR] Тест контрактов в соло: " + (ContractManager.isSoloDebugEnabled() ? "включён" : "выключен") + "."), false
      );
      return ContractManager.isSoloDebugEnabled() ? 1 : 0;
   }

   private static int sendStatus(CommandSourceStack source) {
      source.m_288197_(() -> Component.m_237113_("[WAR] " + TestModeManager.getStatusText()), false);
      return TestModeManager.isSoloStartEnabled() ? 1 : 0;
   }
}
