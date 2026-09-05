package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.report.*;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.notification.Notification;
import net.ccbluex.liquidbounce.notification.NotificationManager;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.misc.SoundFxPlayer;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
        name = "Speed",
        description = "Allows you to move faster.",
        category = ModuleCategory.MOVEMENT,
        keyBind = Keyboard.KEY_C
)
public class Speed extends Module {

    public float speed = 0.0f;
    public float setspeed(float speeds){
        return this.speed = speeds;
    }
    public float getspeed() {
        return this.speed;
    }
    public int stoptick;
    public boolean yn = false;

    private final SpeedMode[] speedModes = new SpeedMode[] {
            // Report.
            new AAC4Hop(),
            new SlowDown(),
            new NewAAC4Hop(),
            new BlocksMC(),
            new NewHypixelHop(),
            new BlocksMCBhop(),
            new NewBlocksMC()
    };

    public final ListValue modeValue = new ListValue("Mode", getModes(), "BlocksMC") {

        @Override
        protected void onChange(final String oldValue, final String newValue) {
            if(getState())
                onDisable();
        }

        @Override
        protected void onChanged(final String oldValue, final String newValue) {
            if(getState())
                onEnable();
        }
    };

    public final ListValue newHypixelHopMode = new ListValue("NewHypixelHopMode", new String[]{"SlowHop","Normal"} , "Normal");
    public final FloatValue newHypixelHopDamageBoostValue = new FloatValue("NewHypixelHopDamageBoost", 0.45F, 0, 1F);
    public final FloatValue motionYValue = new FloatValue("MotionY", 0.42f, 0.0f, 0.42f);
    public final FloatValue movementSpeedValue = new FloatValue("MovementSpeed", 0.475f, 0.3f, 2.14f);
    public final BoolValue oldHypixel = new BoolValue("OldBMC", true);
    public final BoolValue lagbackcheckValue = new BoolValue("LagBack", true);
    public final BoolValue stairDebugValue = new BoolValue("Stair-Debug", true);
    public final IntegerValue stopticksValue = new IntegerValue("StopTicks",12,2,20);
    public final BoolValue damageBoostValue  = new BoolValue("Damage-Boost", true);
    public final BoolValue damageCustomValue  = new BoolValue("Custom-Boost", true);
    public final FloatValue damageValue = new FloatValue("Boost-Value", 0.71F, 0.1F, 2.0F);
    public final BoolValue lowHopValue  = new BoolValue("LowHop", true);
    public int stopTicks;

    @EventTarget
    public void onJump(final JumpEvent event) {
        if(mc.thePlayer == null || stopTicks > 0)
            return;

        final SpeedMode speedMode = getMode();

        if (speedMode != null)
            speedMode.onJump(event);
    }


    @EventTarget
    public void onPacket(final PacketEvent event) {
        if (this.lagbackcheckValue.get()) {
            if (event.getPacket() instanceof S08PacketPlayerPosLook) {
                S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook)event.getPacket();
                packet.yaw = mc.thePlayer.rotationYaw;
                packet.pitch = mc.thePlayer.rotationPitch;
                mc.thePlayer.motionX *= 0.0;
                mc.thePlayer.motionZ *= 0.0;
                this.stopTicks = stopticksValue.get();
            }
        }
    }

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if (stopTicks > 0) {
            stopTicks--;
            return;
        }
        if(mc.thePlayer.isSneaking())
            return;

        if(MovementUtils.isMoving())
            mc.thePlayer.setSprinting(true);

        final SpeedMode speedMode = getMode();

        if(speedMode != null)
            speedMode.onUpdate();
    }

    @EventTarget
    public void onMotion(final MotionEvent event) {
        if(mc.thePlayer.isSneaking() || event.getEventState() != EventState.PRE)
            return;

        if(MovementUtils.isMoving())
            mc.thePlayer.setSprinting(true);

        final SpeedMode speedMode = getMode();

        if(speedMode != null)
            speedMode.onMotion(event);
    }

    @EventTarget
    public void onMove(MoveEvent event) {
        /*
        if(stoptick > 0) {
            mc.thePlayer.jumpMovementFactor = 0;
            mc.thePlayer.horseJumpPower = 0;
            mc.thePlayer.onGround = true;
        }
        if (mc.thePlayer.isSneaking() || stoptick > 0 || LiquidBounce.moduleManager.get(Scaffold.class).getState())
            return;

         */

        if (this.stopTicks > 0)
            return;

        final SpeedMode speedMode = getMode();

        if (speedMode != null)
            speedMode.onMove(event);
    }

    @EventTarget
    public void onTick(final TickEvent event) {
        if (this.stopTicks > 0)
            return;
        if(mc.thePlayer.isSneaking())
            return;

        final SpeedMode speedMode = getMode();

        if(speedMode != null)
            speedMode.onTick();
    }

    @Override
    public void onEnable() {
        if(mc.thePlayer == null)
            return;

        mc.timer.timerSpeed = 1F;

        Stair sp = (Stair) LiquidBounce.moduleManager.getModule("Stair");
        HUD hd = (HUD)LiquidBounce.moduleManager.getModule("HUD");

        if (stairDebugValue.get()) {
            if (sp.getState()) {
                yn = true;
                LiquidBounce.moduleManager.getModule("Stair").setState(false);
                if (hd.getNotifyForAllModule().get()) {
                    if (hd.getSoundNotifyForAllModule().get()) {
                        new SoundFxPlayer().playSound(SoundFxPlayer.SoundType.VICTORY, -8f);
                    }
                    NotificationManager.addNotification("Debug Disable Stair", Notification.Type.INFO);

                }
            }
        }

        final SpeedMode speedMode = getMode();

        if(speedMode != null)
            speedMode.onEnable();
    }

    @Override
    public void onDisable() {
        HUD hd = (HUD)LiquidBounce.moduleManager.getModule("HUD");
        if(mc.thePlayer == null)
            return;

        mc.timer.timerSpeed = 1F;

        if (stairDebugValue.get()) {
            if (yn) {
                LiquidBounce.moduleManager.getModule("Stair").setState(true);
                if (hd.getNotifyForAllModule().get()) {
                    if (hd.getSoundNotifyForAllModule().get()) {
                        new SoundFxPlayer().playSound(SoundFxPlayer.SoundType.VICTORY, -8f);
                    }
                    NotificationManager.addNotification("Debug Enable Stair", Notification.Type.INFO);
                }
                yn = false;
            }
        }

        final SpeedMode speedMode = getMode();

        if(speedMode != null)
            speedMode.onDisable();
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }

    private SpeedMode getMode() {
        final String mode = modeValue.get();

        for(final SpeedMode speedMode : speedModes)
            if(speedMode.modeName.equalsIgnoreCase(mode))
                return speedMode;

        return null;
    }

    private String[] getModes() {
        final List<String> list = new ArrayList<>();
        for(final SpeedMode speedMode : speedModes)
            list.add(speedMode.modeName);
        return list.toArray(new String[0]);
    }
}
