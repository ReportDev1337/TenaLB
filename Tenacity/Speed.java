package dev.tenacity.module.impl.movement;

import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.game.WorldEvent;
import dev.tenacity.event.impl.network.PacketReceiveEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.player.MoveEvent;
import dev.tenacity.event.impl.player.PlayerMoveUpdateEvent;
import dev.tenacity.event.impl.player.UpdateEvent;
import dev.tenacity.event.impl.render.Render2DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.combat.KillAura;
import dev.tenacity.module.impl.combat.TargetStrafe;
import dev.tenacity.module.impl.render.HUDMod;
import dev.tenacity.module.settings.ParentAttribute;
import dev.tenacity.module.settings.Setting;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.ui.notifications.NotificationManager;
import dev.tenacity.ui.notifications.NotificationType;
import dev.tenacity.utils.player.MovementUtils;
import dev.tenacity.utils.player.ScaffoldUtils;
import dev.tenacity.utils.render.RenderFrameCache;
import dev.tenacity.utils.server.PacketUtils;
import dev.tenacity.utils.time.TimerUtil;
import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

public final class Speed extends Module {
   private final ModeSetting mode = new ModeSetting("Mode", "Watchdog", new String[]{"Watchdog", "Strafe", "Matrix", "HurtTime", "Legit", "Vanilla", "BHop", "Verus", "Viper", "Vulcan", "Zonecraft", "Heatseeker", "Mineland", "Miniblox", "Miniblox2", "MinibloxTesting", "NCP", "Mospixel", "Custom"});
   private final ModeSetting ncpMode = new ModeSetting("NCP Mode", "Hop", new String[]{"Hop", "BHop", "SNCPBHop", "FHop", "YPort", "OnGround", "Frame", "MiniJump", "SemiStrafe", "Boost"});
   private final BooleanSetting ncpFrameSkipSlowJump = new BooleanSetting("Skip Slow Jump", false);
   private final ModeSetting mospixelMode = new ModeSetting("Mospixel Mode", "Hop", new String[]{"Hop", "LowHop", "Low", "YPort", "OnGround"});
   private final BooleanSetting mospixelStopInAir = new BooleanSetting("Stop In Air", true);
   private final NumberSetting yportBoost = new NumberSetting("YPort Boost", 2.15, (double)7.5F, 0.1, 0.01);
   private final NumberSetting yportFriction = new NumberSetting("YPort Friction", 0.66, (double)3.0F, 0.1, 0.01);
   private final ModeSetting watchdogMode = new ModeSetting("Watchdog Mode", "Hop", new String[]{"Hop", "Dev", "Low Hop", "Ground"});
   private final ModeSetting verusMode = new ModeSetting("Verus Mode", "Normal", new String[]{"Low", "Normal"});
   private final ModeSetting minibloxTestingMode = new ModeSetting("MinibloxTesting Mode", "Low", new String[]{"Low", "Hop"});
   private final NumberSetting minibloxTestingLowSpeed = new NumberSetting("Low Speed", 0.33, (double)1.0F, 0.1, 0.01);
   private final BooleanSetting minibloxTestingSlowStart = new BooleanSetting("Slow Start", false);
   private final NumberSetting minibloxTestingSlowStartTime = new NumberSetting("Slow Start Time", (double)1.0F, (double)5.0F, 0.1, 0.1);
   private final ModeSetting viperMode = new ModeSetting("Viper Mode", "Normal", new String[]{"High", "Normal"});
   private final BooleanSetting legit45 = new BooleanSetting("45 Rotation", false);
   private final ModeSetting legit45Direction = new ModeSetting("45 Direction", "Left", new String[]{"Left", "Right"});
   private final BooleanSetting allowJumping = new BooleanSetting("Allow Jumping", false);
   private final NumberSetting groundSpeed = new NumberSetting("Ground Speed", (double)2.0F, (double)5.0F, (double)1.0F, 0.1);
   private final NumberSetting timer = new NumberSetting("Timer", (double)1.0F, (double)5.0F, (double)1.0F, 0.1);
   private final NumberSetting vanillaSpeed = new NumberSetting("Speed", (double)1.0F, (double)10.0F, (double)1.0F, 0.1);
   private final NumberSetting minibloxExpand = new NumberSetting("Miniblox Expand", (double)2.0F, (double)6.0F, (double)1.0F, (double)0.5F);
   private final BooleanSetting autoDisable = new BooleanSetting("Auto Disable", true);
   private final ModeSetting flagBehaviour = new ModeSetting("Flag Behaviour", "Disable", new String[]{"Disable", "Cooldown"});
   private final NumberSetting cooldownSetting = new NumberSetting("Cooldown", (double)1.0F, (double)10.0F, 0.1, 0.1);
   private final BooleanSetting spoofY = new BooleanSetting("Spoof Y", false);
   private final ModeSetting spoofYMode = new ModeSetting("Spoof Y Mode", "Flat", new String[]{"Flat", "LowHop"});
   private boolean cooldownActive;
   private long cooldownStart;
   private double y;
   private double spoofGroundY;
   private double spoofMotionY;
   private int spoofAirTicks;
   private final TimerUtil timerUtil = new TimerUtil();
   private final float r = ThreadLocalRandom.current().nextFloat();
   private double speed;
   private double lastDist;
   private float speedChangingDirection;
   private int stage;
   private boolean strafe;
   private boolean wasOnGround;
   private long minibloxTestingSlowStartAt;
   private boolean minibloxTestingWasMoving;
   private int minibloxTestingAirTicks;
   private boolean setTimer = true;
   private double moveSpeed;
   private int inAirTicks;
   private int minibloxPrevSlot = -1;
   private int ncpLevel = 1;
   private double ncpMoveSpeed = 0.2873;
   private double ncpLastDist;
   private int ncpTimerDelay;
   private int ncpJumps;
   private int ncpMotionTicks;
   private boolean ncpFrameMove;
   private int ncpMotionDelay;
   private float ncpGround;
   private int mosStage = 0;
   private double mosSpeed = 0.2873;
   private double mosLastDist = (double)0.0F;
   private int mosAirTicks = 0;
   private int yportStage = 0;
   private double yportSpeed = (double)0.0F;
   private double yportLastDist = (double)0.0F;
   private final BlockPos.MutableBlockPos reusableBlockPos = new BlockPos.MutableBlockPos();
   private final MultipleBoolSetting customCategories = new MultipleBoolSetting("Custom Categories", new BooleanSetting[]{new BooleanSetting("Ground Control", false), new BooleanSetting("Air XZ Control", false), new BooleanSetting("Air Y Control", false), new BooleanSetting("Timer Control", false), new BooleanSetting("Friction Control", false), new BooleanSetting("Conditional Modifiers", false), new BooleanSetting("Directional Control", false), new BooleanSetting("Automation", false)});
   private final NumberSetting groundSpeedMult = new NumberSetting("Speed Multiplier", (double)1.0F, (double)5.0F, (double)-3.0F, 0.1);
   private final NumberSetting jumpHeight = new NumberSetting("Jump Height", (double)0.0F, (double)1.0F, 0.42, 0.01);
   private final NumberSetting jumpDelay = new NumberSetting("Jump Delay", (double)0.0F, (double)10.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting sprintBoost = new NumberSetting("Sprint Boost", (double)0.0F, (double)2.0F, (double)0.0F, 0.1);
   private final NumberSetting sneakModifier = new NumberSetting("Sneak Modifier", (double)0.0F, (double)1.0F, (double)1.0F, 0.1);
   private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", false);
   private final BooleanSetting jumpOnlyMoving = new BooleanSetting("Jump Only Moving", true);
   private final BooleanSetting groundOnly = new BooleanSetting("Ground Only", false);
   private final ModeSetting airXZMode = new ModeSetting("XZ Mode", "Up to", new String[]{"Up to", "From", "During"});
   private final NumberSetting airXZTickStart = new NumberSetting("XZ Tick Start", (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting airXZTickEnd = new NumberSetting("XZ Tick End", (double)0.0F, (double)20.0F, (double)10.0F, (double)1.0F);
   private final NumberSetting airXZMotionMultiplier = new NumberSetting("XZ Motion Multiplier", (double)1.0F, (double)3.0F, (double)-3.0F, 0.1);
   private final ModeSetting airYMode = new ModeSetting("Y Mode", "Up to", new String[]{"Up to", "From", "During"});
   private final NumberSetting airYTickStart = new NumberSetting("Y Tick Start", (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting airYTickEnd = new NumberSetting("Y Tick End", (double)0.0F, (double)20.0F, (double)10.0F, (double)1.0F);
   private final NumberSetting airYMotion = new NumberSetting("Y Motion", (double)-2.0F, (double)2.0F, (double)0.0F, 0.01);
   private final ModeSetting airYMotionMode = new ModeSetting("Y Motion Mode", "Set", new String[]{"Set", "Add", "Multiply"});
   private final NumberSetting timerValue1 = new NumberSetting("Timer Value 1", 0.1, (double)5.0F, (double)1.0F, 0.1);
   private final NumberSetting timerDelay1 = new NumberSetting("Timer Delay 1", (double)0.0F, (double)100.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting timerValue2 = new NumberSetting("Timer Value 2", 0.1, (double)5.0F, (double)1.0F, 0.1);
   private final NumberSetting timerDelay2 = new NumberSetting("Timer Delay 2", (double)0.0F, (double)100.0F, (double)50.0F, (double)1.0F);
   private final ModeSetting timerMode = new ModeSetting("Timer Mode", "Static", new String[]{"Static", "Alternating"});
   private final NumberSetting groundFriction = new NumberSetting("Ground Friction", (double)0.0F, (double)1.0F, 0.91, 0.01);
   private final NumberSetting airFriction = new NumberSetting("Air Friction", (double)0.0F, (double)1.0F, 0.98, 0.01);
   private final NumberSetting waterFriction = new NumberSetting("Water Friction", (double)0.0F, (double)1.0F, 0.8, 0.01);
   private final NumberSetting lavaFriction = new NumberSetting("Lava Friction", (double)0.0F, (double)1.0F, 0.54, 0.01);
   private final NumberSetting hurtSpeedMult = new NumberSetting("Hurt Speed Mult", (double)0.0F, (double)3.0F, (double)1.0F, 0.1);
   private final NumberSetting potionSpeedMult = new NumberSetting("Potion Speed Mult", (double)0.0F, (double)3.0F, (double)1.0F, 0.1);
   private final NumberSetting underwaterSpeedMult = new NumberSetting("Underwater Speed Mult", (double)0.0F, (double)3.0F, (double)1.0F, 0.1);
   private final NumberSetting climbingSpeedMult = new NumberSetting("Climbing Speed Mult", (double)0.0F, (double)3.0F, (double)1.0F, 0.1);
   private final NumberSetting elytraSpeedMult = new NumberSetting("Elytra Speed Mult", (double)0.0F, (double)3.0F, (double)1.0F, 0.1);
   private final NumberSetting forwardMultiplier = new NumberSetting("Forward Mult", (double)0.0F, (double)3.0F, (double)1.0F, 0.1);
   private final NumberSetting backwardMultiplier = new NumberSetting("Backward Mult", (double)0.0F, (double)3.0F, (double)1.0F, 0.1);
   private final NumberSetting strafeMultiplier = new NumberSetting("Strafe Mult", (double)0.0F, (double)3.0F, (double)1.0F, 0.1);
   private final NumberSetting diagonalMultiplier = new NumberSetting("Diagonal Mult", (double)0.0F, (double)3.0F, (double)1.0F, 0.1);
   private final NumberSetting yawOffset = new NumberSetting("Yaw Offset", (double)-180.0F, (double)180.0F, (double)0.0F, (double)1.0F);
   private final BooleanSetting autoSprint = new BooleanSetting("Auto Sprint", false);
   private final BooleanSetting autoSneak = new BooleanSetting("Auto Sneak", false);
   private final BooleanSetting autoJumpWhenSpeed = new BooleanSetting("Auto Jump When Speed", false);
   private final NumberSetting autoJumpThreshold = new NumberSetting("Auto Jump Threshold", (double)0.0F, (double)2.0F, (double)0.5F, 0.1);
   private int customInAirTicks = 0;
   private int customJumpDelayCounter = 0;
   private int customTimerTickCounter = 0;
   private boolean customUseTimer1 = true;

   public Speed() {
      super("Speed", Category.MOVEMENT, "Makes you go faster");
      this.watchdogMode.addParent(this.mode, (modeSetting) -> modeSetting.is("Watchdog"));
      this.ncpMode.addParent(this.mode, (modeSetting) -> modeSetting.is("NCP"));
      this.ncpFrameSkipSlowJump.addParent(this.ncpMode, (m) -> m.is("Frame") && this.mode.is("NCP"));
      this.mospixelMode.addParent(this.mode, (modeSetting) -> modeSetting.is("Mospixel"));
      this.mospixelStopInAir.addParent(this.mospixelMode, (m) -> m.is("OnGround"));
      this.yportBoost.addParent(this.mospixelMode, (m) -> m.is("YPort") && this.mode.is("Mospixel"));
      this.yportFriction.addParent(this.mospixelMode, (m) -> m.is("YPort") && this.mode.is("Mospixel"));
      this.verusMode.addParent(this.mode, (modeSetting) -> modeSetting.is("Verus"));
      this.minibloxTestingMode.addParent(this.mode, (modeSetting) -> modeSetting.is("MinibloxTesting"));
      this.minibloxTestingLowSpeed.addParent(this.minibloxTestingMode, (modeSetting) -> modeSetting.is("Low") && this.mode.is("MinibloxTesting"));
      this.minibloxTestingSlowStart.addParent(this.minibloxTestingMode, (modeSetting) -> modeSetting.is("Low") && this.mode.is("MinibloxTesting"));
      this.minibloxTestingSlowStartTime.addParent(this.minibloxTestingSlowStart, BooleanSetting::isEnabled);
      this.viperMode.addParent(this.mode, (modeSetting) -> modeSetting.is("Viper"));
      this.groundSpeed.addParent(this.watchdogMode, (modeSetting) -> modeSetting.is("Ground") && this.mode.is("Watchdog"));
      this.vanillaSpeed.addParent(this.mode, (modeSetting) -> modeSetting.is("Vanilla") || modeSetting.is("BHop"));
      this.allowJumping.addParent(this.mode, (modeSetting) -> modeSetting.is("Vanilla"));
      this.legit45.addParent(this.mode, (m) -> m.is("Legit"));
      this.legit45Direction.addParent(this.legit45, ParentAttribute.BOOLEAN_CONDITION);
      this.minibloxExpand.addParent(this.mode, (modeSetting) -> modeSetting.is("Miniblox"));
      this.flagBehaviour.addParent(this.autoDisable, (booleanSetting) -> booleanSetting.isEnabled());
      this.cooldownSetting.addParent(this.flagBehaviour, (modeSetting) -> modeSetting.is("Cooldown"));
      this.spoofYMode.addParent(this.spoofY, ParentAttribute.BOOLEAN_CONDITION);
      this.customCategories.addParent(this.mode, (m) -> m.is("Custom"));
      this.groundSpeedMult.addParent(this.customCategories, (m) -> m.getSetting("Ground Control").isEnabled());
      this.jumpHeight.addParent(this.customCategories, (m) -> m.getSetting("Ground Control").isEnabled());
      this.jumpDelay.addParent(this.customCategories, (m) -> m.getSetting("Ground Control").isEnabled());
      this.sprintBoost.addParent(this.customCategories, (m) -> m.getSetting("Ground Control").isEnabled());
      this.sneakModifier.addParent(this.customCategories, (m) -> m.getSetting("Ground Control").isEnabled());
      this.autoJump.addParent(this.customCategories, (m) -> m.getSetting("Ground Control").isEnabled());
      this.jumpOnlyMoving.addParent(this.customCategories, (m) -> m.getSetting("Ground Control").isEnabled());
      this.groundOnly.addParent(this.customCategories, (m) -> m.getSetting("Ground Control").isEnabled());
      this.airXZMode.addParent(this.customCategories, (m) -> m.getSetting("Air XZ Control").isEnabled());
      this.airXZTickStart.addParent(this.customCategories, (m) -> m.getSetting("Air XZ Control").isEnabled());
      this.airXZTickEnd.addParent(this.airXZMode, (m) -> m.is("During") && this.customCategories.getSetting("Air XZ Control").isEnabled());
      this.airXZMotionMultiplier.addParent(this.customCategories, (m) -> m.getSetting("Air XZ Control").isEnabled());
      this.airYMode.addParent(this.customCategories, (m) -> m.getSetting("Air Y Control").isEnabled());
      this.airYTickStart.addParent(this.customCategories, (m) -> m.getSetting("Air Y Control").isEnabled());
      this.airYTickEnd.addParent(this.airYMode, (m) -> m.is("During") && this.customCategories.getSetting("Air Y Control").isEnabled());
      this.airYMotion.addParent(this.customCategories, (m) -> m.getSetting("Air Y Control").isEnabled());
      this.airYMotionMode.addParent(this.customCategories, (m) -> m.getSetting("Air Y Control").isEnabled());
      this.timerValue1.addParent(this.customCategories, (m) -> m.getSetting("Timer Control").isEnabled());
      this.timerDelay1.addParent(this.customCategories, (m) -> m.getSetting("Timer Control").isEnabled());
      this.timerValue2.addParent(this.timerMode, (m) -> m.is("Alternating") && this.customCategories.getSetting("Timer Control").isEnabled());
      this.timerDelay2.addParent(this.timerMode, (m) -> m.is("Alternating") && this.customCategories.getSetting("Timer Control").isEnabled());
      this.timerMode.addParent(this.customCategories, (m) -> m.getSetting("Timer Control").isEnabled());
      this.groundFriction.addParent(this.customCategories, (m) -> m.getSetting("Friction Control").isEnabled());
      this.airFriction.addParent(this.customCategories, (m) -> m.getSetting("Friction Control").isEnabled());
      this.waterFriction.addParent(this.customCategories, (m) -> m.getSetting("Friction Control").isEnabled());
      this.lavaFriction.addParent(this.customCategories, (m) -> m.getSetting("Friction Control").isEnabled());
      this.hurtSpeedMult.addParent(this.customCategories, (m) -> m.getSetting("Conditional Modifiers").isEnabled());
      this.potionSpeedMult.addParent(this.customCategories, (m) -> m.getSetting("Conditional Modifiers").isEnabled());
      this.underwaterSpeedMult.addParent(this.customCategories, (m) -> m.getSetting("Conditional Modifiers").isEnabled());
      this.climbingSpeedMult.addParent(this.customCategories, (m) -> m.getSetting("Conditional Modifiers").isEnabled());
      this.elytraSpeedMult.addParent(this.customCategories, (m) -> m.getSetting("Conditional Modifiers").isEnabled());
      this.forwardMultiplier.addParent(this.customCategories, (m) -> m.getSetting("Directional Control").isEnabled());
      this.backwardMultiplier.addParent(this.customCategories, (m) -> m.getSetting("Directional Control").isEnabled());
      this.strafeMultiplier.addParent(this.customCategories, (m) -> m.getSetting("Directional Control").isEnabled());
      this.diagonalMultiplier.addParent(this.customCategories, (m) -> m.getSetting("Directional Control").isEnabled());
      this.yawOffset.addParent(this.customCategories, (m) -> m.getSetting("Directional Control").isEnabled());
      this.autoSprint.addParent(this.customCategories, (m) -> m.getSetting("Automation").isEnabled());
      this.autoSneak.addParent(this.customCategories, (m) -> m.getSetting("Automation").isEnabled());
      this.autoJumpWhenSpeed.addParent(this.customCategories, (m) -> m.getSetting("Automation").isEnabled());
      this.autoJumpThreshold.addParent(this.customCategories, (m) -> m.getSetting("Automation").isEnabled());
      this.addSettings(new Setting[]{this.mode, this.vanillaSpeed, this.allowJumping, this.legit45, this.legit45Direction, this.watchdogMode, this.ncpMode, this.ncpFrameSkipSlowJump, this.mospixelMode, this.mospixelStopInAir, this.yportBoost, this.yportFriction, this.verusMode, this.minibloxTestingMode, this.minibloxTestingLowSpeed, this.minibloxTestingSlowStart, this.minibloxTestingSlowStartTime, this.viperMode, this.autoDisable, this.flagBehaviour, this.cooldownSetting, this.groundSpeed, this.timer, this.minibloxExpand, this.spoofY, this.spoofYMode, this.customCategories, this.groundSpeedMult, this.jumpHeight, this.jumpDelay, this.sprintBoost, this.sneakModifier, this.autoJump, this.jumpOnlyMoving, this.groundOnly, this.airXZMode, this.airXZTickStart, this.airXZTickEnd, this.airXZMotionMultiplier, this.airYMode, this.airYTickStart, this.airYTickEnd, this.airYMotion, this.airYMotionMode, this.timerValue1, this.timerDelay1, this.timerValue2, this.timerDelay2, this.timerMode, this.groundFriction, this.airFriction, this.waterFriction, this.lavaFriction, this.hurtSpeedMult, this.potionSpeedMult, this.underwaterSpeedMult, this.climbingSpeedMult, this.elytraSpeedMult, this.forwardMultiplier, this.backwardMultiplier, this.strafeMultiplier, this.diagonalMultiplier, this.yawOffset, this.autoSprint, this.autoSneak, this.autoJumpWhenSpeed, this.autoJumpThreshold});
   }

   public void onUpdateEvent(UpdateEvent event) {
      if (this.mode.is("Legit") && !this.cooldownActive && MovementUtils.isMoving() && mc.thePlayer.onGround && mc.thePlayer.motionY < 0.003) {
         mc.thePlayer.jump();
      }

   }

   public void onMotionEvent(MotionEvent e) {
      this.setSuffix(this.mode.getMode());
      if (!this.cooldownActive) {
         if (this.spoofY.isEnabled()) {
            if (this.spoofYMode.is("Flat")) {
               if (mc.thePlayer.onGround) {
                  this.y = mc.thePlayer.posY;
               }

               mc.thePlayer.posY = this.y;
            } else if (this.spoofYMode.is("LowHop")) {
               if (mc.thePlayer.onGround) {
                  this.spoofGroundY = mc.thePlayer.posY;
                  this.y = this.spoofGroundY;
               }

               if (!mc.thePlayer.onGround && this.spoofAirTicks == 0) {
                  this.spoofMotionY = 0.42;
                  this.spoofAirTicks = 1;
               } else if (this.spoofAirTicks > 0) {
                  if (this.spoofAirTicks >= 7) {
                     this.spoofMotionY = Math.max(this.spoofMotionY, -0.115);
                  }

                  this.y += this.spoofMotionY;
                  if (this.spoofAirTicks >= 7) {
                     this.spoofMotionY = Math.max((this.spoofMotionY - 0.08) * 0.98, -0.115);
                  } else {
                     this.spoofMotionY = (this.spoofMotionY - 0.08) * 0.98;
                  }

                  ++this.spoofAirTicks;
                  if (this.y <= this.spoofGroundY) {
                     this.y = this.spoofGroundY;
                     this.spoofMotionY = 0.42;
                     this.spoofAirTicks = 1;
                  }
               }

               mc.thePlayer.posY = this.y;
            }
         }

         if (this.setTimer) {
            mc.timer.timerSpeed = this.timer.getValue().floatValue();
         }

         double distX = e.getX() - mc.thePlayer.prevPosX;
         double distZ = e.getZ() - mc.thePlayer.prevPosZ;
         this.lastDist = Math.hypot(distX, distZ);
         switch (this.mode.getMode()) {
            case "Watchdog":
               switch (this.watchdogMode.getMode()) {
                  case "Hop":
                  case "Low Hop":
                  case "Dev":
                     if (e.isPre() && MovementUtils.isMoving() && mc.thePlayer.fallDistance < 1.0F && mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                     }

                     return;
                  default:
                     return;
               }
            case "Heatseeker":
               if (e.isPre() && mc.thePlayer.onGround) {
                  if (this.timerUtil.hasTimeElapsed(300L, true)) {
                     this.strafe = !this.strafe;
                  }

                  if (this.strafe) {
                     MovementUtils.setSpeed((double)1.5F);
                  }
               }
               break;
            case "Mineland":
               if (e.isPre()) {
                  ++this.stage;
                  if (this.stage == 1) {
                     mc.thePlayer.motionY = 0.2;
                  }

                  if (mc.thePlayer.onGround && this.stage > 1) {
                     MovementUtils.setSpeed((double)0.5F);
                  }

                  if (this.stage % 14 == 0) {
                     this.stage = 0;
                  }
               }
               break;
            case "Vulcan":
               if (e.isPre()) {
                  if (mc.thePlayer.onGround) {
                     if (MovementUtils.isMoving()) {
                        mc.thePlayer.jump();
                        MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * 1.6);
                        this.inAirTicks = 0;
                     }
                  } else {
                     ++this.inAirTicks;
                     if (this.inAirTicks == 1) {
                        MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * 1.16);
                     }
                  }
               }
               break;
            case "Zonecraft":
               if (e.isPre()) {
                  if (mc.thePlayer.onGround) {
                     mc.thePlayer.jump();
                     MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * 1.8);
                     this.stage = 0;
                  } else {
                     if (this.stage == 0 && !mc.thePlayer.isCollidedHorizontally) {
                        mc.thePlayer.motionY = -0.4;
                     }

                     ++this.stage;
                  }
               }
               break;
            case "Matrix":
               if (MovementUtils.isMoving()) {
                  if (mc.thePlayer.onGround && mc.thePlayer.motionY < 0.003) {
                     mc.thePlayer.jump();
                     mc.timer.timerSpeed = 1.0F;
                  }

                  if (mc.thePlayer.motionY > 0.003) {
                     EntityPlayerSP var80 = mc.thePlayer;
                     var80.motionX *= this.speed;
                     var80 = mc.thePlayer;
                     var80.motionZ *= this.speed;
                     mc.timer.timerSpeed = 1.05F;
                  }

                  this.speed = (double)1.0012F;
               }
               break;
            case "HurtTime":
               if (MovementUtils.isMoving()) {
                  if (mc.thePlayer.hurtTime <= 0) {
                     EntityPlayerSP var76 = mc.thePlayer;
                     var76.motionX *= (double)1.001F;
                     var76 = mc.thePlayer;
                     var76.motionZ *= (double)1.001F;
                  } else {
                     EntityPlayerSP var78 = mc.thePlayer;
                     var78.motionX *= (double)1.0294F;
                     var78 = mc.thePlayer;
                     var78.motionZ *= (double)1.0294F;
                  }

                  if (mc.thePlayer.onGround && mc.thePlayer.motionY < 0.003) {
                     mc.thePlayer.jump();
                  }
               }
               break;
            case "Legit":
               if (this.legit45.isEnabled() && e.isPre() && MovementUtils.isMoving() && KillAura.target == null && !Tenacity.INSTANCE.isEnabled(Scaffold.class)) {
                  float forward = mc.thePlayer.movementInput.moveForward;
                  float strafe = mc.thePlayer.movementInput.moveStrafe;
                  float yaw = mc.thePlayer.rotationYaw;
                  float desiredAngle = yaw - (float)Math.toDegrees(Math.atan2((double)strafe, (double)forward));
                  if (!mc.thePlayer.onGround) {
                     desiredAngle += this.legit45Direction.is("Left") ? -45.0F : 45.0F;
                  }

                  mc.thePlayer.rotationYaw = desiredAngle;
               }
               break;
            case "Vanilla":
               if (MovementUtils.isMoving()) {
                  MovementUtils.setSpeed(this.vanillaSpeed.getValue() / (double)4.0F);
               }
               break;
            case "BHop":
               if (MovementUtils.isMoving()) {
                  MovementUtils.setSpeed(this.vanillaSpeed.getValue() / (double)4.0F);
                  if (mc.thePlayer.onGround) {
                     mc.thePlayer.jump();
                  }
               }
               break;
            case "Verus":
               switch (this.verusMode.getMode()) {
                  case "Low":
                     if (e.isPre()) {
                        if (MovementUtils.isMoving()) {
                           if (mc.thePlayer.onGround) {
                              mc.thePlayer.jump();
                              this.wasOnGround = true;
                           } else if (this.wasOnGround) {
                              if (!mc.thePlayer.isCollidedHorizontally) {
                                 mc.thePlayer.motionY = -0.11584000015258789;
                              }

                              this.wasOnGround = false;
                           }

                           MovementUtils.setSpeed(0.33);
                        } else {
                           mc.thePlayer.motionX = mc.thePlayer.motionZ = (double)0.0F;
                        }

                        return;
                     }

                     return;
                  case "Normal":
                     if (e.isPre()) {
                        if (MovementUtils.isMoving()) {
                           if (mc.thePlayer.onGround) {
                              mc.thePlayer.jump();
                              MovementUtils.setSpeed(0.48);
                           } else {
                              MovementUtils.setSpeed((double)MovementUtils.getSpeed());
                           }

                           return;
                        } else {
                           MovementUtils.setSpeed((double)0.0F);
                           return;
                        }
                     }

                     return;
                  default:
                     return;
               }
            case "MinibloxTesting":
               switch (this.minibloxTestingMode.getMode()) {
                  case "Low":
                     if (e.isPre()) {
                        if (MovementUtils.isMoving()) {
                           if (mc.thePlayer.onGround) {
                              mc.thePlayer.jump();
                              this.wasOnGround = true;
                           } else if (this.wasOnGround) {
                              if (!mc.thePlayer.isCollidedHorizontally) {
                                 mc.thePlayer.motionY = -0.11584000015258789;
                              }

                              this.wasOnGround = false;
                           }

                           double speed = this.minibloxTestingLowSpeed.getValue();
                           if (this.minibloxTestingSlowStart.isEnabled()) {
                              if (!this.minibloxTestingWasMoving) {
                                 this.minibloxTestingSlowStartAt = System.currentTimeMillis();
                              }

                              double progress = Math.min((double)1.0F, (double)(System.currentTimeMillis() - this.minibloxTestingSlowStartAt) / (this.minibloxTestingSlowStartTime.getValue() * (double)1000.0F));
                              double startSpeed = this.minibloxTestingLowSpeed.getMinValue();
                              speed = startSpeed + (speed - startSpeed) * progress;
                           }

                           this.minibloxTestingWasMoving = true;
                           MovementUtils.setSpeed(speed);
                        } else {
                           mc.thePlayer.motionX = mc.thePlayer.motionZ = (double)0.0F;
                           this.minibloxTestingWasMoving = false;
                        }

                        return;
                     }

                     return;
                  case "Hop":
                     if (e.isPre() && MovementUtils.isMoving() && mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                     }

                     return;
                  default:
                     return;
               }
            case "Viper":
               switch (this.viperMode.getMode()) {
                  case "High":
                     if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.7;
                     }
                     break;
                  case "Normal":
                     if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.42;
                     }
               }

               MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * 1.2);
               break;
            case "Strafe":
               if (e.isPre() && MovementUtils.isMoving()) {
                  if (mc.thePlayer.onGround) {
                     mc.thePlayer.jump();
                  } else {
                     MovementUtils.setSpeed((double)MovementUtils.getSpeed());
                  }
               }
               break;
            case "Miniblox":
               if (e.isPre() && MovementUtils.isMoving()) {
                  if (mc.thePlayer.onGround) {
                     mc.thePlayer.jump();
                  }

                  this.minibloxPlaceBlock();
               }
            case "Miniblox2":
            default:
               break;
            case "NCP":
               switch (this.ncpMode.getMode()) {
                  case "Hop":
                     if (e.isPre() && MovementUtils.isMoving()) {
                        if (mc.thePlayer.onGround) {
                           mc.thePlayer.jump();
                           mc.thePlayer.jumpMovementFactor = 0.0223F;
                        }

                        MovementUtils.setSpeed((double)MovementUtils.getSpeed());
                     }

                     return;
                  case "FHop":
                     if (e.isPre() && MovementUtils.isMoving()) {
                        if (mc.thePlayer.onGround) {
                           mc.thePlayer.jump();
                           EntityPlayerSP var73 = mc.thePlayer;
                           var73.motionX *= 1.01;
                           var73 = mc.thePlayer;
                           var73.motionZ *= 1.01;
                           mc.thePlayer.jumpMovementFactor = 0.0223F;
                        }

                        EntityPlayerSP var75 = mc.thePlayer;
                        var75.motionY -= 9.9999E-4;
                        MovementUtils.setSpeed((double)MovementUtils.getSpeed());
                     }

                     return;
                  case "YPort":
                     if (e.isPre() && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && !mc.thePlayer.isInWeb && MovementUtils.isMoving()) {
                        if (this.ncpJumps >= 4 && mc.thePlayer.onGround) {
                           this.ncpJumps = 0;
                        }

                        if (mc.thePlayer.onGround) {
                           mc.thePlayer.motionY = this.ncpJumps <= 1 ? (double)0.42F : (double)0.4F;
                           float f = mc.thePlayer.rotationYaw * ((float)Math.PI / 180F);
                           EntityPlayerSP var71 = mc.thePlayer;
                           var71.motionX -= (double)(MathHelper.sin(f) * 0.2F);
                           var71 = mc.thePlayer;
                           var71.motionZ += (double)(MathHelper.cos(f) * 0.2F);
                           ++this.ncpJumps;
                        } else if (this.ncpJumps <= 1) {
                           mc.thePlayer.motionY = (double)-5.0F;
                        }

                        MovementUtils.setSpeed((double)MovementUtils.getSpeed());
                     }

                     return;
                  case "OnGround":
                     if (e.isPre() && MovementUtils.isMoving() && !((double)mc.thePlayer.fallDistance > 3.994) && !mc.thePlayer.isInWater() && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isCollidedHorizontally) {
                        EntityPlayerSP var67 = mc.thePlayer;
                        var67.posY -= (double)0.3993F;
                        mc.thePlayer.motionY = (double)-1000.0F;
                        mc.thePlayer.distanceWalkedModified = 44.0F;
                        if (mc.thePlayer.onGround) {
                           var67 = mc.thePlayer;
                           var67.posY += (double)0.3993F;
                           mc.thePlayer.motionY = (double)0.3993F;
                           mc.thePlayer.distanceWalkedOnStepModified = 44.0F;
                           var67 = mc.thePlayer;
                           var67.motionX *= (double)1.59F;
                           var67 = mc.thePlayer;
                           var67.motionZ *= (double)1.59F;
                           return;
                        }
                     }

                     return;
                  case "Frame":
                     if (e.isPre() && (mc.thePlayer.movementInput.moveForward > 0.0F || mc.thePlayer.movementInput.moveStrafe > 0.0F)) {
                        if (mc.thePlayer.onGround) {
                           mc.thePlayer.jump();
                           if (this.ncpFrameSkipSlowJump.isEnabled()) {
                              this.ncpMotionTicks = 1;
                              this.ncpFrameMove = false;
                           } else if (this.ncpMotionTicks == 1) {
                              if (this.ncpFrameMove) {
                                 mc.thePlayer.motionX = (double)0.0F;
                                 mc.thePlayer.motionZ = (double)0.0F;
                                 this.ncpFrameMove = false;
                              }

                              this.ncpMotionTicks = 0;
                           } else {
                              this.ncpMotionTicks = 1;
                           }
                        } else if (!this.ncpFrameMove && this.ncpMotionTicks == 1) {
                           EntityPlayerSP var65 = mc.thePlayer;
                           var65.motionX *= (double)4.25F;
                           var65 = mc.thePlayer;
                           var65.motionZ *= (double)4.25F;
                           this.ncpFrameMove = true;
                        }

                        if (!mc.thePlayer.onGround) {
                           MovementUtils.setSpeed((double)MovementUtils.getSpeed());
                           return;
                        }
                     }

                     return;
                  case "MiniJump":
                     if (e.isPre() && MovementUtils.isMoving()) {
                        if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
                           EntityPlayerSP var62 = mc.thePlayer;
                           var62.motionY += 0.1;
                           var62 = mc.thePlayer;
                           var62.motionX *= 1.8;
                           var62 = mc.thePlayer;
                           var62.motionZ *= 1.8;
                           double curSpeed = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
                           if (curSpeed > 0.66) {
                              mc.thePlayer.motionX = mc.thePlayer.motionX / curSpeed * 0.66;
                              mc.thePlayer.motionZ = mc.thePlayer.motionZ / curSpeed * 0.66;
                           }
                        }

                        MovementUtils.setSpeed((double)MovementUtils.getSpeed());
                     }

                     return;
                  case "SemiStrafe":
                     if (e.isPre() && MovementUtils.isMoving()) {
                        if (mc.thePlayer.onGround) {
                           mc.thePlayer.motionY = 0.41999998688698;
                        } else {
                           float strafeSpeed = mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 0.265F : 0.165F;
                           MovementUtils.setSpeed((double)strafeSpeed);
                           mc.thePlayer.jumpMovementFactor = 0.13F;
                        }

                        return;
                     }

                     return;
                  case "Boost":
                     if (e.isPre() && !mc.thePlayer.isInWater() && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isSneaking() && MovementUtils.isMoving()) {
                        if (mc.thePlayer.onGround && this.ncpGround < 1.0F) {
                           this.ncpGround += 0.2F;
                        }

                        if (!mc.thePlayer.onGround) {
                           this.ncpGround = 0.0F;
                        }

                        if (this.ncpGround == 1.0F) {
                           double offset = 4.69;
                           double spd = 3.1981;
                           if (!mc.thePlayer.isSprinting()) {
                              offset += 0.8;
                           }

                           if (mc.thePlayer.moveStrafing != 0.0F) {
                              spd -= 0.1;
                              offset += (double)0.5F;
                           }

                           ++this.ncpMotionDelay;
                           switch (this.ncpMotionDelay) {
                              case 1:
                                 EntityPlayerSP var60 = mc.thePlayer;
                                 var60.motionX *= spd;
                                 var60 = mc.thePlayer;
                                 var60.motionZ *= spd;
                                 return;
                              case 2:
                                 EntityPlayerSP var58 = mc.thePlayer;
                                 var58.motionX /= 1.458;
                                 var58 = mc.thePlayer;
                                 var58.motionZ /= 1.458;
                                 return;
                              case 3:
                              default:
                                 return;
                              case 4:
                                 mc.thePlayer.setPosition(mc.thePlayer.posX + mc.thePlayer.motionX / offset, mc.thePlayer.posY, mc.thePlayer.posZ + mc.thePlayer.motionZ / offset);
                                 this.ncpMotionDelay = 0;
                                 return;
                           }
                        }
                     }

                     return;
                  case "BHop":
                  case "SNCPBHop":
                     if (e.isPre()) {
                        double xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX;
                        double zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
                        this.ncpLastDist = Math.sqrt(xDist * xDist + zDist * zDist);
                     }

                     return;
                  default:
                     return;
               }
            case "Mospixel":
               switch (this.mospixelMode.getMode()) {
                  case "Hop":
                  case "LowHop":
                     if (e.isPre()) {
                        double xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX;
                        double zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
                        double dist = Math.sqrt(xDist * xDist + zDist * zDist);
                        this.ncpLastDist = dist;
                        this.mosLastDist = mc.thePlayer.isInWater() ? (double)0.0F : dist;
                        if (this.mospixelMode.is("LowHop")) {
                           if (mc.thePlayer.onGround) {
                              this.mosAirTicks = 0;
                           } else {
                              ++this.mosAirTicks;
                           }

                           return;
                        }
                     }

                     return;
                  case "Low":
                     if (e.isPre()) {
                        if (mc.thePlayer.onGround) {
                           this.inAirTicks = 0;
                        } else {
                           ++this.inAirTicks;
                        }

                        if (MovementUtils.isMoving()) {
                           if (mc.thePlayer.onGround) {
                              mc.thePlayer.jump();
                              double groundSpeed = this.getAllowedHorizontalDistance();
                              if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                                 MovementUtils.setSpeed((double)MovementUtils.getSpeed() * 1.1);
                              } else {
                                 MovementUtils.setSpeed(groundSpeed);
                              }
                           }

                           if (mc.thePlayer.motionY < 0.1 && mc.thePlayer.motionY > -0.21 && mc.thePlayer.motionY != (double)0.0F) {
                              EntityPlayerSP var57 = mc.thePlayer;
                              var57.motionY -= 0.05;
                           }

                           if (this.inAirTicks == 5) {
                              mc.thePlayer.motionY = (mc.thePlayer.motionY - 0.08) * 0.98;
                           }

                           MovementUtils.setSpeed((double)MovementUtils.getSpeed());
                           if (mc.thePlayer.hurtTime > 0) {
                              MovementUtils.setSpeed((double)MovementUtils.getSpeed() * 1.03);
                           }

                           return;
                        }
                     }

                     return;
                  case "YPort":
                     if (e.isPre()) {
                        if (this.yportStage <= 1) {
                           ++this.yportStage;
                           this.yportLastDist = (double)0.0F;
                        } else {
                           if (!this.isStairOrSlabNearby() && this.yportStage == 3) {
                              double jumpY = (double)0.42F;
                              if (mc.thePlayer.isPotionActive(Potion.jump)) {
                                 jumpY += (double)((float)(mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F);
                              }

                              e.setOnGround(false);
                              e.setY(e.getY() + jumpY);
                           }

                           double xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX;
                           double zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
                           this.yportLastDist = Math.sqrt(xDist * xDist + zDist * zDist);
                        }

                        return;
                     }

                     return;
                  case "OnGround":
                     if (e.isPre() && (!this.mospixelStopInAir.isEnabled() || mc.thePlayer.onGround)) {
                        mc.timer.timerSpeed = 1.085F;
                        double forward = (double)mc.thePlayer.movementInput.moveForward;
                        double strafe = (double)mc.thePlayer.movementInput.moveStrafe;
                        if ((forward != (double)0.0F || strafe != (double)0.0F) && !mc.gameSettings.keyBindJump.isKeyDown() && !mc.thePlayer.isInWater() && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isCollidedHorizontally) {
                           if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset((double)0.0F, 0.4, (double)0.0F)).isEmpty()) {
                              e.setY(mc.thePlayer.posY + (mc.thePlayer.ticksExisted % 2 != 0 ? 0.2 : (double)0.0F));
                           } else {
                              e.setY(mc.thePlayer.posY + (mc.thePlayer.ticksExisted % 2 != 0 ? 0.4198 : (double)0.0F));
                           }
                        }

                        double spd = Math.max(mc.thePlayer.ticksExisted % 2 == 0 ? 2.1 : 1.3, MovementUtils.getBaseMoveSpeed());
                        float yaw = mc.thePlayer.rotationYaw;
                        if (forward == (double)0.0F && strafe == (double)0.0F) {
                           mc.thePlayer.motionX = (double)0.0F;
                           mc.thePlayer.motionZ = (double)0.0F;
                        } else {
                           if (forward != (double)0.0F) {
                              if (strafe > (double)0.0F) {
                                 yaw += forward > (double)0.0F ? -45.0F : 45.0F;
                                 strafe = (double)0.0F;
                              } else if (strafe < (double)0.0F) {
                                 yaw += forward > (double)0.0F ? 45.0F : -45.0F;
                                 strafe = (double)0.0F;
                              }

                              forward = forward > (double)0.0F ? 0.15 : -0.115;
                           }

                           if (strafe > (double)0.0F) {
                              strafe = 0.15;
                           } else if (strafe < (double)0.0F) {
                              strafe = -0.115;
                           }

                           mc.thePlayer.motionX = forward * spd * Math.cos(Math.toRadians((double)(yaw + 90.0F))) + strafe * spd * Math.sin(Math.toRadians((double)(yaw + 90.0F)));
                           mc.thePlayer.motionZ = forward * spd * Math.sin(Math.toRadians((double)(yaw + 90.0F))) - strafe * spd * Math.cos(Math.toRadians((double)(yaw + 90.0F)));
                        }

                        return;
                     }

                     return;
                  default:
                     return;
               }
            case "Custom":
               if (e.isPre()) {
                  if (mc.thePlayer.onGround) {
                     this.customInAirTicks = 0;
                  } else {
                     ++this.customInAirTicks;
                  }

                  if (this.customCategories.getSetting("Ground Control").isEnabled()) {
                     if (mc.thePlayer.onGround) {
                        MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * this.groundSpeedMult.getValue());
                        boolean shouldJump = this.autoJump.isEnabled() && (!this.jumpOnlyMoving.isEnabled() || MovementUtils.isMoving());
                        if (shouldJump && this.customJumpDelayCounter <= 0) {
                           if (this.jumpHeight.getValue() > (double)0.0F) {
                              mc.thePlayer.motionY = this.jumpHeight.getValue();
                           } else {
                              mc.thePlayer.jump();
                           }

                           this.customJumpDelayCounter = this.jumpDelay.getValue().intValue();
                        }

                        if (mc.thePlayer.isSprinting()) {
                           MovementUtils.setSpeed((double)MovementUtils.getSpeed() + this.sprintBoost.getValue());
                        }

                        if (mc.thePlayer.isSneaking()) {
                           MovementUtils.setSpeed((double)MovementUtils.getSpeed() * this.sneakModifier.getValue());
                        }
                     } else {
                        if (this.customJumpDelayCounter > 0) {
                           --this.customJumpDelayCounter;
                        }

                        if (this.groundOnly.isEnabled()) {
                           mc.thePlayer.motionX = (double)0.0F;
                           mc.thePlayer.motionZ = (double)0.0F;
                        }
                     }
                  }

