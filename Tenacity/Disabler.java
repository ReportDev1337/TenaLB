package dev.tenacity.module.impl.exploit;

import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.game.TickEvent;
import dev.tenacity.event.impl.game.WorldEvent;
import dev.tenacity.event.impl.network.PacketReceiveEvent;
import dev.tenacity.event.impl.network.PacketSendEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.movement.MoveFix;
import dev.tenacity.module.impl.movement.Speed;
import dev.tenacity.module.settings.ParentAttribute;
import dev.tenacity.module.settings.Setting;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.utils.misc.MathUtils;
import dev.tenacity.utils.player.PolarBoatHelper;
import dev.tenacity.utils.server.PacketUtils;
import dev.tenacity.utils.server.ServerUtils;
import dev.tenacity.utils.time.TimerUtil;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;

public final class Disabler extends Module {
   private final MultipleBoolSetting disablers = new MultipleBoolSetting("Disablers", new BooleanSetting[]{new BooleanSetting("Watchdog Strafe", false), new BooleanSetting("Watchdog Timer", false), new BooleanSetting("MMC", false), new BooleanSetting("C06->C04", false), new BooleanSetting("C04->C06", false), new BooleanSetting("Hover", false), new BooleanSetting("Spoof Ground", false), new BooleanSetting("C0B Cancel", false), new BooleanSetting("C0C Spam", false), new BooleanSetting("Verus", false), new BooleanSetting("Omni Sprint", false), new BooleanSetting("Void TP", false), new BooleanSetting("Silent S08", false), new BooleanSetting("Miniblox", false), new BooleanSetting("Polar", false)});
   private final NumberSetting hoverHeight = new NumberSetting("Hover Height", 0.1, (double)5.0F, 0.01, 0.01);
   private final BooleanSetting onGround = new BooleanSetting("On Ground", true);
   private final NumberSetting TPDelay = new NumberSetting("TP Delay", (double)20.0F, (double)100.0F, (double)5.0F, (double)1.0F);
   private final ModeSetting minibloxMode = new ModeSetting("Miniblox Mode", "Default", new String[]{"Default", "Down", "Offset", "Reactive"});
   private final NumberSetting minibloxCycle = new NumberSetting("Miniblox Cycle", (double)300.0F, (double)2000.0F, (double)50.0F, (double)50.0F);
   private final NumberSetting minibloxHold = new NumberSetting("Miniblox Hold", (double)500.0F, (double)3000.0F, (double)100.0F, (double)100.0F);
   private final NumberSetting minibloxOffset = new NumberSetting("Miniblox Offset", (double)20.0F, (double)50.0F, (double)5.0F, (double)1.0F);
   private final NumberSetting minibloxReactDist = new NumberSetting("React Dist", (double)20.0F, (double)50.0F, (double)5.0F, (double)1.0F);
   private final TimerUtil timer = new TimerUtil();
   private final TimerUtil joinTimer = new TimerUtil();
   private boolean synced;
   private int minibloxStage;
   private double minibloxTargetY;
   private boolean minibloxSpoof;
   private double minibloxSpoofX;
   private double minibloxSpoofY;
   private double minibloxSpoofZ;
   private double minibloxOffsetY;
   private double minibloxRealX;
   private double minibloxRealY;
   private double minibloxRealZ;
   private double minibloxSetbackX;
   private double minibloxSetbackY;
   private double minibloxSetbackZ;
   private final CopyOnWriteArrayList<Packet> watchdogPlayerPackets = new CopyOnWriteArrayList();
   private final CopyOnWriteArrayList<Packet> watchdogC0FC00Packets = new CopyOnWriteArrayList();
   private final CopyOnWriteArrayList<Packet> watchdogInvPackets = new CopyOnWriteArrayList();
   private final CopyOnWriteArrayList<Packet> packets = new CopyOnWriteArrayList();
   private double s08Y;
   public static boolean spiking;
   public static final TimerUtil spikeTimer = new TimerUtil();
   private int airTicks;
   private final PolarBoatHelper polar = new PolarBoatHelper(true);

