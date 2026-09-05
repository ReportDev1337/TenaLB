package dev.tenacity.utils.lag;

import java.util.function.Predicate;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

public class PacketFilter {
   public final Direction direction;
   public final Predicate<Packet<?>> predicate;
   public static PacketFilter C03;
   public static PacketFilter S08;
   public static PacketFilter S12;
   public static PacketFilter S27;
   public static PacketFilter ALL_OUTBOUND;
   public static PacketFilter ALL_INBOUND;
   public static PacketFilter ALL;

   public PacketFilter(Direction direction, Predicate<Packet<?>> predicate) {
      this.direction = direction;
      this.predicate = predicate;
   }

   public boolean matches(Packet<?> packet, Direction eventDirection) {
      return this.direction != Direction.BOTH && this.direction != eventDirection ? false : this.predicate.test(packet);
   }

   public static PacketFilter types(Direction direction, Class<?>... types) {
      return new PacketFilter(direction, (p) -> {
         for(Class<?> t : types) {
            if (t.isInstance(p)) {
               return true;
            }
         }

         return false;
      });
   }

   public static PacketFilter onlyAnd(PacketFilter a, PacketFilter b) {
      Direction dir = a.direction == b.direction ? a.direction : Direction.BOTH;
      return new PacketFilter(dir, (p) -> a.predicate.test(p) && b.predicate.test(p));
   }

   static {
      C03 = new PacketFilter(Direction.OUTBOUND, (p) -> p instanceof C03PacketPlayer);
      S08 = new PacketFilter(Direction.INBOUND, (p) -> p instanceof S08PacketPlayerPosLook);
      S12 = new PacketFilter(Direction.INBOUND, (p) -> p instanceof S12PacketEntityVelocity);
      S27 = new PacketFilter(Direction.INBOUND, (p) -> p instanceof S27PacketExplosion);
      ALL_OUTBOUND = new PacketFilter(Direction.OUTBOUND, (p) -> true);
      ALL_INBOUND = new PacketFilter(Direction.INBOUND, (p) -> true);
      ALL = new PacketFilter(Direction.BOTH, (p) -> true);
   }
}
