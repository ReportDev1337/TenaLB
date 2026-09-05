package dev.tenacity.event.impl.game;

import dev.tenacity.event.Event;
import net.minecraft.world.World;

public class WorldEvent extends Event {
   public final World world;

   public WorldEvent(World world) {
      this.world = world;
   }

   public static class Load extends WorldEvent {
      public Load(World world) {
         super(world);
      }
   }

   public static class Unload extends WorldEvent {
      public Unload(World world) {
         super(world);
      }
   }
}
