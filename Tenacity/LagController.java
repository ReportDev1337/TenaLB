package dev.tenacity.utils.lag;

import dev.tenacity.event.Event;
import dev.tenacity.event.EventListener;
import dev.tenacity.event.EventProtocol;
import dev.tenacity.event.impl.game.TickEvent;
import dev.tenacity.event.impl.game.WorldEvent;
import dev.tenacity.event.impl.network.PacketReceiveEvent;
import dev.tenacity.event.impl.network.PacketSendEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LagController implements EventListener {
   private final Map<String, PacketQueue> queues = new LinkedHashMap();

   public LagController(EventProtocol eventProtocol) {
      eventProtocol.register(this);
   }

   public void registerQueue(String name, PacketQueue queue) {
      this.queues.put(name, queue);
   }

   public void unregisterQueue(String name) {
      PacketQueue q = (PacketQueue)this.queues.remove(name);
      if (q != null && q.isActive()) {
         q.flush();
      }

   }

   public void stopAndDrop(String name) {
      PacketQueue q = (PacketQueue)this.queues.remove(name);
      if (q != null) {
         q.clear();
         q.stop();
      }

   }

   public PacketQueue getQueue(String name) {
      return (PacketQueue)this.queues.get(name);
   }

   public BlinkSession blinkTicks(int ticks) {
      final String name = "blink_" + System.nanoTime();
      PacketQueue queue = new PacketQueue(PacketFilter.C03, FlushMode.TICK_LIMITED, ticks, 0L, 0, 0);
      this.registerQueue(name, queue);
      queue.start();
      BlinkSession session = new BlinkSession(queue) {
         public void cancel() {
            super.cancel();
            LagController.this.unregisterQueue(name);
         }

         public void finish() {
            super.finish();
            LagController.this.unregisterQueue(name);
         }
      };
      return session;
   }

   public void onEvent(Event event) {
      if (event instanceof WorldEvent) {
         this.flushAll();
      } else if (event instanceof PacketSendEvent) {
         this.onPacketSend((PacketSendEvent)event);
      } else if (event instanceof PacketReceiveEvent) {
         this.onPacketReceive((PacketReceiveEvent)event);
      } else if (event instanceof TickEvent) {
         this.onTick((TickEvent)event);
      }

   }

   private void flushAll() {
      for(PacketQueue queue : this.queues.values()) {
         if (queue.isActive()) {
            queue.flush();
         }

         queue.clear();
      }

      this.queues.clear();
   }

   private void onPacketSend(PacketSendEvent event) {
      if (!event.isCancelled()) {
         for(PacketQueue queue : this.queues.values()) {
            if (queue.isActive() && queue.filter.matches(event.getPacket(), Direction.OUTBOUND)) {
               queue.enqueue(event.getPacket(), Direction.OUTBOUND);
               event.cancel();
               return;
            }
         }

      }
   }

   private void onPacketReceive(PacketReceiveEvent event) {
      if (!event.isCancelled()) {
         for(PacketQueue queue : this.queues.values()) {
            if (queue.isActive() && queue.filter.matches(event.getPacket(), Direction.INBOUND)) {
               queue.enqueue(event.getPacket(), Direction.INBOUND);
               event.cancel();
               return;
            }
         }

      }
   }

   private void onTick(TickEvent event) {
      if (event.isPre()) {
         List<String> toRemove = null;

         for(Map.Entry<String, PacketQueue> entry : this.queues.entrySet()) {
            PacketQueue queue = (PacketQueue)entry.getValue();
            if (queue.isActive()) {
               queue.tick();
               if (!queue.isActive()) {
                  if (toRemove == null) {
                     toRemove = new ArrayList();
                  }

                  toRemove.add(entry.getKey());
               }
            }
         }

         if (toRemove != null) {
            for(String name : toRemove) {
               this.queues.remove(name);
            }
         }

      }
   }
}
