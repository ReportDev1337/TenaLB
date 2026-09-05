package dev.tenacity.utils.player;

import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.network.PacketReceiveEvent;
import dev.tenacity.event.impl.network.PacketSendEvent;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.utils.Utils;
import dev.tenacity.utils.lag.Direction;
import dev.tenacity.utils.lag.FlushMode;
import dev.tenacity.utils.lag.PacketFilter;
import dev.tenacity.utils.lag.PacketQueue;
import dev.tenacity.utils.server.PacketUtils;
import dev.tenacity.utils.time.TimerUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBoat;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S05PacketSpawnPosition;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0APacketUseBed;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S10PacketSpawnPainting;
import net.minecraft.network.play.server.S11PacketSpawnExperienceOrb;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1EPacketRemoveEntityEffect;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S24PacketBlockAction;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S28PacketEffect;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S31PacketWindowProperty;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S33PacketUpdateSign;
import net.minecraft.network.play.server.S34PacketMaps;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.network.play.server.S36PacketSignEditorOpen;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S3APacketTabComplete;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.network.play.server.S41PacketServerDifficulty;
import net.minecraft.network.play.server.S42PacketCombatEvent;
import net.minecraft.network.play.server.S43PacketCamera;
import net.minecraft.network.play.server.S44PacketWorldBorder;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.network.play.server.S46PacketSetCompressionLevel;
import net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter;
import net.minecraft.network.play.server.S48PacketResourcePackSend;
import net.minecraft.network.play.server.S49PacketUpdateEntityNBT;
import net.minecraft.util.EnumChatFormatting;

public class PolarBoatHelper implements Utils {
   public final MultipleBoolSetting allowIncoming;
   public final NumberSetting breakDelay;
   public final BooleanSetting autoplace;
   public final NumberSetting expiryTime;
   public final BooleanSetting constantFlush;
   public final NumberSetting constantFlushInterval;
   public final BooleanSetting slowFlush;
   public final NumberSetting slowFlushRate;
   public final BooleanSetting cancelTransactions;
   public final BooleanSetting disableOnVelocity;
   private final String queueName;
   private final boolean resyncOnExpire;
   private final TimerUtil placeTimer;
   private final TimerUtil expiryTimer;
   private int placeStage;
   private boolean expired;
   private boolean warned;
   private boolean slowFlushing;
   private int lastQueueSize;
   private int oneTickPackets;
   private PacketQueue blinkQueue;
   private int ticks;
   private boolean brokeBoat;
   private EntityBoat boat;

   public PolarBoatHelper() {
      this(false);
   }

   public PolarBoatHelper(boolean resyncOnExpire) {
      this.allowIncoming = new MultipleBoolSetting("Allow Incoming", new String[]{"Spawns", "Moves", "Damage", "Metadata", "Attach", "Chat", "World", "Inventory", "Health", "Game", "Scoreboard", "Other", "S08"});
      this.breakDelay = new NumberSetting("Break Delay", (double)4.0F, (double)20.0F, (double)0.0F, (double)1.0F);
      this.autoplace = new BooleanSetting("Auto Place", false);
      this.expiryTime = new NumberSetting("Expiry Time", (double)0.0F, (double)300.0F, (double)0.0F, (double)1.0F);
      this.constantFlush = new BooleanSetting("Constant Flush", false);
      this.constantFlushInterval = new NumberSetting("Constant Flush Interval", (double)10.0F, (double)100.0F, (double)1.0F, (double)1.0F);
      this.slowFlush = new BooleanSetting("Slow Flush", false);
      this.slowFlushRate = new NumberSetting("Slow Flush Rate", (double)5.0F, (double)20.0F, (double)1.0F, (double)1.0F);
      this.cancelTransactions = new BooleanSetting("Cancel Transactions", false);
      this.disableOnVelocity = new BooleanSetting("Disable on Velocity", false);
      this.placeTimer = new TimerUtil();
      this.expiryTimer = new TimerUtil();
      this.oneTickPackets = 1;
      this.resyncOnExpire = resyncOnExpire;
      this.queueName = "polar_blink_" + Integer.toHexString(System.identityHashCode(this));
   }

