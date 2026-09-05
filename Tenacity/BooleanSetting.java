package dev.tenacity.module.settings.impl;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.tenacity.module.settings.Setting;

public class BooleanSetting extends Setting {
   @Expose
   @SerializedName("name")
   private boolean state;

   public BooleanSetting(String name, boolean state) {
      this.name = name;
      this.state = state;
   }

   public boolean isEnabled() {
      return this.state;
   }

   public void toggle() {
      this.setState(!this.isEnabled());
   }

   public void setState(boolean state) {
      this.state = state;
   }

   public Boolean getConfigValue() {
      return this.isEnabled();
   }
}
