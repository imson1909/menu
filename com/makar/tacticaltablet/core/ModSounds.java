package com.makar.tacticaltablet.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "tacticaltablet");
   public static final RegistryObject<SoundEvent> PARACHUTE_CLOSE = SOUND_EVENTS.register(
      "parachute_close", () -> SoundEvent.m_262856_(new ResourceLocation("tacticaltablet", "parachute_close"), 16.0F)
   );

   private ModSounds() {
   }
}
