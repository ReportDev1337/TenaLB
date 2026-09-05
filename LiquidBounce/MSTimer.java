/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.utils.timer;

public final class MSTimer {
    public long time = -1L;
    private long prevMS = this.getTime();
    private long lastMs;
    public boolean hasTimePassed(final long MS) {
        return System.currentTimeMillis() >= time + MS;
    }
    public long getTime() {
        return System.nanoTime() / 1000000L;
    }
    public long hasTimeLeft(final long MS) {
        return (MS + time) - System.currentTimeMillis();
    }
    public boolean delay(float milliSec) {
        return (float)(this.getTime() - this.prevMS) >= milliSec;
    }
    public void reset() {
        time = System.currentTimeMillis();
    }
    public void reSet() {
        this.lastMs = this.getTime();
    }
    public boolean hasPassed(double milli) {
        return (double)(this.getTime() - this.lastMs) >= milli;
    }
}
