package dev.tenacity.utils.lag;

import dev.tenacity.utils.Utils;
import dev.tenacity.utils.server.PacketUtils;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Map;
import net.minecraft.network.Packet;

public class PacketQueue implements Utils {
   public final PacketFilter filter;
   public final FlushMode flushMode;
   public final Direction direction;
   public final int maxTicks;
   public final long delayMs;
   public final int intervalTicks;
   public final int maxPerTick;
   private boolean active;
   private int tickCount;
   private long startTime;
   private final ArrayDeque<Map.Entry<Packet<?>, Direction>> queue;

   public PacketQueue(PacketFilter filter, FlushMode flushMode) {
      this(filter, flushMode, 0, 0L, 0, 0);
   }

   public PacketQueue(PacketFilter filter, FlushMode flushMode, int maxTicks, long delayMs, int intervalTicks, int maxPerTick) {
      this.queue = new ArrayDeque();
      this.filter = filter;
      this.flushMode = flushMode;
      this.direction = filter.direction;
      this.maxTicks = maxTicks;
      this.delayMs = delayMs;
      this.intervalTicks = intervalTicks;
      this.maxPerTick = maxPerTick;
   }

   public void start() {
      this.active = true;
      this.tickCount = 0;
      this.startTime = System.currentTimeMillis();
   }

   public void stop() {
      this.active = false;
   }

   public boolean isActive() {
      return this.active;
   }

   public boolean isEmpty() {
      return this.queue.isEmpty();
   }

   public int size() {
      return this.queue.size();
   }

   public int remainingTicks() {
      return this.active && this.flushMode == FlushMode.TICK_LIMITED ? Math.max(0, this.maxTicks - this.tickCount) : 0;
   }

   public void enqueue(Packet<?> packet, Direction dir) {
      this.queue.add(new AbstractMap.SimpleEntry(packet, dir));
   }

   public void tick() {
      if (this.active) {
         ++this.tickCount;
         switch (this.flushMode) {
            case TICK_LIMITED:
               if (this.tickCount >= this.maxTicks) {
                  this.flush();
                  this.stop();
               }
               break;
            case TIME_LIMITED:
               if (System.currentTimeMillis() - this.startTime >= this.delayMs) {
                  this.flush();
                  this.stop();
               }
               break;
            case PULSE:
               if (this.intervalTicks > 0 && this.tickCount % this.intervalTicks == 0) {
                  for(int released = 0; !this.queue.isEmpty() && (this.maxPerTick == 0 || released < this.maxPerTick); ++released) {
                     this.send((Map.Entry)this.queue.poll());
                  }
               }
            case MANUAL:
         }

      }
   }

   public void flush() {
      while(!this.queue.isEmpty()) {
         this.send((Map.Entry)this.queue.poll());
      }

   }

   public void flush(int maxPackets) {
      for(int released = 0; !this.queue.isEmpty() && released < maxPackets; ++released) {
         this.send((Map.Entry)this.queue.poll());
      }

   }

   public void clear() {
      this.queue.clear();
   }

   private void send(Map.Entry<Packet<?>, Direction> entry) {
      if (mc.getNetHandler() != null) {
         Packet<?> packet = (Packet)entry.getKey();
         switch ((Direction)entry.getValue()) {
            case OUTBOUND:
               PacketUtils.sendPacketNoEvent(packet);
               break;
            case INBOUND:
               try {
                  packet.processPacket(mc.getNetHandler().getNetworkManager().getNetHandler());
               } catch (Exception var4) {
               }
               break;
            default:
               PacketUtils.sendPacketNoEvent(packet);
         }

      }
   }
}