   public Disabler() {
      super("Disabler", Category.EXPLOIT, "Disables some anticheats");
      this.TPDelay.addParent(this.disablers, (voidTPDisabler) -> voidTPDisabler.getSetting("Void TP").isEnabled());
      this.hoverHeight.addParent(this.disablers, (hoverDisabler) -> hoverDisabler.getSetting("Hover").isEnabled());
      this.onGround.addParent(this.disablers, (spoofGroundDisabler) -> spoofGroundDisabler.getSetting("Spoof Ground").isEnabled());
      this.minibloxMode.addParent(this.disablers, (d) -> d.getSetting("Miniblox").isEnabled());
      this.minibloxCycle.addParent(this.disablers, (d) -> d.getSetting("Miniblox").isEnabled());
      this.minibloxHold.addParent(this.minibloxMode, (m) -> m.is("Down"));
      this.minibloxOffset.addParent(this.minibloxMode, (m) -> m.is("Offset"));
      this.minibloxReactDist.addParent(this.minibloxMode, (m) -> m.is("Reactive"));
      this.polar.allowIncoming.addParent(this.disablers, (d) -> d.getSetting("Polar").isEnabled());
      this.polar.breakDelay.addParent(this.disablers, (d) -> d.getSetting("Polar").isEnabled());
      this.polar.autoplace.addParent(this.disablers, (d) -> d.getSetting("Polar").isEnabled());
      this.polar.expiryTime.addParent(this.disablers, (d) -> d.getSetting("Polar").isEnabled());
      this.polar.constantFlush.addParent(this.disablers, (d) -> d.getSetting("Polar").isEnabled());
      this.polar.constantFlushInterval.addParent(this.polar.constantFlush, ParentAttribute.BOOLEAN_CONDITION);
      this.polar.slowFlush.addParent(this.disablers, (d) -> d.getSetting("Polar").isEnabled());
      this.polar.slowFlushRate.addParent(this.polar.slowFlush, ParentAttribute.BOOLEAN_CONDITION);
      this.polar.cancelTransactions.addParent(this.disablers, (d) -> d.getSetting("Polar").isEnabled());
      this.polar.disableOnVelocity.addParent(this.disablers, (d) -> d.getSetting("Polar").isEnabled());
      this.addSettings(new Setting[]{this.disablers, this.TPDelay, this.onGround, this.hoverHeight, this.minibloxMode, this.minibloxCycle, this.minibloxHold, this.minibloxOffset, this.minibloxReactDist, this.polar.allowIncoming, this.polar.breakDelay, this.polar.autoplace, this.polar.expiryTime, this.polar.constantFlush, this.polar.constantFlushInterval, this.polar.slowFlush, this.polar.slowFlushRate, this.polar.cancelTransactions, this.polar.disableOnVelocity});
   }

