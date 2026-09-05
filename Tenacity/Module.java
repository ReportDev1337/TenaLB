package dev.tenacity.module;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.tenacity.Tenacity;
import dev.tenacity.config.ConfigSetting;
import dev.tenacity.event.ListenerAdapter;
import dev.tenacity.module.impl.render.NotificationsMod;
import dev.tenacity.module.settings.Setting;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ColorSetting;
import dev.tenacity.module.settings.impl.KeybindSetting;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.module.settings.impl.StringSetting;
import dev.tenacity.ui.notifications.NotificationManager;
import dev.tenacity.ui.notifications.NotificationType;
import dev.tenacity.utils.Utils;
import dev.tenacity.utils.animations.Animation;
import dev.tenacity.utils.animations.Direction;
import dev.tenacity.utils.animations.impl.DecelerateAnimation;
import dev.tenacity.utils.misc.Multithreading;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Generated;

public class Module extends ListenerAdapter implements Utils {
   @Expose
   @SerializedName("name")
   private final String name;
   private final String description;
   private final Category category;
   private final ArrayList<Setting> settingsList = new ArrayList();
   private String suffix;
   private String author = "";
   @Expose
   @SerializedName("toggled")
   protected boolean enabled;
   @Expose
   @SerializedName("settings")
   public ConfigSetting[] cfgSettings;
   private boolean expanded;
   private final Animation animation;
   public static int categoryCount;
   public static float allowedClickGuiHeight = 300.0F;
   private Map<String, Setting> settingsMap;
   private boolean settingsDirty;
   private final KeybindSetting keybind;

   public Module(String name, Category category, String description) {
      this.animation = (new DecelerateAnimation(250, (double)1.0F)).setDirection(Direction.BACKWARDS);
      this.settingsDirty = true;
      this.keybind = new KeybindSetting(0);
      this.name = name;
      this.category = category;
      this.description = description;
      this.addSettings(this.keybind);
   }

   public boolean isInGame() {
      return mc.theWorld != null && mc.thePlayer != null;
   }

   public void addSettings(Setting... settings) {
      this.settingsList.addAll(Arrays.asList(settings));
      this.settingsDirty = true;
   }

   private Setting getCachedSetting(String name) {
      if (this.settingsDirty || this.settingsMap == null) {
         this.settingsMap = new HashMap();

         for(Setting s : this.settingsList) {
            this.settingsMap.put(s.getName().toLowerCase(), s);
         }

         this.settingsDirty = false;
      }

      return (Setting)this.settingsMap.get(name.toLowerCase());
   }

   public void setToggled(boolean toggled) {
      this.enabled = toggled;
      if (toggled) {
         this.onEnable();
      } else {
         this.onDisable();
      }

   }

   public void toggleSilent() {
      this.toggleSilent(!this.enabled);
   }

   public void toggleSilent(boolean toggled) {
      this.enabled = toggled;
      if (toggled) {
         this.onEnable();
      } else {
         this.onDisable();
      }

   }

   public void toggle() {
      this.toggleSilent();
      if (NotificationsMod.toggleNotifications.isEnabled()) {
         String title = "Module toggled";
         String description = "";
         switch (NotificationsMod.mode.getMode()) {
            case "Default":
               if (NotificationsMod.onlyTitle.isEnabled()) {
                  title = this.getName() + " toggled";
               }

               description = this.getName() + " was " + (this.enabled ? "§aenabled" : "§cdisabled") + "\r";
               break;
            case "SuicideX":
               title = this.enabled ? "Enabled Module " + this.getName() + ". PogO" : "Disabled Module " + this.getName() + ". :/";
         }

         NotificationManager.post(this.enabled ? NotificationType.SUCCESS : NotificationType.DISABLE, title, description);
      }

   }

   public boolean hasMode() {
      return this.suffix != null;
   }

   public long getUnregisterDelay() {
      return 0L;
   }

   public void onEnable() {
      Tenacity.INSTANCE.getEventProtocol().register(this);
   }

   public void onDisable() {
      long delay = this.getUnregisterDelay();
      if (delay > 0L) {
         Multithreading.schedule(() -> Tenacity.INSTANCE.getEventProtocol().unregister(this), delay, TimeUnit.MILLISECONDS);
      } else {
         Tenacity.INSTANCE.getEventProtocol().unregister(this);
      }

   }

   public void setKey(int code) {
      this.keybind.setCode(code);
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public int getKeybindCode() {
      return this.keybind.getCode();
   }

   public NumberSetting getNumberSetting(String name) {
      Setting s = this.getCachedSetting(name);
      return s instanceof NumberSetting ? (NumberSetting)s : null;
   }

   public BooleanSetting getBooleanSetting(String name) {
      Setting s = this.getCachedSetting(name);
      return s instanceof BooleanSetting ? (BooleanSetting)s : null;
   }

   public ModeSetting getModeSetting(String name) {
      Setting s = this.getCachedSetting(name);
      return s instanceof ModeSetting ? (ModeSetting)s : null;
   }

   public StringSetting getStringSetting(String name) {
      Setting s = this.getCachedSetting(name);
      return s instanceof StringSetting ? (StringSetting)s : null;
   }

   public MultipleBoolSetting getMultiBoolSetting(String name) {
      Setting s = this.getCachedSetting(name);
      return s instanceof MultipleBoolSetting ? (MultipleBoolSetting)s : null;
   }

   public ColorSetting getColorSetting(String name) {
      Setting s = this.getCachedSetting(name);
      return s instanceof ColorSetting ? (ColorSetting)s : null;
   }

   @Generated
   public Category getCategory() {
      return this.category;
   }

   @Generated
   public ArrayList<Setting> getSettingsList() {
      return this.settingsList;
   }

   @Generated
   public String getSuffix() {
      return this.suffix;
   }

   @Generated
   public String getAuthor() {
      return this.author;
   }

   @Generated
   public ConfigSetting[] getCfgSettings() {
      return this.cfgSettings;
   }

   @Generated
   public boolean isExpanded() {
      return this.expanded;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }

   @Generated
   public Map<String, Setting> getSettingsMap() {
      return this.settingsMap;
   }

   @Generated
   public boolean isSettingsDirty() {
      return this.settingsDirty;
   }

   @Generated
   public KeybindSetting getKeybind() {
      return this.keybind;
   }

   @Generated
   public void setSuffix(String suffix) {
      this.suffix = suffix;
   }

   @Generated
   public void setAuthor(String author) {
      this.author = author;
   }

   @Generated
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   @Generated
   public void setCfgSettings(ConfigSetting[] cfgSettings) {
      this.cfgSettings = cfgSettings;
   }

   @Generated
   public void setExpanded(boolean expanded) {
      this.expanded = expanded;
   }

   @Generated
   public void setSettingsMap(Map<String, Setting> settingsMap) {
      this.settingsMap = settingsMap;
   }

   @Generated
   public void setSettingsDirty(boolean settingsDirty) {
      this.settingsDirty = settingsDirty;
   }
}
