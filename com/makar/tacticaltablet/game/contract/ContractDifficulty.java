package com.makar.tacticaltablet.game.contract;

public enum ContractDifficulty {
   LOW(2, 10, -10027162, "Лёгкий"),
   MEDIUM(5, 20, -9882, "Средний"),
   HIGH(15, 40, -43691, "Высокий");

   private final int price;
   private final int reward;
   private final int color;
   private final String displayName;

   ContractDifficulty(int price, int reward, int color, String displayName) {
      this.price = price;
      this.reward = reward;
      this.color = color;
      this.displayName = displayName;
   }

   public int price() {
      return this.price;
   }

   public int reward() {
      return this.reward;
   }

   public int color() {
      return this.color;
   }

   public String displayName() {
      return this.displayName;
   }

   public static ContractDifficulty forCareerPercent(int percent) {
      if (percent >= 60) {
         return HIGH;
      } else {
         return percent >= 30 ? MEDIUM : LOW;
      }
   }
}
