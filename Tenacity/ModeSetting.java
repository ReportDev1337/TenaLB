package dev.tenacity.module.settings.impl;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.tenacity.module.settings.Setting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ModeSetting extends Setting {
   public List<String> modes;
   private final HashMap<String, ArrayList<Setting>> childrenMap = new HashMap();
   private String defaultMode;
   private int modeIndex;
   @Expose
   @SerializedName("value")
   private String currentMode;

   public ModeSetting(String name, String defaultMode, String... modes) {
      this.name = name;
      this.modes = new ArrayList(Arrays.asList(modes));
      this.modeIndex = this.modes.indexOf(defaultMode);
      if (this.currentMode == null) {
         this.currentMode = defaultMode;
      }

   }

   public String getMode() {
      return this.currentMode;
   }

   public boolean is(String mode) {
      return this.currentMode.equalsIgnoreCase(mode);
   }

   public void cycleForwards() {
      ++this.modeIndex;
      if (this.modeIndex > this.modes.size() - 1) {
         this.modeIndex = 0;
      }

      this.currentMode = (String)this.modes.get(this.modeIndex);
   }

   public void cycleBackwards() {
      --this.modeIndex;
      if (this.modeIndex < 0) {
         this.modeIndex = this.modes.size() - 1;
      }

      this.currentMode = (String)this.modes.get(this.modeIndex);
   }

   public void setCurrentMode(String currentMode) {
      this.currentMode = currentMode;
   }

   public void setModes(List<String> newModes) {
      if (newModes != null && !newModes.isEmpty()) {
         this.modes.clear();
         this.modes.addAll(newModes);
         if (!this.modes.contains(this.currentMode)) {
            this.currentMode = (String)this.modes.get(0);
            this.modeIndex = 0;
         } else {
            this.modeIndex = this.modes.indexOf(this.currentMode);
         }

      }
   }

   public String getConfigValue() {
      return this.currentMode;
   }
}
