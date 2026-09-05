package dev.tenacity.module.settings.impl;

import dev.tenacity.module.settings.Setting;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MultipleBoolSetting extends Setting {
   private final Map<String, BooleanSetting> boolSettings;
   private HashMap<String, Boolean> cachedConfigValue;
   private boolean configDirty = true;

   public MultipleBoolSetting(String name, String... booleanSettingNames) {
      this.name = name;
      this.boolSettings = new HashMap();
      Arrays.stream(booleanSettingNames).forEach((boolName) -> {
         BooleanSetting var10000 = (BooleanSetting)this.boolSettings.put(boolName.toLowerCase(), new BooleanSetting(boolName, false));
      });
   }

   public MultipleBoolSetting(String name, BooleanSetting... booleanSettings) {
      this.name = name;
      this.boolSettings = new HashMap();
      Arrays.stream(booleanSettings).forEach((booleanSetting) -> {
         BooleanSetting var10000 = (BooleanSetting)this.boolSettings.put(booleanSetting.name.toLowerCase(), booleanSetting);
      });
   }

   public BooleanSetting getSetting(String settingName) {
      BooleanSetting setting = (BooleanSetting)this.boolSettings.computeIfAbsent(settingName.toLowerCase(), (k) -> null);
      this.configDirty = true;
      return setting;
   }

   public boolean isEnabled(String settingName) {
      return ((BooleanSetting)this.boolSettings.get(settingName.toLowerCase())).isEnabled();
   }

   public Collection<BooleanSetting> getBoolSettings() {
      return this.boolSettings.values();
   }

   public HashMap<String, Boolean> getConfigValue() {
      if (this.configDirty || this.cachedConfigValue == null) {
         this.cachedConfigValue = new HashMap();

         for(BooleanSetting booleanSetting : this.boolSettings.values()) {
            this.cachedConfigValue.put(booleanSetting.name, booleanSetting.isEnabled());
         }

         this.configDirty = false;
      }

      return this.cachedConfigValue;
   }
}
