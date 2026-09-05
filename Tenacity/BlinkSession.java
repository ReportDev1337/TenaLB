package dev.tenacity.utils.lag;

public class BlinkSession {
   private final PacketQueue queue;

   BlinkSession(PacketQueue queue) {
      this.queue = queue;
   }

   public boolean isComplete() {
      return !this.queue.isActive();
   }

   public int getRemainingTicks() {
      return this.queue.remainingTicks();
   }

   public void cancel() {
      if (this.queue.isActive()) {
         this.queue.clear();
         this.queue.stop();
      }

   }

   public void finish() {
      if (this.queue.isActive()) {
         this.queue.flush();
         this.queue.stop();
      }

   }
}
