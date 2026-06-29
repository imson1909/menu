package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.game.extraction.ExtractionCompassHelper;
import com.makar.tacticaltablet.game.extraction.ExtractionPointData;
import com.makar.tacticaltablet.game.extraction.ExtractionPointManager;
import com.makar.tacticaltablet.game.extraction.ExtractionPointVisualHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.border.WorldBorder;

public final class ExtractionPointCommand {
   private ExtractionPointCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.m_82127_("tt")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                                                   "extraction"
                                                )
                                                .then(
                                                   ((LiteralArgumentBuilder)Commands.m_82127_("status").requires(source -> source.m_6761_(2)))
                                                      .executes(context -> status((CommandSourceStack)context.getSource()))
                                                ))
                                             .then(
                                                ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("tp").requires(source -> source.m_6761_(2)))
                                                      .executes(context -> teleport((CommandSourceStack)context.getSource(), false)))
                                                   .then(Commands.m_82127_("edge").executes(context -> teleport((CommandSourceStack)context.getSource(), true)))
                                             ))
                                          .then(
                                             ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("start")
                                                         .requires(source -> source.m_6761_(4)))
                                                      .executes(context -> start((CommandSourceStack)context.getSource())))
                                                   .then(Commands.m_82127_("here").executes(context -> startHere((CommandSourceStack)context.getSource()))))
                                                .then(
                                                   Commands.m_82127_("at")
                                                      .then(
                                                         Commands.m_82129_("x", DoubleArgumentType.doubleArg())
                                                            .then(
                                                               Commands.m_82129_("y", DoubleArgumentType.doubleArg())
                                                                  .then(
                                                                     Commands.m_82129_("z", DoubleArgumentType.doubleArg())
                                                                        .executes(
                                                                           context -> startAt(
                                                                              (CommandSourceStack)context.getSource(),
                                                                              DoubleArgumentType.getDouble(context, "x"),
                                                                              DoubleArgumentType.getDouble(context, "y"),
                                                                              DoubleArgumentType.getDouble(context, "z")
                                                                           )
                                                                        )
                                                                  )
                                                            )
                                                      )
                                                )
                                          ))
                                       .then(
                                          ((LiteralArgumentBuilder)Commands.m_82127_("stop").requires(source -> source.m_6761_(4) && debugEnabled(source)))
                                             .executes(context -> stop((CommandSourceStack)context.getSource()))
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)Commands.m_82127_("cleanup").requires(source -> source.m_6761_(4) && debugEnabled(source)))
                                          .executes(context -> cleanup((CommandSourceStack)context.getSource()))
                                    ))
                                 .then(
                                    ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                                                      "progress"
                                                   )
                                                   .requires(source -> source.m_6761_(4) && debugEnabled(source)))
                                                .then(
                                                   Commands.m_82127_("set")
                                                      .then(
                                                         Commands.m_82129_("seconds", IntegerArgumentType.integer(0))
                                                            .executes(
                                                               context -> progressSet(
                                                                  (CommandSourceStack)context.getSource(), IntegerArgumentType.getInteger(context, "seconds")
                                                               )
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.m_82127_("add")
                                                   .then(
                                                      Commands.m_82129_("seconds", IntegerArgumentType.integer())
                                                         .executes(
                                                            context -> progressAdd(
                                                               (CommandSourceStack)context.getSource(), IntegerArgumentType.getInteger(context, "seconds")
                                                            )
                                                         )
                                                   )
                                             ))
                                          .then(Commands.m_82127_("reset").executes(context -> progressReset((CommandSourceStack)context.getSource()))))
                                       .then(
                                          Commands.m_82127_("decay")
                                             .then(
                                                Commands.m_82129_("seconds", IntegerArgumentType.integer(0))
                                                   .executes(
                                                      context -> progressDecay(
                                                         (CommandSourceStack)context.getSource(), IntegerArgumentType.getInteger(context, "seconds")
                                                      )
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("debug")
                                                .requires(source -> source.m_6761_(4) && debugEnabled(source)))
                                             .then(
                                                ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                                                                  "visual"
                                                               )
                                                               .then(
                                                                  Commands.m_82127_("normal")
                                                                     .executes(
                                                                        context -> visual(
                                                                           (CommandSourceStack)context.getSource(),
                                                                           ExtractionPointVisualHelper.VisualMode.NORMAL
                                                                        )
                                                                     )
                                                               ))
                                                            .then(
                                                               Commands.m_82127_("capturing")
                                                                  .executes(
                                                                     context -> visual(
                                                                        (CommandSourceStack)context.getSource(),
                                                                        ExtractionPointVisualHelper.VisualMode.CAPTURING
                                                                     )
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.m_82127_("contested")
                                                               .executes(
                                                                  context -> visual(
                                                                     (CommandSourceStack)context.getSource(), ExtractionPointVisualHelper.VisualMode.CONTESTED
                                                                  )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.m_82127_("captured")
                                                            .executes(
                                                               context -> visual(
                                                                  (CommandSourceStack)context.getSource(), ExtractionPointVisualHelper.VisualMode.CAPTURED
                                                               )
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.m_82127_("ending")
                                                         .executes(
                                                            context -> visual(
                                                               (CommandSourceStack)context.getSource(), ExtractionPointVisualHelper.VisualMode.ENDING
                                                            )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             Commands.m_82127_("forceContested")
                                                .then(
                                                   Commands.m_82129_("value", BoolArgumentType.bool())
                                                      .executes(
                                                         context -> forceContested(
                                                            (CommandSourceStack)context.getSource(), BoolArgumentType.getBool(context, "value")
                                                         )
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.m_82127_("forceOwner")
                                             .then(Commands.m_82127_("self").executes(context -> forceOwnerSelf((CommandSourceStack)context.getSource())))
                                       ))
                                    .then(Commands.m_82127_("clearOwner").executes(context -> clearOwner((CommandSourceStack)context.getSource())))
                              ))
                           .then(
                              ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("compass")
                                          .requires(source -> source.m_6761_(4) && debugEnabled(source)))
                                       .then(
                                          ((LiteralArgumentBuilder)Commands.m_82127_("give")
                                                .executes(context -> compassGiveSelf((CommandSourceStack)context.getSource())))
                                             .then(
                                                Commands.m_82129_("player", EntityArgument.m_91466_())
                                                   .executes(
                                                      context -> compassGive(
                                                         (CommandSourceStack)context.getSource(), EntityArgument.m_91474_(context, "player")
                                                      )
                                                   )
                                             )
                                       ))
                                    .then(Commands.m_82127_("remove").executes(context -> compassRemoveSelf((CommandSourceStack)context.getSource()))))
                                 .then(Commands.m_82127_("removeAll").executes(context -> compassRemoveAll((CommandSourceStack)context.getSource())))
                           ))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("reward")
                                    .requires(source -> source.m_6761_(4) && debugEnabled(source)))
                                 .then(Commands.m_82127_("milestone").executes(context -> rewardMilestone((CommandSourceStack)context.getSource()))))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.m_82127_("final")
                                       .executes(context -> rewardFinalSelf((CommandSourceStack)context.getSource())))
                                    .then(
                                       Commands.m_82129_("player", EntityArgument.m_91466_())
                                          .executes(context -> rewardFinal((CommandSourceStack)context.getSource(), EntityArgument.m_91474_(context, "player")))
                                    )
                              )
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.m_82127_("bordercheck").requires(source -> source.m_6761_(4) && debugEnabled(source)))
                           .executes(context -> borderCheck((CommandSourceStack)context.getSource()))
                     ))
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("findpos")
                              .requires(source -> source.m_6761_(4) && debugEnabled(source)))
                           .executes(context -> findPos((CommandSourceStack)context.getSource(), -1)))
                        .then(
                           Commands.m_82129_("attempts", IntegerArgumentType.integer(1))
                              .executes(context -> findPos((CommandSourceStack)context.getSource(), IntegerArgumentType.getInteger(context, "attempts")))
                        )
                  )
            )
      );
   }

   private static boolean debugEnabled(CommandSourceStack source) {
      return ExtractionPointManager.getConfig(source.m_81377_()).debugCommandsEnabled;
   }

   private static int start(CommandSourceStack source) {
      boolean started = ExtractionPointManager.startRandom(source);
      if (!started) {
         source.m_81352_(Component.m_237113_("[ExtractionPoint] Не удалось найти валидную позицию."));
         return 0;
      } else {
         source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Event запущен около центра border."), true);
         return 1;
      }
   }

   private static int startHere(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.m_81375_();
      return startAt(source, player.m_20185_(), player.m_20186_(), player.m_20189_());
   }

   private static int startAt(CommandSourceStack source, double x, double y, double z) {
      boolean started = ExtractionPointManager.startAt(source, BlockPos.m_274561_(x, y, z));
      if (!started) {
         source.m_81352_(Component.m_237113_("[ExtractionPoint] Не удалось запустить event."));
         return 0;
      } else {
         source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Event запущен: " + format(BlockPos.m_274561_(x, y, z))), true);
         return 1;
      }
   }

   private static int stop(CommandSourceStack source) {
      ExtractionPointManager.stopExpired(source.m_81377_());
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Event завершен без победителя."), true);
      return 1;
   }

   private static int cleanup(CommandSourceStack source) {
      ExtractionPointManager.reset(source.m_81377_());
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] State очищен."), true);
      return 1;
   }

   private static int status(CommandSourceStack source) {
      ExtractionPointData data = ExtractionPointManager.getData();
      ServerLevel level = source.m_81377_().m_129783_();
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] state=" + data.state), false);
      source.m_288197_(() -> Component.m_237113_("- eventId=" + value(data.eventId)), false);
      source.m_288197_(() -> Component.m_237113_("- center=" + format(data.center)), false);
      source.m_288197_(() -> Component.m_237113_("- radius=" + data.radius + ", halfHeight=" + data.halfHeight), false);
      source.m_288197_(
         () -> Component.m_237113_(
            "- globalProgressSeconds=" + data.globalCaptureProgressTicks / 20 + ", requiredCaptureSeconds=" + Math.max(1, data.requiredCaptureTicks / 20)
         ),
         false
      );
      source.m_288197_(
         () -> Component.m_237113_("- currentOwnerPlayer=" + value(data.currentOwnerPlayerId) + ", currentOwnerTeam=" + value(data.currentOwnerTeamId)), false
      );
      source.m_288197_(() -> Component.m_237113_("- continuousOwnerSeconds=" + data.continuousOwnerCaptureTicks / 20 + ", contested=" + data.contested), false);
      source.m_288197_(() -> Component.m_237113_("- playersInside=" + data.playersInside), false);
      source.m_288197_(() -> Component.m_237113_("- teamsInside=" + data.teamsInside), false);
      source.m_288197_(
         () -> Component.m_237113_(
            "- matchTime="
               + ExtractionPointManager.getMatchTimeSeconds(source.m_81377_())
               + ", timeUntilExpire="
               + Math.max(0L, (data.expireAtMatchTick - source.m_81377_().m_129921_()) / 20L)
         ),
         false
      );
      source.m_288197_(
         () -> Component.m_237113_(
            "- distanceToNearestBorderSide=" + String.format(Locale.ROOT, "%.2f", ExtractionPointManager.distanceToNearestBorderSide(level, data.center))
         ),
         false
      );
      return 1;
   }

   private static int teleport(CommandSourceStack source, boolean edge) throws CommandSyntaxException {
      ServerPlayer player = source.m_81375_();
      ExtractionPointData data = ExtractionPointManager.getData();
      if (data.center == null) {
         source.m_81352_(Component.m_237113_("[ExtractionPoint] Активной точки нет."));
         return 0;
      } else {
         double x = data.center.m_123341_() + 0.5 + (edge ? data.radius : 0.0);
         double y = data.center.m_123342_() + 1.0;
         double z = data.center.m_123343_() + 0.5;
         player.m_8999_(source.m_81372_(), x, y, z, player.m_146908_(), player.m_146909_());
         return 1;
      }
   }

   private static int progressSet(CommandSourceStack source, int seconds) {
      ExtractionPointManager.setProgressSeconds(seconds);
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Progress set: " + seconds + "s"), true);
      return 1;
   }

   private static int progressAdd(CommandSourceStack source, int seconds) {
      ExtractionPointManager.addProgressSeconds(seconds);
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Progress add: " + seconds + "s"), true);
      return 1;
   }

   private static int progressReset(CommandSourceStack source) {
      ExtractionPointManager.resetProgress();
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Progress reset."), true);
      return 1;
   }

   private static int progressDecay(CommandSourceStack source, int seconds) {
      ExtractionPointManager.decayProgressSeconds(seconds);
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Progress decayed: " + seconds + "s"), true);
      return 1;
   }

   private static int visual(CommandSourceStack source, ExtractionPointVisualHelper.VisualMode mode) {
      ExtractionPointManager.setDebugVisualMode(mode);
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Debug visual=" + mode), true);
      return 1;
   }

   private static int forceContested(CommandSourceStack source, boolean value) {
      ExtractionPointManager.setForcedContested(value);
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] forceContested=" + value), true);
      return 1;
   }

   private static int forceOwnerSelf(CommandSourceStack source) throws CommandSyntaxException {
      ExtractionPointManager.forceOwner(source.m_81375_());
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Owner forced to self."), true);
      return 1;
   }

   private static int clearOwner(CommandSourceStack source) {
      ExtractionPointManager.clearOwner();
      ExtractionPointManager.setForcedContested(null);
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Debug owner/contested cleared."), true);
      return 1;
   }

   private static int compassGiveSelf(CommandSourceStack source) throws CommandSyntaxException {
      return compassGive(source, source.m_81375_());
   }

   private static int compassGive(CommandSourceStack source, ServerPlayer player) {
      ExtractionCompassHelper.giveOrUpdate(player, ExtractionPointManager.getData(), source.m_81372_().m_46472_());
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Compass given: " + player.m_7755_().getString()), true);
      return 1;
   }

   private static int compassRemoveSelf(CommandSourceStack source) throws CommandSyntaxException {
      ExtractionCompassHelper.removeAllExtractionCompasses(source.m_81375_());
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Compass removed."), true);
      return 1;
   }

   private static int compassRemoveAll(CommandSourceStack source) {
      for (ServerPlayer player : source.m_81377_().m_6846_().m_11314_()) {
         ExtractionCompassHelper.removeAllExtractionCompasses(player);
      }

      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] All compasses removed."), true);
      return 1;
   }

   private static int rewardMilestone(CommandSourceStack source) throws CommandSyntaxException {
      ExtractionPointManager.rewardMilestone(source.m_81375_());
      return 1;
   }

   private static int rewardFinalSelf(CommandSourceStack source) throws CommandSyntaxException {
      return rewardFinal(source, source.m_81375_());
   }

   private static int rewardFinal(CommandSourceStack source, ServerPlayer player) {
      ExtractionPointManager.rewardFinal(player);
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] Final reward issued: " + player.m_7755_().getString()), true);
      return 1;
   }

   private static int borderCheck(CommandSourceStack source) {
      ExtractionPointData data = ExtractionPointManager.getData();
      ServerLevel level = source.m_81377_().m_129783_();
      WorldBorder border = level.m_6857_();
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] borderSize=" + border.m_61959_()), false);
      source.m_288197_(() -> Component.m_237113_("- borderCenter=" + border.m_6347_() + " " + border.m_6345_()), false);
      source.m_288197_(() -> Component.m_237113_("- eventCenter=" + format(data.center)), false);
      source.m_288197_(
         () -> Component.m_237113_(
            "- captureRadius=" + data.radius + ", safetyMargin=" + ExtractionPointManager.getConfig(source.m_81377_()).borderSafetyMargin
         ),
         false
      );
      source.m_288197_(
         () -> Component.m_237113_(
            "- distanceToNearestBorderSide=" + String.format(Locale.ROOT, "%.2f", ExtractionPointManager.distanceToNearestBorderSide(level, data.center))
         ),
         false
      );
      source.m_288197_(() -> Component.m_237113_("- willExpireByBorder=" + ExtractionPointManager.willExpireByBorder(level)), false);
      return 1;
   }

   private static int findPos(CommandSourceStack source, int attempts) {
      int actualAttempts = attempts > 0 ? attempts : ExtractionPointManager.getConfig(source.m_81377_()).maxLocationAttempts;
      ExtractionPointManager.FindPositionResult result = ExtractionPointManager.findPosition(source.m_81377_(), actualAttempts);
      source.m_288197_(() -> Component.m_237113_("[ExtractionPoint] findpos attempts=" + actualAttempts), false);
      source.m_288197_(() -> Component.m_237113_("- rejected_water=" + result.rejectedWater), false);
      source.m_288197_(() -> Component.m_237113_("- rejected_lava=" + result.rejectedLava), false);
      source.m_288197_(() -> Component.m_237113_("- rejected_y_too_low=" + result.rejectedYTooLow), false);
      source.m_288197_(() -> Component.m_237113_("- rejected_y_too_high=" + result.rejectedYTooHigh), false);
      source.m_288197_(() -> Component.m_237113_("- rejected_border=" + result.rejectedBorder), false);
      source.m_288197_(() -> Component.m_237113_("- accepted=" + result.accepted + ", pos=" + format(result.acceptedPos)), false);
      return result.accepted > 0 ? 1 : 0;
   }

   private static String format(BlockPos pos) {
      return pos == null ? "-" : pos.m_123341_() + " " + pos.m_123342_() + " " + pos.m_123343_();
   }

   private static String value(Object value) {
      return value == null ? "-" : value.toString();
   }
}
