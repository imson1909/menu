package com.makar.tacticaltablet.tablet.client;

import com.makar.tacticaltablet.game.MatchPhase;
import com.makar.tacticaltablet.tablet.TacticalTabletItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "tacticaltablet", value = Dist.CLIENT)
public class ClientEvents {
   @SubscribeEvent
   public static void onUse(InteractionKeyMappingTriggered event) {
      if (event.isUseItem()) {
         Minecraft mc = Minecraft.m_91087_();
         Player player = mc.f_91074_;
         if (player != null) {
            if (hasTabletInHand(player)) {
               mc.m_91152_(createScreenForCurrentPhase());
            }
         }
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91073_ != null && mc.f_91074_ != null) {
            Screen current = mc.f_91080_;
            boolean voting = TabletClientState.getMatchPhase() == MatchPhase.VOTING;
            boolean teamSelect = TabletClientState.getMatchPhase() == MatchPhase.TEAM_SELECT;
            boolean mapVoting = TabletClientState.getMatchPhase() == MatchPhase.MAP_VOTING && MapVoteClientState.isActive();
            if (mapVoting && !(current instanceof MapVotingScreen)) {
               mc.m_91152_(new MapVotingScreen());
            } else if (current instanceof MapVotingScreen && !mapVoting) {
               mc.m_91152_(null);
            } else if (current instanceof VotingScreen && teamSelect) {
               mc.m_91152_(new TeamSelectScreen());
            } else if (current instanceof TeamSelectScreen && voting) {
               mc.m_91152_(new VotingScreen());
            } else {
               if (!voting && !teamSelect && (current instanceof VotingScreen || current instanceof TeamSelectScreen)) {
                  mc.m_91152_(null);
               }
            }
         }
      }
   }

   private static boolean hasTabletInHand(Player player) {
      return player == null ? false : isTablet(player.m_21120_(InteractionHand.MAIN_HAND)) || isTablet(player.m_21120_(InteractionHand.OFF_HAND));
   }

   private static boolean isTablet(ItemStack stack) {
      return !stack.m_41619_() && stack.m_41720_() instanceof TacticalTabletItem;
   }

   private static Screen createScreenForCurrentPhase() {
      MatchPhase phase = TabletClientState.getMatchPhase();
      if (phase == MatchPhase.MAP_VOTING) {
         return new MapVotingScreen();
      } else if (phase == MatchPhase.VOTING) {
         return new VotingScreen();
      } else {
         return (Screen)(phase == MatchPhase.TEAM_SELECT ? new TeamSelectScreen() : new TabletScreen());
      }
   }
}
