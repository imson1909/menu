package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.map.MapRotationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.io.IOException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class MapRotationCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                                       "ttmaprotation"
                                    )
                                    .requires(source -> source.m_6761_(2)))
                                 .executes(ctx -> status((CommandSourceStack)ctx.getSource())))
                              .then(Commands.m_82127_("status").executes(ctx -> status((CommandSourceStack)ctx.getSource()))))
                           .then(Commands.m_82127_("list").executes(ctx -> list((CommandSourceStack)ctx.getSource()))))
                        .then(Commands.m_82127_("arm").executes(ctx -> arm((CommandSourceStack)ctx.getSource()))))
                     .then(Commands.m_82127_("disarm").executes(ctx -> disarm((CommandSourceStack)ctx.getSource()))))
                  .then(Commands.m_82127_("reload").executes(ctx -> reload((CommandSourceStack)ctx.getSource()))))
               .then(
                  Commands.m_82127_("next")
                     .then(
                        Commands.m_82129_("map", StringArgumentType.greedyString())
                           .executes(ctx -> setNextMap((CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "map")))
                     )
               ))
            .then(Commands.m_82127_("clear-next").executes(ctx -> clearNextMap((CommandSourceStack)ctx.getSource())))
      );
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("ttmaps").requires(source -> source.m_6761_(2)))
            .redirect(dispatcher.getRoot().getChild("ttmaprotation"))
      );
   }

   private static int status(CommandSourceStack source) {
      MapRotationManager.RotationStatus status = MapRotationManager.getStatus(source.m_81377_());
      source.m_288197_(() -> Component.m_237113_("Ротация карт тактического планшета:"), false);
      source.m_288197_(() -> Component.m_237113_("- включена: " + status.enabled()), false);
      source.m_288197_(() -> Component.m_237113_("- менять при выключении: " + status.rotateEveryShutdown()), false);
      source.m_288197_(() -> Component.m_237113_("- подготовлена к следующему выключению: " + status.armed()), false);
      source.m_288197_(() -> Component.m_237113_("- текущая карта: " + emptyToDash(status.currentMap())), false);
      source.m_288197_(() -> Component.m_237113_("- следующая карта: " + emptyToDash(status.nextMap())), false);
      source.m_288197_(() -> Component.m_237113_("- папка карт: " + status.mapsRoot()), false);
      source.m_288197_(() -> Component.m_237113_("- найдено карт: " + status.maps().size()), false);
      if (status.lastError() != null && !status.lastError().isBlank()) {
         source.m_81352_(Component.m_237113_("Последняя ошибка: " + status.lastError()));
      }

      return status.maps().size();
   }

   private static int list(CommandSourceStack source) {
      List<String> maps = MapRotationManager.listMapNames(source.m_81377_());
      if (maps.isEmpty()) {
         source.m_81352_(Component.m_237113_("В map_pool не найдено подходящих карт."));
         return 0;
      } else {
         source.m_288197_(() -> Component.m_237113_("Доступные карты: " + String.join(", ", maps)), false);
         return maps.size();
      }
   }

   private static int arm(CommandSourceStack source) {
      try {
         MapRotationManager.arm(source.m_81377_());
         MapRotationManager.RotationStatus status = MapRotationManager.getStatus(source.m_81377_());
         source.m_288197_(
            () -> Component.m_237113_(
               "Ротация карт подготовлена. При следующем корректном выключении будет установлена карта: " + emptyToDash(status.nextMap())
            ),
            true
         );
         return 1;
      } catch (IOException exception) {
         source.m_81352_(Component.m_237113_("Не удалось подготовить ротацию карт: " + exception.getMessage()));
         return 0;
      }
   }

   private static int disarm(CommandSourceStack source) {
      try {
         MapRotationManager.disarm(source.m_81377_());
         source.m_288197_(() -> Component.m_237113_("Ротация карт снята с подготовки."), true);
         return 1;
      } catch (IOException exception) {
         source.m_81352_(Component.m_237113_("Не удалось снять подготовку ротации карт: " + exception.getMessage()));
         return 0;
      }
   }

   private static int reload(CommandSourceStack source) {
      try {
         MapRotationManager.reload(source.m_81377_());
         source.m_288197_(() -> Component.m_237113_("Конфиг ротации карт перезагружен."), true);
         return 1;
      } catch (RuntimeException exception) {
         source.m_81352_(Component.m_237113_("Не удалось перезагрузить конфиг ротации карт: " + exception.getMessage()));
         return 0;
      }
   }

   private static int setNextMap(CommandSourceStack source, String mapName) {
      try {
         MapRotationManager.setNextMap(source.m_81377_(), mapName);
         source.m_288197_(() -> Component.m_237113_("Следующая карта: " + mapName), true);
         return 1;
      } catch (IOException exception) {
         source.m_81352_(Component.m_237113_("Не удалось выбрать следующую карту: " + exception.getMessage()));
         return 0;
      }
   }

   private static int clearNextMap(CommandSourceStack source) {
      try {
         MapRotationManager.clearNextMapOverride(source.m_81377_());
         source.m_288197_(() -> Component.m_237113_("Принудительный выбор следующей карты очищен."), true);
         return 1;
      } catch (IOException exception) {
         source.m_81352_(Component.m_237113_("Не удалось очистить выбор следующей карты: " + exception.getMessage()));
         return 0;
      }
   }

   private static String emptyToDash(String value) {
      return value != null && !value.isBlank() ? value : "-";
   }
}