   public void onEnable() {
      super.onEnable();
      if (this.disablers.getSetting("Polar").isEnabled()) {
         this.polar.reset();
      }

      if (this.disablers.getSetting("Miniblox").isEnabled()) {
         if (this.minibloxMode.is("Offset")) {
            double offset = this.minibloxOffset.getValue();
            this.minibloxOffsetY = mc.thePlayer.posY + offset;
            MoveFix.blocked = true;
            this.minibloxSpoof = true;
            this.minibloxSpoofY = this.minibloxOffsetY;

            for(int i = 0; i < 3; ++i) {
               mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, this.minibloxOffsetY, mc.thePlayer.posZ, false));
            }

            this.sendMinibloxPacket(this.minibloxOffsetY);
            this.minibloxSpoof = false;
         } else {
            this.minibloxStage = 0;
         }
      }

   }

   public void onDisable() {
      super.onDisable();
      this.polar.onDisable();
      if (this.disablers.getSetting("Miniblox").isEnabled() && this.minibloxMode.is("Offset")) {
         this.sendMinibloxPacket(mc.thePlayer.posY);
      }

      MoveFix.blocked = false;
      this.minibloxStage = 0;
   }

   public void onMotionEvent(MotionEvent event) {
      List<BooleanSetting> enabledBooleanSettings = (List)this.disablers.getBoolSettings().stream().filter(BooleanSetting::isEnabled).collect(Collectors.toList());
      if (enabledBooleanSettings.size() == 1) {
         this.setSuffix(((BooleanSetting)enabledBooleanSettings.get(0)).name);
      } else if (enabledBooleanSettings.size() > 1) {
         this.setSuffix(enabledBooleanSettings.size() + " Enabled");
      } else {
         this.setSuffix("None");
      }

      for(BooleanSetting booleanSetting : this.disablers.getBoolSettings()) {
         if (booleanSetting.isEnabled()) {
            switch (booleanSetting.name) {
               case "MMC":
                  if (this.timer.hasTimeElapsed((long)MathUtils.getRandomInRange(1000, 1500), true)) {
                     this.packets.forEach(PacketUtils::sendPacketNoEvent);
                     this.packets.clear();
                  }
                  break;
               case "Watchdog Timer":
                  if (spikeTimer.hasTimeElapsed(700L, true)) {
                     this.timer.reset();
                  }
                  break;
               case "C0C Spam":
                  PacketUtils.sendPacketNoEvent(new C0CPacketInput());
                  break;
               case "Miniblox":
                  if (event.isPre()) {
                     if (this.minibloxMode.is("Offset")) {
                        if (this.timer.hasTimeElapsed(1500L, true)) {
                           this.sendMinibloxPacket(this.minibloxOffsetY);
                        }
                     } else if (this.minibloxMode.is("Reactive")) {
                        if (this.minibloxStage == 1) {
                           double reactDist = this.minibloxReactDist.getValue();
                           MoveFix.blocked = true;
                           this.minibloxSpoof = true;
                           this.minibloxSpoofX = this.minibloxSetbackX;
                           this.minibloxSpoofY = this.minibloxSetbackY + reactDist;
                           this.minibloxSpoofZ = this.minibloxSetbackZ;
                           this.minibloxStage = 2;
                        } else if (this.minibloxStage == 2) {
                           this.sendMinibloxPacket(this.minibloxSpoofY);
                           this.minibloxStage = 3;
                        } else if (this.minibloxStage == 3) {
                           this.minibloxSpoofX = this.minibloxRealX;
                           this.minibloxSpoofY = this.minibloxRealY;
                           this.minibloxSpoofZ = this.minibloxRealZ;
                           this.minibloxStage = 4;
                        } else if (this.minibloxStage == 4) {
                           this.sendMinibloxPacket(this.minibloxSpoofY);
                           this.minibloxSpoof = false;
                           MoveFix.blocked = false;
                           MoveFix moveFix = (MoveFix)Tenacity.INSTANCE.getModuleCollection().getModule(MoveFix.class);
                           if (moveFix != null) {
                              moveFix.resetAccumulated();
                           }

                           this.minibloxStage = 0;
                        }
                     } else {
                        switch (this.minibloxStage) {
                           case 0:
                              if (this.timer.hasTimeElapsed((long)this.minibloxCycle.getValue(), true)) {
                                 this.minibloxTargetY = mc.thePlayer.posY;
                                 MoveFix.blocked = true;
                                 this.minibloxSpoof = true;
                                 this.minibloxSpoofX = mc.thePlayer.posX;
                                 this.minibloxSpoofY = this.minibloxTargetY + (double)30.0F;
                                 this.minibloxSpoofZ = mc.thePlayer.posZ;
                                 this.minibloxStage = 1;
                              }
                              break;
                           case 1:
                              this.sendMinibloxPacket(this.minibloxSpoofY);
                              this.minibloxStage = 2;
                              break;
                           case 2:
                              this.minibloxSpoofX = mc.thePlayer.posX;
                              this.minibloxSpoofY = this.minibloxTargetY;
                              this.minibloxSpoofZ = mc.thePlayer.posZ;
                              this.minibloxStage = 3;
                              break;
                           case 3:
                              this.sendMinibloxPacket(this.minibloxSpoofY);
                              this.minibloxSpoof = false;
                              if (this.minibloxMode.is("Down")) {
                                 this.timer.reset();
                                 this.minibloxStage = 4;
                              } else {
                                 this.minibloxStage = 0;
                              }
                              break;
                           case 4:
                              if (this.timer.hasTimeElapsed((long)this.minibloxHold.getValue(), true)) {
                                 this.minibloxStage = 0;
                              }
                        }
                     }
                  }
            }
         }
      }

   }

   public void onPacketSendEvent(PacketSendEvent event) {
      if (mc.thePlayer != null) {
         if (this.disablers.getSetting("Polar").isEnabled()) {
            this.polar.onPacketSend(event);
         }

         for(BooleanSetting booleanSetting : this.disablers.getBoolSettings()) {
            if (booleanSetting.isEnabled()) {
               switch (booleanSetting.name) {
                  case "Watchdog Strafe":
                     if (!ServerUtils.isOnHypixel() || !(event.getPacket() instanceof C03PacketPlayer) || mc.thePlayer.isPotionActive(Potion.jump)) {
                        break;
                     }

                     C03PacketPlayer c03 = (C03PacketPlayer)event.getPacket();
                     if (((Speed)Tenacity.INSTANCE.getModuleCollection().getModule(Speed.class)).isEnabled() && mc.thePlayer.fallDistance < 1.0F) {
                        c03.setOnGround(true);
                     }

                     if (((Speed)Tenacity.INSTANCE.getModuleCollection().getModule(Speed.class)).isEnabled() && mc.thePlayer.ticksExisted % 4 != 0 && mc.thePlayer.fallDistance < 1.0F) {
                        event.cancel();
                        this.watchdogPlayerPackets.add(event.getPacket());
                     } else if (!this.watchdogPlayerPackets.isEmpty()) {
                        this.watchdogPlayerPackets.forEach(PacketUtils::sendPacketNoEvent);
                        this.watchdogPlayerPackets.clear();
                     }
                     break;
                  case "Watchdog Timer":
                     if (!ServerUtils.isOnHypixel()) {
                        break;
                     }

                     if (event.getPacket() instanceof C03PacketPlayer) {
                        C03PacketPlayer c03 = (C03PacketPlayer)event.getPacket();
                        if (!c03.isMoving() && !c03.getRotating()) {
                           event.cancel();
                           break;
                        }
                     }

                     if (!this.timer.hasTimeElapsed(350L)) {
                        if (event.getPacket() instanceof C0FPacketConfirmTransaction || event.getPacket() instanceof C00PacketKeepAlive) {
                           event.cancel();
                           this.watchdogC0FC00Packets.add(event.getPacket());
                        }
                     } else if (!this.watchdogC0FC00Packets.isEmpty()) {
                        this.watchdogC0FC00Packets.forEach(PacketUtils::sendPacketNoEvent);
                        this.watchdogC0FC00Packets.clear();
                     }
                     break;
                  case "Void TP":
                     if (event.getPacket() instanceof C03PacketPlayer) {
                        C03PacketPlayer c03 = (C03PacketPlayer)event.getPacket();
                        if ((double)mc.thePlayer.ticksExisted % this.TPDelay.getValue() == (double)0.0F) {
                           c03.setY(c03.getPositionY() - (double)1000.0F);
                        }
                     }
                     break;
                  case "MMC":
                     if (event.getPacket() instanceof C0BPacketEntityAction) {
                        C0BPacketEntityAction c0b = (C0BPacketEntityAction)event.getPacket();
                        if (c0b.getAction().equals(C0BPacketEntityAction.Action.START_SPRINTING) && EntityPlayerSP.serverSprintState) {
                           PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                           EntityPlayerSP.serverSprintState = false;
                        }

                        event.cancel();
                     }

                     if (event.getPacket() instanceof C0FPacketConfirmTransaction || event.getPacket() instanceof C00PacketKeepAlive) {
                        event.cancel();
                        this.packets.add(event.getPacket());
                     }
                     break;
                  case "C06->C04":
                     if (event.getPacket() instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
                        C03PacketPlayer.C06PacketPlayerPosLook c06 = (C03PacketPlayer.C06PacketPlayerPosLook)event.getPacket();
                        event.setPacket(new C03PacketPlayer.C04PacketPlayerPosition(c06.getPositionX(), c06.getPositionY(), c06.getPositionZ(), c06.isOnGround()));
                     }
                     break;
                  case "C04->C06":
                     if (event.getPacket() instanceof C03PacketPlayer.C04PacketPlayerPosition) {
                        C03PacketPlayer.C04PacketPlayerPosition c04 = (C03PacketPlayer.C04PacketPlayerPosition)event.getPacket();
                        event.setPacket(new C03PacketPlayer.C06PacketPlayerPosLook(c04.getPositionX(), c04.getPositionY(), c04.getPositionZ(), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, c04.isOnGround()));
                     }
                     break;
                  case "Hover":
                     if (event.getPacket() instanceof C03PacketPlayer) {
                        C03PacketPlayer c03 = (C03PacketPlayer)event.getPacket();
                        c03.setY(mc.thePlayer.posY + this.hoverHeight.getValue());
                     }
                     break;
                  case "Spoof Ground":
                     if (event.getPacket() instanceof C03PacketPlayer) {
                        C03PacketPlayer c03 = (C03PacketPlayer)event.getPacket();
                        c03.setOnGround(this.onGround.isEnabled());
                     }
                     break;
                  case "C0B Cancel":
                     if (event.getPacket() instanceof C0BPacketEntityAction) {
                        event.cancel();
                     }
                     break;
                  case "Verus":
                     if (event.getPacket() instanceof C0FPacketConfirmTransaction || event.getPacket() instanceof C00PacketKeepAlive) {
                        event.cancel();
                     }
                     break;
                  case "Omni Sprint":
                     if (event.getPacket() instanceof C0BPacketEntityAction) {
                        C0BPacketEntityAction c0b = (C0BPacketEntityAction)event.getPacket();
                        if (c0b.getAction().equals(C0BPacketEntityAction.Action.START_SPRINTING) && EntityPlayerSP.serverSprintState) {
                           PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                           EntityPlayerSP.serverSprintState = false;
                        }

                        event.cancel();
                     }
                     break;
                  case "Miniblox":
                     if (this.minibloxSpoof && event.getPacket() instanceof C03PacketPlayer) {
                        C03PacketPlayer c03 = (C03PacketPlayer)event.getPacket();
                        if (c03.isMoving()) {
                           c03.setX(this.minibloxSpoofX);
                           c03.setY(this.minibloxSpoofY);
                           c03.setZ(this.minibloxSpoofZ);
                        }
                     }
               }
            }
         }

      }
   }

   public void onPacketReceiveEvent(PacketReceiveEvent event) {
      if (this.disablers.getSetting("Polar").isEnabled()) {
         this.polar.onPacketReceive(event);
      }

      for(BooleanSetting booleanSetting : this.disablers.getBoolSettings()) {
         if (booleanSetting.isEnabled()) {
            switch (booleanSetting.name) {
               case "Silent S08":
                  if (event.getPacket() instanceof S08PacketPlayerPosLook) {
                     S08PacketPlayerPosLook s08 = (S08PacketPlayerPosLook)event.getPacket();
                     event.cancel();
                     PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(s08.getX(), s08.getY(), s08.getZ(), s08.getYaw(), s08.getPitch(), false));
                     if (!mc.getNetHandler().doneLoadingTerrain) {
                        mc.getNetHandler().doneLoadingTerrain = true;
                        mc.displayGuiScreen((GuiScreen)null);
                     }
                  }
                  break;
               case "Miniblox":
                  if (this.minibloxMode.is("Reactive") && event.getPacket() instanceof S08PacketPlayerPosLook) {
                     S08PacketPlayerPosLook s08 = (S08PacketPlayerPosLook)event.getPacket();
                     event.cancel();
                     this.minibloxRealX = mc.thePlayer.posX;
                     this.minibloxRealY = mc.thePlayer.posY;
                     this.minibloxRealZ = mc.thePlayer.posZ;
                     this.minibloxSetbackX = s08.getX();
                     this.minibloxSetbackY = s08.getY();
                     this.minibloxSetbackZ = s08.getZ();
                     this.minibloxStage = 1;
                  }
            }
         }
      }

   }

   private boolean isBlockUnder() {
      if (mc.thePlayer.posY < (double)0.0F) {
         return false;
      } else {
         for(int offset = 0; offset < (int)mc.thePlayer.posY + 2; offset += 2) {
            AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset((double)0.0F, (double)(-offset), (double)0.0F);
            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
               return true;
            }
         }

         return false;
      }
   }

   private void sendMinibloxPacket(double y) {
      PacketBuffer packetbuffer = new PacketBuffer(Unpooled.buffer());
      packetbuffer.writeDouble(mc.thePlayer.lastTickPosX);
      packetbuffer.writeDouble(y);
      packetbuffer.writeDouble(mc.thePlayer.lastTickPosZ);
      packetbuffer.writeFloat(mc.thePlayer.rotationYaw);
      packetbuffer.writeFloat(mc.thePlayer.rotationPitch);
      packetbuffer.writeFloat(mc.thePlayer.movementInput.moveForward);
      packetbuffer.writeFloat(mc.thePlayer.movementInput.moveStrafe);
      packetbuffer.writeBoolean(mc.thePlayer.movementInput.jump);
      packetbuffer.writeBoolean(mc.thePlayer.movementInput.sneak);
      boolean spoofOnGround = this.disablers.getSetting("Spoof Ground").isEnabled() ? this.onGround.isEnabled() : mc.thePlayer.onGround;
      packetbuffer.writeBoolean(spoofOnGround);
      mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload("miniblox:movepacket", packetbuffer));
   }

   public void onTickEvent(TickEvent event) {
      if (event.isPre() && mc.thePlayer != null) {
         if (this.disablers.getSetting("Polar").isEnabled()) {
            this.polar.onTick();
         } else {
            this.polar.onDisable();
         }

      }
   }

   public void onWorldEvent(WorldEvent event) {
      this.polar.reset();
      this.watchdogC0FC00Packets.clear();
      this.timer.reset();
   }
}
