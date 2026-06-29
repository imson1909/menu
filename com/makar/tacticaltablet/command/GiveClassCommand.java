package com.makar.tacticaltablet.command;

import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class GiveClassCommand {
   private static final List<String> CLASS_KEYS = List.of("killer", "miniboss", "shahed", "krot");

   private GiveClassCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("ttgiveclass").requires(source -> source.m_6761_(2)))
               .then(
                  Commands.m_82129_("class", StringArgumentType.string())
                     .suggests((context, builder) -> SharedSuggestionProvider.m_82970_(CLASS_KEYS, builder))
                     .executes(
                        context -> grant(
                           (CommandSourceStack)context.getSource(),
                           List.of(((CommandSourceStack)context.getSource()).m_81375_()),
                           StringArgumentType.getString(context, "class")
                        )
                     )
               ))
            .then(
               Commands.m_82129_("targets", EntityArgument.m_91470_())
                  .then(
                     Commands.m_82129_("class", StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.m_82970_(CLASS_KEYS, builder))
                        .executes(
                           context -> grant(
                              (CommandSourceStack)context.getSource(),
                              EntityArgument.m_91477_(context, "targets"),
                              StringArgumentType.getString(context, "class")
                           )
                        )
                  )
            )
      );
   }

   private static int grant(CommandSourceStack source, Collection<ServerPlayer> targets, String requestedClass) {
      String classKey = normalizeClassKey(requestedClass);
      if (classKey == null) {
         source.m_81352_(Component.m_237113_("Неизвестный эксклюзивный класс. Доступно: killer, miniboss, shahed, krot."));
         return 0;
      }

      int granted = 0;
      int alreadyOwned = 0;

      for (ServerPlayer target : targets) {
         if (PlayerProgressManager.grantExclusiveClass(target, classKey)) {
            PlayerProgressManager.savePlayer(target);
            ClassXPManager.sync(target);
            target.m_213846_(Component.m_237113_("[WAR] Тебе выдан эксклюзивный класс " + displayName(classKey) + "."));
            granted++;
         } else if (PlayerProgressManager.isExclusiveClassGranted(target, classKey)) {
            alreadyOwned++;
         }
      }

      int grantedCount = granted;
      int alreadyOwnedCount = alreadyOwned;
      source.m_288197_(
         () -> Component.m_237113_("Класс " + displayName(classKey) + ": выдан " + grantedCount + ", уже был выдан " + alreadyOwnedCount + "."), true
      );
      return granted;
   }

   private static String normalizeClassKey(String value) {
      if (value == null) {
         return null;
      }

      return switch (value.trim().toLowerCase(Locale.ROOT)) {
         case "killer", "киллер" -> "killer";
         case "miniboss", "mini-boss", "mini_boss", "мини-босс", "минибосс", "мини_босс" -> "miniboss";
         case "shahed", "shahedop", "shahed_op", "shahed-op", "шахед", "шахедоп", "шахед_оп", "шахед-оп", "шахед оп." -> "shahed";
         case "krot", "крот" -> "krot";
         default -> null;
      };
   }

   private static String displayName(String classKey) {
      return switch (classKey) {
         case "miniboss" -> "Мини-Босс";
         case "shahed" -> "Шахед оп.";
         case "krot" -> "Крот";
         default -> "Киллер";
      };
   }
}
