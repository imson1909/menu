package com.makar.tacticaltablet.stats;

import java.util.Locale;
import java.util.Objects;

public final class PlayerStats {
   private final String name;
   private final int kills;
   private final int deaths;
   private final int wins;
   private final int matchesPlayed;
   private final int coins;

   public PlayerStats(String name, int kills, int deaths, int wins, int matchesPlayed, int coins) {
      this.name = sanitizeName(name);
      this.kills = Math.max(0, kills);
      this.deaths = Math.max(0, deaths);
      this.wins = Math.max(0, wins);
      this.matchesPlayed = Math.max(0, matchesPlayed);
      this.coins = Math.max(0, coins);
   }

   public String getName() {
      return this.name;
   }

   public int getKills() {
      return this.kills;
   }

   public int getDeaths() {
      return this.deaths;
   }

   public int getWins() {
      return this.wins;
   }

   public int getMatchesPlayed() {
      return this.matchesPlayed;
   }

   public int getCoins() {
      return this.coins;
   }

   public double getKd() {
      return this.deaths <= 0 ? this.kills : (double)this.kills / this.deaths;
   }

   public String getFormattedKd() {
      return String.format(Locale.US, "%.2f", this.getKd());
   }

   private static String sanitizeName(String rawName) {
      String value = Objects.toString(rawName, "unknown").replace('\n', '_').replace('\r', '_').replace('`', '\'').trim();
      if (value.isBlank()) {
         return "unknown";
      } else {
         return value.length() > 17 ? value.substring(0, 17) : value;
      }
   }
}