                  if (this.customCategories.getSetting("Air XZ Control").isEnabled() && !mc.thePlayer.onGround && this.isTickInRange(this.airXZMode, this.airXZTickStart, this.airXZTickEnd)) {
                     MovementUtils.setSpeed((double)MovementUtils.getSpeed() * this.airXZMotionMultiplier.getValue());
                  }

                  if (this.customCategories.getSetting("Air Y Control").isEnabled() && !mc.thePlayer.onGround && this.isTickInRange(this.airYMode, this.airYTickStart, this.airYTickEnd)) {
                     switch (this.airYMotionMode.getMode()) {
                        case "Set":
                           mc.thePlayer.motionY = this.airYMotion.getValue();
                           break;
                        case "Add":
                           EntityPlayerSP var55 = mc.thePlayer;
                           var55.motionY += this.airYMotion.getValue();
                           break;
                        case "Multiply":
                           EntityPlayerSP var10000 = mc.thePlayer;
                           var10000.motionY *= this.airYMotion.getValue();
                     }
                  }

                  if (this.customCategories.getSetting("Directional Control").isEnabled() && MovementUtils.isMoving()) {
                     float forward = mc.thePlayer.movementInput.moveForward;
                     float strafe = mc.thePlayer.movementInput.moveStrafe;
                     double currentSpeed = (double)MovementUtils.getSpeed();
                     if (this.yawOffset.getValue() != (double)0.0F) {
                        EntityPlayerSP var56 = mc.thePlayer;
                        var56.rotationYaw += this.yawOffset.getValue().floatValue();
                     }

                     if (forward > 0.0F) {
                        currentSpeed *= this.forwardMultiplier.getValue();
                     } else if (forward < 0.0F) {
                        currentSpeed *= this.backwardMultiplier.getValue();
                     }

                     if (strafe != 0.0F) {
                        if (forward != 0.0F) {
                           currentSpeed *= this.diagonalMultiplier.getValue();
                        } else {
                           currentSpeed *= this.strafeMultiplier.getValue();
                        }
                     }

                     MovementUtils.setSpeed(currentSpeed);
                  }