   public void reset() {
      this.ticks = 0;
      this.brokeBoat = false;
      this.placeStage = 0;
      this.expired = false;
      this.warned = false;
      this.slowFlushing = false;
      this.lastQueueSize = 0;
      this.oneTickPackets = 1;
      this.expiryTimer.reset();
      this.blinkQueue = null;
      this.boat = null;
   }

   public void onDisable() {
      Tenacity.INSTANCE.lagController.unregisterQueue(this.queueName);
      this.reset();
   }

   public void onPacketSend(PacketSendEvent event) {
      if (this.cancelTransactions.isEnabled() && event.getPacket() instanceof C0FPacketConfirmTransaction) {
         event.cancel();
      }

   }

   public void onPacketReceive(PacketReceiveEvent event) {
      if (this.disableOnVelocity.isEnabled() && this.boat != null && mc.thePlayer != null) {
         if (event.getPacket() instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity)event.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
            this.resyncNow();
         }

      }
   }

   private void resyncNow() {
      if (this.resyncOnExpire) {
         this.onDisable();
      } else {
         this.expired = true;
      }

   }

   public boolean hasBoat() {
      return this.boat != null;
   }

   public boolean hasExpired() {
      return this.expired;
   }

   public void onTick() {
      if (mc.thePlayer != null) {
         if (this.slowFlushing) {
            this.updateSlowFlush();
         } else {
            if (this.boat == null && this.autoplace.isEnabled()) {
               this.updateAutoPlace();
            }

            if (mc.thePlayer.ridingEntity instanceof EntityBoat && mc.thePlayer.ridingEntity != this.boat) {
               this.boat = (EntityBoat)mc.thePlayer.ridingEntity;
               this.brokeBoat = false;
               this.warned = false;
               this.expiryTimer.reset();
               if (this.blinkQueue == null) {
                  this.blinkQueue = new PacketQueue(new PacketFilter(Direction.INBOUND, this::shouldHold), FlushMode.MANUAL);
                  Tenacity.INSTANCE.lagController.registerQueue(this.queueName, this.blinkQueue);
                  this.blinkQueue.start();
               }
            }

            ++this.ticks;
            if (this.boat != null) {
               int delay = this.breakDelay.getValue().intValue();
               if (delay <= 0) {
                  if (!this.brokeBoat) {
                     this.brokeBoat = true;

                     for(int i = 0; i < 5; ++i) {
                        PacketUtils.sendPacketNoEvent(new C02PacketUseEntity(this.boat, C02PacketUseEntity.Action.ATTACK));
                     }
                  }
               } else if (this.ticks % delay == 0) {
                  PacketUtils.sendPacketNoEvent(new C02PacketUseEntity(this.boat, C02PacketUseEntity.Action.ATTACK));
               }
            }

            this.trackArrivals();
            this.checkExpiry();
         }
      }
   }

   private void trackArrivals() {
      if (this.blinkQueue == null) {
         this.lastQueueSize = 0;
      } else {
         int size = this.blinkQueue.size();
         int arrived = Math.max(0, size - this.lastQueueSize);
         this.oneTickPackets = Math.max(1, arrived);
         if (this.constantFlush.isEnabled() && arrived > 0 && this.ticks % this.constantFlushInterval.getValue().intValue() == 0) {
            this.blinkQueue.flush(arrived);
            this.lastQueueSize = this.blinkQueue.size();
         } else {
            this.lastQueueSize = size;
         }

      }
   }

   private void startSlowFlush() {
      if (this.blinkQueue != null) {
         this.blinkQueue.stop();
         this.slowFlushing = true;
      } else {
         this.finishSlowFlush();
      }

   }

   private void updateSlowFlush() {
      if (this.blinkQueue != null && !this.blinkQueue.isEmpty()) {
         int perTick = this.slowFlushRate.getValue().intValue() * this.oneTickPackets;
         this.blinkQueue.flush(perTick);
      } else {
         this.finishSlowFlush();
      }
   }

   private void finishSlowFlush() {
      this.slowFlushing = false;
      if (this.blinkQueue != null) {
         Tenacity.INSTANCE.lagController.unregisterQueue(this.queueName);
         this.blinkQueue = null;
      }

      if (this.resyncOnExpire) {
         this.reset();
      } else {
         this.expired = true;
      }

   }

   private void checkExpiry() {
      if (this.boat != null) {
         int expiry = this.expiryTime.getValue().intValue();
         if (expiry > 0) {
            long remaining = (long)expiry * 1000L - this.expiryTimer.getTime();
            if (!this.warned && remaining <= 5000L) {
               this.warned = true;
               int seconds = Math.max(1, (int)((remaining + 999L) / 1000L));
               ChatUtil.print("Polar", EnumChatFormatting.YELLOW, (this.resyncOnExpire ? "Resync in " : "Disable in ") + seconds + "s");
            }

            if (this.expiryTimer.hasTimeElapsed((long)expiry * 1000L)) {
               if (this.slowFlush.isEnabled()) {
                  this.startSlowFlush();
               } else {
                  this.resyncNow();
               }
            }

         }
      }
   }

   private void updateAutoPlace() {
      int hotbarSlot = InventoryUtils.getItemSlot(Items.boat);
      int invSlot = -1;
      if (hotbarSlot == -1) {
         for(int i = 9; i < 36; ++i) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBoat) {
               invSlot = i;
               break;
            }
         }
      }

      if (hotbarSlot == -1 && invSlot == -1) {
         this.placeStage = 0;
      } else {
         switch (this.placeStage) {
            case 0:
               if (hotbarSlot != -1) {
                  mc.thePlayer.inventory.currentItem = hotbarSlot;
                  this.placeStage = 2;
               } else {
                  mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
                  this.placeStage = 1;
               }

               this.placeTimer.reset();
               break;
            case 1:
               if (this.placeTimer.hasTimeElapsed(150L)) {
                  InventoryUtils.swap(invSlot, mc.thePlayer.inventory.currentItem);
                  mc.displayGuiScreen((GuiScreen)null);
                  this.placeStage = 2;
                  this.placeTimer.reset();
               }
               break;
            case 2:
               if (mc.thePlayer.onGround) {
                  mc.thePlayer.jump();
               }

               if (this.placeTimer.hasTimeElapsed(250L)) {
                  this.placeStage = 3;
                  this.placeTimer.reset();
               }
               break;
            case 3:
               mc.thePlayer.rotationPitch = 90.0F;
               this.placeStage = 4;
               this.placeTimer.reset();
               break;
            case 4:
               if (this.placeTimer.hasTimeElapsed(100L)) {
                  ItemStack held = mc.thePlayer.getHeldItem();
                  if (held != null && held.getItem() instanceof ItemBoat) {
                     mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, held);
                     this.placeStage = 5;
                  } else {
                     this.placeStage = 0;
                  }

                  this.placeTimer.reset();
               }
               break;
            case 5:
               if (this.placeTimer.hasTimeElapsed(200L)) {
                  EntityBoat nearest = this.getNearestBoat((double)8.0F);
                  if (nearest != null) {
                     mc.playerController.interactWithEntitySendPacket(mc.thePlayer, nearest);
                     this.placeStage = 6;
                  } else {
                     this.placeStage = 3;
                  }

                  this.placeTimer.reset();
               }
               break;
            case 6:
               if (this.placeTimer.hasTimeElapsed(1500L)) {
                  this.placeStage = 5;
                  this.placeTimer.reset();
               }
         }

      }
   }

   private EntityBoat getNearestBoat(double range) {
      EntityBoat nearest = null;
      double best = range * range;

      for(Entity entity : mc.theWorld.loadedEntityList) {
         if (entity instanceof EntityBoat) {
            double d = mc.thePlayer.getDistanceSqToEntity(entity);
            if (d < best) {
               best = d;
               nearest = (EntityBoat)entity;
            }
         }
      }

      return nearest;
   }

   private boolean shouldHold(Packet<?> packet) {
      if (packet instanceof S12PacketEntityVelocity) {
         return false;
      } else if (packet instanceof S13PacketDestroyEntities) {
         return false;
      } else if (this.allowIncoming.isEnabled("S08") && packet instanceof S08PacketPlayerPosLook) {
         return false;
      } else if (!this.allowIncoming.isEnabled("Spawns") || !(packet instanceof S0CPacketSpawnPlayer) && !(packet instanceof S0EPacketSpawnObject) && !(packet instanceof S0FPacketSpawnMob) && !(packet instanceof S10PacketSpawnPainting) && !(packet instanceof S11PacketSpawnExperienceOrb) && !(packet instanceof S2CPacketSpawnGlobalEntity)) {
         if (!this.allowIncoming.isEnabled("Moves") || !(packet instanceof S14PacketEntity) && !(packet instanceof S18PacketEntityTeleport)) {
            if (!this.allowIncoming.isEnabled("Damage") || !(packet instanceof S19PacketEntityStatus) && !(packet instanceof S0BPacketAnimation) && !(packet instanceof S0DPacketCollectItem)) {
               if (!this.allowIncoming.isEnabled("Metadata") || !(packet instanceof S1CPacketEntityMetadata) && !(packet instanceof S19PacketEntityHeadLook) && !(packet instanceof S04PacketEntityEquipment) && !(packet instanceof S1DPacketEntityEffect) && !(packet instanceof S1EPacketRemoveEntityEffect) && !(packet instanceof S20PacketEntityProperties)) {
                  if (this.allowIncoming.isEnabled("Attach") && packet instanceof S1BPacketEntityAttach) {
                     return false;
                  } else if (!this.allowIncoming.isEnabled("Chat") || !(packet instanceof S02PacketChat) && !(packet instanceof S38PacketPlayerListItem) && !(packet instanceof S3APacketTabComplete) && !(packet instanceof S3FPacketCustomPayload)) {
                     if (!this.allowIncoming.isEnabled("World") || !(packet instanceof S21PacketChunkData) && !(packet instanceof S22PacketMultiBlockChange) && !(packet instanceof S23PacketBlockChange) && !(packet instanceof S24PacketBlockAction) && !(packet instanceof S25PacketBlockBreakAnim) && !(packet instanceof S26PacketMapChunkBulk) && !(packet instanceof S27PacketExplosion) && !(packet instanceof S28PacketEffect) && !(packet instanceof S29PacketSoundEffect) && !(packet instanceof S2APacketParticles) && !(packet instanceof S2BPacketChangeGameState) && !(packet instanceof S33PacketUpdateSign) && !(packet instanceof S34PacketMaps) && !(packet instanceof S35PacketUpdateTileEntity)) {
                        if (!this.allowIncoming.isEnabled("Inventory") || !(packet instanceof S09PacketHeldItemChange) && !(packet instanceof S2DPacketOpenWindow) && !(packet instanceof S2EPacketCloseWindow) && !(packet instanceof S2FPacketSetSlot) && !(packet instanceof S30PacketWindowItems) && !(packet instanceof S31PacketWindowProperty) && !(packet instanceof S32PacketConfirmTransaction) && !(packet instanceof S36PacketSignEditorOpen)) {
                           if (!this.allowIncoming.isEnabled("Health") || !(packet instanceof S06PacketUpdateHealth) && !(packet instanceof S1FPacketSetExperience) && !(packet instanceof S39PacketPlayerAbilities) && !(packet instanceof S42PacketCombatEvent)) {
                              if (!this.allowIncoming.isEnabled("Game") || !(packet instanceof S01PacketJoinGame) && !(packet instanceof S03PacketTimeUpdate) && !(packet instanceof S05PacketSpawnPosition) && !(packet instanceof S07PacketRespawn) && !(packet instanceof S40PacketDisconnect) && !(packet instanceof S41PacketServerDifficulty) && !(packet instanceof S43PacketCamera) && !(packet instanceof S44PacketWorldBorder) && !(packet instanceof S45PacketTitle) && !(packet instanceof S46PacketSetCompressionLevel) && !(packet instanceof S47PacketPlayerListHeaderFooter) && !(packet instanceof S48PacketResourcePackSend)) {
                                 if (!this.allowIncoming.isEnabled("Scoreboard") || !(packet instanceof S3BPacketScoreboardObjective) && !(packet instanceof S3CPacketUpdateScore) && !(packet instanceof S3DPacketDisplayScoreboard) && !(packet instanceof S3EPacketTeams)) {
                                    return !this.allowIncoming.isEnabled("Other") || !(packet instanceof S00PacketKeepAlive) && !(packet instanceof S0APacketUseBed) && !(packet instanceof S37PacketStatistics) && !(packet instanceof S49PacketUpdateEntityNBT);
                                 } else {
                                    return false;
                                 }
                              } else {
                                 return false;
                              }
                           } else {
                              return false;
                           }
                        } else {
                           return false;
                        }
                     } else {
                        return false;
                     }
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
