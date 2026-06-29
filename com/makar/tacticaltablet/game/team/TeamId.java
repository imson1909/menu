package com.makar.tacticaltablet.game.team;

import java.util.Locale;
import net.minecraft.ChatFormatting;

public enum TeamId {
   ALFA("Альфа", ChatFormatting.RED, -43691),
   BETA("Бета", ChatFormatting.BLUE, -11167233),
   GAMMA("Гамма", ChatFormatting.GREEN, -11141291),
   DELTA("Дельта", ChatFormatting.YELLOW, -171);

   private final String displayName;
   private final ChatFormatting chatColor;
   private final int textColor;

   TeamId(String displayName, ChatFormatting chatColor, int textColor) {
      this.displayName = displayName;
      this.chatColor = chatColor;
      this.textColor = textColor;
   }

   public String displayName() {
      return this.displayName;
   }

   public ChatFormatting chatColor() {
      return this.chatColor;
   }

   public int textColor() {
      return this.textColor;
   }

   public String scoreboardName() {
      return "tt_" + this.name().toLowerCase(Locale.ROOT);
   }

   public static TeamId byId(int id) {
      TeamId[] values = values();
      if (id >= 0 && id < values.length) {
         return values[id];
      } else {
         throw new IllegalArgumentException("Invalid team id: " + id);
      }
   }
}