                  if (this.customCategories.getSetting("Conditional Modifiers").isEnabled()) {
                     double currentSpeed = (double)MovementUtils.getSpeed();
                     if (mc.thePlayer.hurtTime > 0) {
                        currentSpeed *= this.hurtSpeedMult.getValue();
                     }

                     if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                        currentSpeed *= this.potionSpeedMult.getValue();
                     }

                     if (mc.thePlayer.isInWater()) {
                        currentSpeed *= this.underwaterSpeedMult.getValue();
                     }

                     if (mc.thePlayer.isOnLadder()) {
                        currentSpeed *= this.climbingSpeedMult.getValue();
                     }

                     MovementUtils.setSpeed(currentSpeed);
                  }
               }

               if (this.customCategories.getSetting("Timer Control").isEnabled()) {
                  mc.timer.timerSpeed = (float)this.getCustomTimerValue();
               }

               if (this.customCategories.getSetting("Automation").isEnabled()) {
                  if (this.autoSprint.isEnabled() && MovementUtils.isMoving()) {
                     mc.thePlayer.setSprinting(true);
                  }

                  if (this.autoSneak.isEnabled()) {
                     mc.thePlayer.setSneaking(true);
                  }

                  if (this.autoJumpWhenSpeed.isEnabled() && mc.thePlayer.onGround) {
                     double motionMagnitude = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
                     if (motionMagnitude > this.autoJumpThreshold.getValue()) {
                        mc.thePlayer.jump();
                     }
                  }
               }
         }

      }
   }

   public void onMoveEvent(MoveEvent e) {
      if (!this.cooldownActive) {
         if (this.mode.is("Watchdog")) {
            switch (this.watchdogMode.getMode()) {
               case "Ground":
                  this.strafe = !this.strafe;
                  this.reusableBlockPos.set(MathHelper.floor_double(mc.thePlayer.posX + e.getX()), MathHelper.floor_double(mc.thePlayer.posY), MathHelper.floor_double(mc.thePlayer.posZ + e.getZ()));
                  if (mc.thePlayer.onGround && MovementUtils.isMoving() && mc.theWorld.getBlockState(this.reusableBlockPos).getBlock() == Blocks.air && !mc.thePlayer.isCollidedHorizontally && !Step.isStepping) {
                     if (this.strafe || this.groundSpeed.getValue() >= 1.6) {
                        PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX + e.getX(), mc.thePlayer.posY, mc.thePlayer.posZ + e.getZ(), true));
                     }

                     e.setSpeed(MovementUtils.getBaseMoveSpeed() * this.groundSpeed.getValue());
                  }
                  break;
               case "Low Hop":
                  if (MovementUtils.isMoving()) {
                     if (mc.thePlayer.onGround) {
                        this.inAirTicks = 0;
                     } else {
                        ++this.inAirTicks;
                     }

                     if (this.inAirTicks == 5) {
                        e.setY(mc.thePlayer.motionY = -0.19);
                     }
                  }
            }
         }

         if (this.mode.is("Mospixel") && this.mospixelMode.is("LowHop") && MovementUtils.isMoving() && this.mosAirTicks == 5) {
            e.setY(mc.thePlayer.motionY = -0.19);
         }

         if (this.mode.is("MinibloxTesting") && this.minibloxTestingMode.is("Hop") && MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) {
               this.minibloxTestingAirTicks = 0;
               e.setY(mc.thePlayer.motionY = 0.42);
            } else {
               MovementUtils.setSpeed((double)MovementUtils.getSpeed());
               ++this.minibloxTestingAirTicks;
            }

            if (this.minibloxTestingAirTicks > 4) {
               e.setY(mc.thePlayer.motionY = -0.4);
               mc.thePlayer.capabilities.isFlying = false;
               PacketUtils.sendPacketNoEvent(new C13PacketPlayerAbilities(mc.thePlayer.capabilities));
            }
         }

         if (this.mode.is("NCP") && (this.ncpMode.is("BHop") || this.ncpMode.is("SNCPBHop"))) {
            boolean sncp = this.ncpMode.is("SNCPBHop");
            if (MovementUtils.isMoving()) {
               EntityPlayerSP var10000 = mc.thePlayer;
               var10000.motionX *= (double)1.02F;
               var10000 = mc.thePlayer;
               var10000.motionZ *= (double)1.02F;
            }

            if (sncp) {
               this.ncpTimerDelay = (this.ncpTimerDelay + 1) % 5;
               if (this.ncpTimerDelay != 0) {
                  mc.timer.timerSpeed = 1.0F;
               } else if (MovementUtils.isMoving()) {
                  mc.timer.timerSpeed = 1.3F;
               }
            }

            if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
               this.ncpLevel = 2;
            }

            double posYFrac = mc.thePlayer.posY - (double)((int)mc.thePlayer.posY);
            if ((double)Math.round(posYFrac * (double)1000.0F) / (double)1000.0F == 0.138) {
               EntityPlayerSP var30 = mc.thePlayer;
               var30.motionY -= 0.08;
               e.setY(e.getY() - 0.09316090325960147);
               var30 = mc.thePlayer;
               var30.posY -= 0.09316090325960147;
            }

            double base = MovementUtils.getBaseMoveSpeed();
            if (this.ncpLevel != 1 || mc.thePlayer.moveForward == 0.0F && mc.thePlayer.moveStrafing == 0.0F) {
               if (this.ncpLevel == 2) {
                  this.ncpLevel = 3;
                  mc.thePlayer.motionY = (double)0.3994F;
                  e.setY((double)0.3994F);
                  this.ncpMoveSpeed *= 2.149;
               } else if (this.ncpLevel == 3) {
                  this.ncpLevel = 4;
                  this.ncpMoveSpeed = this.ncpLastDist - 0.66 * (this.ncpLastDist - base);
               } else if (sncp && this.ncpLevel == 88) {
                  this.ncpMoveSpeed = base;
                  this.ncpLastDist = (double)0.0F;
                  this.ncpLevel = 89;
               } else {
                  if (sncp && this.ncpLevel == 89) {
                     if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset((double)0.0F, mc.thePlayer.motionY, (double)0.0F)).isEmpty() || mc.thePlayer.isCollidedVertically) {
                        this.ncpLevel = 1;
                     }

                     this.ncpLastDist = (double)0.0F;
                     this.ncpMoveSpeed = base;
                     return;
                  }

                  if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset((double)0.0F, mc.thePlayer.motionY, (double)0.0F)).isEmpty() || mc.thePlayer.isCollidedVertically) {
                     this.ncpMoveSpeed = base;
                     this.ncpLastDist = (double)0.0F;
                     if (sncp) {
                        this.ncpLevel = 88;
                        return;
                     }

                     this.ncpLevel = 1;
                  }

                  this.ncpMoveSpeed = this.ncpLastDist - this.ncpLastDist / (double)159.0F;
               }
            } else {
               this.ncpLevel = 2;
               this.ncpMoveSpeed = 1.35 * base - 0.01;
            }

            this.ncpMoveSpeed = Math.max(this.ncpMoveSpeed, base);
            float forward = mc.thePlayer.movementInput.moveForward;
            float strafe2 = mc.thePlayer.movementInput.moveStrafe;
            float yaw = mc.thePlayer.rotationYaw;
            if (forward == 0.0F && strafe2 == 0.0F) {
               e.setX((double)0.0F);
               e.setZ((double)0.0F);
            } else {
               if (forward != 0.0F) {
                  if (strafe2 >= 1.0F) {
                     yaw += forward > 0.0F ? -45.0F : 45.0F;
                     strafe2 = 0.0F;
                  } else if (strafe2 <= -1.0F) {
                     yaw += forward > 0.0F ? 45.0F : -45.0F;
                     strafe2 = 0.0F;
                  }

                  forward = forward > 0.0F ? 1.0F : -1.0F;
               }

               double mx = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
               double mz = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
               e.setX((double)forward * this.ncpMoveSpeed * mx + (double)strafe2 * this.ncpMoveSpeed * mz);
               e.setZ((double)forward * this.ncpMoveSpeed * mz - (double)strafe2 * this.ncpMoveSpeed * mx);
            }

            mc.thePlayer.stepHeight = 0.6F;
         }

         if (this.mode.is("Mospixel") && (this.mospixelMode.is("Hop") || this.mospixelMode.is("LowHop"))) {
            double base = MovementUtils.getBaseMoveSpeed();
            boolean moving = mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
            if (!moving) {
               this.mosSpeed = base;
            }

            if (this.mosStage == 1 && mc.thePlayer.isCollidedVertically && moving) {
               this.mosSpeed = (double)0.25F + base - 0.01;
            } else if (!mc.thePlayer.isInWater() && this.mosStage == 2 && mc.thePlayer.isCollidedVertically && !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset((double)0.0F, -0.001, (double)0.0F)).isEmpty() && moving) {
               mc.thePlayer.motionY = 0.4;
               e.setY(0.4);
               mc.thePlayer.jump();
               this.mosSpeed *= 2.149;
            } else if (this.mosStage == 3) {
               double diff = 0.66 * (this.mosLastDist - base);
               this.mosSpeed = this.mosLastDist - diff;
            } else if (this.mosStage >= 4) {
               boolean colliding = !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset((double)0.0F, mc.thePlayer.motionY, (double)0.0F)).isEmpty() || mc.thePlayer.isCollidedVertically;
               if (colliding) {
                  if (1.35 * base - 0.01 > this.mosSpeed) {
                     this.mosStage = 0;
                  } else {
                     this.mosStage = moving ? 1 : 0;
                  }
               }

               this.mosSpeed = this.mosLastDist - this.mosLastDist / (double)159.0F;
            }

            this.mosSpeed = Math.max(this.mosSpeed, base);
            if (this.mosStage > 0) {
               if (mc.thePlayer.isInWater()) {
                  this.mosSpeed = 0.1;
               }

               MovementUtils.setSpeed(e, this.mosSpeed);
            }

            if (moving) {
               ++this.mosStage;
            }
         }

         if (this.mode.is("Mospixel") && this.mospixelMode.is("YPort")) {
            if (this.yportStage <= 1) {
               ++this.yportStage;
               this.yportLastDist = (double)0.0F;
               return;
            }

            if (this.isStairOrSlabNearby()) {
               return;
            }

            boolean moving = mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
            double base = 0.2873;
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
               int amp = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
               base *= (double)1.0F + 0.15 * (double)(amp + 1);
            }

            if (mc.thePlayer.onGround || this.yportStage == 3) {
               if (!mc.thePlayer.isCollidedHorizontally && moving) {
                  if (this.yportStage == 2) {
                     this.yportSpeed = base * this.yportBoost.getValue();
                     this.yportStage = 3;
                  } else if (this.yportStage == 3) {
                     this.yportStage = 2;
                     double difference = this.yportFriction.getValue() * (this.yportLastDist - base);
                     this.yportSpeed = this.yportLastDist - difference;
                  } else {
                     boolean colliding = !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset((double)0.0F, mc.thePlayer.motionY, (double)0.0F)).isEmpty() || mc.thePlayer.isCollidedVertically;
                     if (colliding) {
                        this.yportStage = 1;
                     }

                     this.yportSpeed = this.yportLastDist - this.yportLastDist / (double)159.0F;
                  }
               } else {
                  this.yportSpeed = base;
                  this.yportLastDist = (double)0.0F;
               }

               this.yportSpeed = Math.max(this.yportSpeed, base);
               float forward = mc.thePlayer.movementInput.moveForward;
               float strafe = mc.thePlayer.movementInput.moveStrafe;
               float yaw = mc.thePlayer.rotationYaw;
               if (forward == 0.0F && strafe == 0.0F) {
                  e.setX((double)0.0F);
                  e.setZ((double)0.0F);
               } else {
                  if (forward != 0.0F) {
                     if (strafe > 0.0F) {
                        yaw += forward > 0.0F ? -45.0F : 45.0F;
                        strafe = 0.0F;
                     } else if (strafe < 0.0F) {
                        yaw += forward > 0.0F ? 45.0F : -45.0F;
                        strafe = 0.0F;
                     }

                     forward = forward > 0.0F ? 1.0F : -1.0F;
                  }

                  double mx = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
                  double mz = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
                  e.setX((double)forward * this.yportSpeed * mx + (double)strafe * this.yportSpeed * mz);
                  e.setZ((double)forward * this.yportSpeed * mz - (double)strafe * this.yportSpeed * mx);
               }
            }
         }

         double eventSpeed = Math.sqrt(e.getX() * e.getX() + e.getZ() * e.getZ());
         TargetStrafe.strafe(e, eventSpeed);
      }
   }

   public void onPlayerMoveUpdateEvent(PlayerMoveUpdateEvent e) {
      if (!this.cooldownActive) {
         if (this.mode.is("Watchdog") && (this.watchdogMode.is("Hop") || this.watchdogMode.is("Dev") || this.watchdogMode.is("Low Hop")) && mc.thePlayer.fallDistance < 1.0F && !mc.thePlayer.isPotionActive(Potion.jump)) {
            if (MovementUtils.isMoving()) {
               switch (this.watchdogMode.getMode()) {
                  case "Low Hop":
                  case "Hop":
                     if (mc.thePlayer.onGround) {
                        this.speed = (double)1.5F;
                     }

                     this.speed -= 0.025;
                     e.applyMotion(MovementUtils.getBaseMoveSpeed() * this.speed, 0.55F);
                     break;
                  case "Dev":
                     if (mc.thePlayer.onGround) {
                        this.moveSpeed = MovementUtils.getBaseMoveSpeed() * 2.1475 * 0.76;
                        this.wasOnGround = true;
                     } else if (this.wasOnGround) {
                        this.moveSpeed = this.lastDist - 0.81999 * (this.lastDist - MovementUtils.getBaseMoveSpeed());
                        this.moveSpeed *= 1.0989010989010988;
                        this.wasOnGround = false;
                     } else {
                        this.moveSpeed -= TargetStrafe.canStrafe() ? this.lastDist / (double)100.0F : this.lastDist / (double)150.0F;
                     }

                     if (!mc.thePlayer.isInWater() && !mc.thePlayer.isInLava()) {
                        this.speed = Math.max(this.moveSpeed, MovementUtils.getBaseMoveSpeed());
                     } else {
                        this.speed = MovementUtils.getBaseMoveSpeed() * (double)0.25F;
                     }

                     e.applyMotion(this.speed, 0.6F);
               }
            } else {
               e.applyMotion((double)0.0F, 0.0F);
            }
         }

      }
   }

   private void minibloxPlaceBlock() {
      int slot = ScaffoldUtils.getBlockSlot();
      if (slot != -1) {
         int baseY = MathHelper.floor_double(mc.thePlayer.posY + (double)2.0F);
         BlockPos above = new BlockPos(MathHelper.floor_double(mc.thePlayer.posX), baseY, MathHelper.floor_double(mc.thePlayer.posZ));
         int prevSlot = mc.thePlayer.inventory.currentItem;
         if (slot != prevSlot) {
            PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(slot));
            this.minibloxPrevSlot = prevSlot;
         }

         ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
         Block block = ((ItemBlock)stack.getItem()).getBlock();
         this.placeAt(above, stack, block);
         if (this.minibloxPrevSlot != -1) {
            PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(this.minibloxPrevSlot));
            this.minibloxPrevSlot = -1;
         }

      }
   }

   private void placeAt(BlockPos target, ItemStack stack, Block block) {
      if (mc.theWorld.getBlockState(target).getBlock() == Blocks.air) {
         PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(target, EnumFacing.DOWN.getIndex(), stack, 0.5F, 0.5F, 0.5F));
         mc.theWorld.setBlockState(target, block.getDefaultState());
      }
   }

   private double getAllowedHorizontalDistance() {
      double dist = 0.221;
      if (!mc.thePlayer.isInWater() && !mc.thePlayer.isInLava()) {
         if (mc.thePlayer.isSneaking()) {
            return 0.0663;
         } else {
            if (Math.abs(mc.thePlayer.moveForward) >= 0.8F || Math.abs(mc.thePlayer.moveStrafing) >= 0.8F) {
               dist *= 1.3;
            }

            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
               dist *= (double)1.0F + 0.2 * (double)(mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
            }

            if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
               dist = 0.29;
            }

            this.reusableBlockPos.set(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY - (double)1.0F), MathHelper.floor_double(mc.thePlayer.posZ));
            Block below = mc.theWorld.getBlockState(this.reusableBlockPos).getBlock();
            if (below == Blocks.ice || below == Blocks.packed_ice) {
               dist *= (double)2.5F;
            }

            return dist;
         }
      } else {
         return 0.115;
      }
   }

   private boolean isStairOrSlabNearby() {
      double yaw = Math.toRadians((double)mc.thePlayer.rotationYaw);
      double dx = -Math.sin(yaw);
      double dz = Math.cos(yaw);

      for(double dist = (double)0.0F; dist <= (double)1.0F; dist += (double)0.5F) {
         double checkX = mc.thePlayer.posX + dx * dist;
         double checkZ = mc.thePlayer.posZ + dz * dist;

         for(int yOff = -1; yOff <= 1; ++yOff) {
            this.reusableBlockPos.set(MathHelper.floor_double(checkX), MathHelper.floor_double(mc.thePlayer.posY + (double)yOff), MathHelper.floor_double(checkZ));
            Block block = mc.theWorld.getBlockState(this.reusableBlockPos).getBlock();
            if (block instanceof BlockStairs || block instanceof BlockSlab) {
               return true;
            }
         }
      }

      return false;
   }

   public void onPacketReceiveEvent(PacketReceiveEvent e) {
      if (e.getPacket() instanceof S08PacketPlayerPosLook && this.autoDisable.isEnabled()) {
         if (this.flagBehaviour.is("Cooldown")) {
            this.cooldownActive = true;
            this.cooldownStart = System.currentTimeMillis();
            NotificationManager.post(NotificationType.WARNING, "Flag Detector", "Speed cooldown activated due to " + (mc.thePlayer != null && mc.thePlayer.ticksExisted >= 5 ? "lagback" : "world change"), 1.5F);
         } else {
            NotificationManager.post(NotificationType.WARNING, "Flag Detector", "Speed disabled due to " + (mc.thePlayer != null && mc.thePlayer.ticksExisted >= 5 ? "lagback" : "world change"), 1.5F);
            this.toggleSilent();
         }
      }

   }

   public void onRender2DEvent(Render2DEvent event) {
      if (this.cooldownActive) {
         double elapsed = (double)(System.currentTimeMillis() - this.cooldownStart) / (double)1000.0F;
         double remaining = this.cooldownSetting.getValue() - elapsed;
         if (remaining <= (double)0.0F) {
            this.cooldownActive = false;
            return;
         }

         remaining = (double)Math.round(remaining * (double)10.0F) / (double)10.0F;
         ScaledResolution sr = RenderFrameCache.getScaledResolution();
         String text = "§lSpeed disabled for " + remaining + "s";
         tenacityFont18.drawStringWithShadow(text, (float)sr.getScaledWidth() / 2.0F - tenacityFont18.getStringWidth(text) / 2.0F, (float)sr.getScaledHeight() / 2.0F + 15.0F, (Color)HUDMod.getClientColors().getFirst());
      }

   }

   public boolean shouldPreventJumping() {
      return Tenacity.INSTANCE.isEnabled(Speed.class) && MovementUtils.isMoving() && (!this.mode.is("Watchdog") || !this.watchdogMode.is("Ground")) && (!this.mode.is("Vanilla") || !this.allowJumping.isEnabled());
   }

   public void onEnable() {
      this.speed = (double)1.5F;
      this.timerUtil.reset();
      if (mc.thePlayer != null) {
         this.wasOnGround = mc.thePlayer.onGround;
         if (this.mode.is("Miniblox") && ScaffoldUtils.getBlockSlot() == -1) {
            NotificationManager.post(NotificationType.WARNING, "Speed", "Miniblox mode needs blocks in your hotbar!");
            this.toggleSilent();
            return;
         }
      }

      this.inAirTicks = 0;
      this.moveSpeed = (double)0.0F;
      this.stage = 0;
      this.minibloxTestingWasMoving = false;
      this.minibloxTestingSlowStartAt = 0L;
      this.minibloxPrevSlot = -1;
      this.ncpLevel = 1;
      this.ncpMoveSpeed = MovementUtils.getBaseMoveSpeed();
      this.ncpLastDist = (double)0.0F;
      this.ncpTimerDelay = 0;
      this.ncpJumps = 0;
      this.ncpMotionTicks = 0;
      this.ncpFrameMove = false;
      this.ncpMotionDelay = 0;
      this.ncpGround = 0.0F;
      this.mosStage = 0;
      this.mosSpeed = MovementUtils.getBaseMoveSpeed();
      this.mosLastDist = (double)0.0F;
      this.mosAirTicks = 0;
      this.yportStage = 0;
      this.yportSpeed = (double)0.0F;
      this.yportLastDist = (double)0.0F;
      this.y = mc.thePlayer.posY;
      this.customInAirTicks = 0;
      this.customJumpDelayCounter = 0;
      this.customTimerTickCounter = 0;
      this.customUseTimer1 = true;
      if (this.mode.is("NCP") && this.ncpMode.is("Hop")) {
         mc.timer.timerSpeed = 1.0865F;
      }

      if (this.mode.is("NCP") && this.ncpMode.is("FHop")) {
         mc.timer.timerSpeed = 1.0866F;
      }

      super.onEnable();
   }

   public void onWorldEvent(WorldEvent event) {
      this.setToggled(false);
   }

   public void onDisable() {
      mc.timer.timerSpeed = 1.0F;
      if (!this.mode.is("Legit") && mc.thePlayer != null) {
         mc.thePlayer.motionX = (double)0.0F;
         mc.thePlayer.motionZ = (double)0.0F;
      }

      KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
      this.cooldownActive = false;
      this.cooldownStart = 0L;
      this.minibloxTestingWasMoving = false;
      this.minibloxTestingSlowStartAt = 0L;
      if (this.minibloxPrevSlot != -1 && mc.thePlayer != null) {
         PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(this.minibloxPrevSlot));
         this.minibloxPrevSlot = -1;
      }

      super.onDisable();
   }

   private boolean isTickInRange(ModeSetting mode, NumberSetting start, NumberSetting end) {
      int tick = this.customInAirTicks;
      switch (mode.getMode()) {
         case "Up to":
            return (double)tick <= start.getValue();
         case "From":
            return (double)tick >= start.getValue();
         case "During":
            return (double)tick >= start.getValue() && (double)tick <= end.getValue();
         default:
            return false;
      }
   }

   private double getCustomTimerValue() {
      if (this.timerMode.is("Static")) {
         return this.timerValue1.getValue();
      } else {
         ++this.customTimerTickCounter;
         if (this.customUseTimer1) {
            if ((double)this.customTimerTickCounter >= this.timerDelay1.getValue()) {
               this.customTimerTickCounter = 0;
               this.customUseTimer1 = false;
            }

            return this.timerValue1.getValue();
         } else {
            if ((double)this.customTimerTickCounter >= this.timerDelay2.getValue()) {
               this.customTimerTickCounter = 0;
               this.customUseTimer1 = true;
            }

            return this.timerValue2.getValue();
         }
      }
   }
}
