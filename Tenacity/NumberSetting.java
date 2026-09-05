package dev.tenacity.module.settings.impl;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.tenacity.module.settings.Setting;
import java.util.function.Supplier;

public class NumberSetting extends Setting {
   private final double maxValue;
   private final double minValue;
   private final double increment;
   private final double defaultValue;
   private Supplier<Double> dynamicMax = null;
   private Supplier<Double> dynamicMin = null;
   @Expose
   @SerializedName("value")
   private Double value;

   public NumberSetting(String name, double defaultValue, double maxValue, double minValue, double increment) {
      this.name = name;
      this.maxValue = maxValue;
      this.minValue = minValue;
      this.value = defaultValue;
      this.defaultValue = defaultValue;
      this.increment = increment;
   }

   public NumberSetting setDynamicMax(Supplier<Double> supplier) {
      this.dynamicMax = supplier;
      return this;
   }

   public NumberSetting setDynamicMin(Supplier<Double> supplier) {
      this.dynamicMin = supplier;
      return this;
   }

   private static double clamp(double value, double min, double max) {
      value = Math.max(min, value);
      value = Math.min(max, value);
      return value;
   }

   public double getMaxValue() {
      return this.dynamicMax != null ? (Double)this.dynamicMax.get() : this.maxValue;
   }

   public double getMinValue() {
      return this.dynamicMin != null ? (Double)this.dynamicMin.get() : this.minValue;
   }

   public double getDefaultValue() {
      return this.defaultValue;
   }

   public Double getValue() {
      if (this.dynamicMax != null) {
         double max = (Double)this.dynamicMax.get();
         if (this.value > max) {
            this.value = max;
         }
      }

      if (this.dynamicMin != null) {
         double min = (Double)this.dynamicMin.get();
         if (this.value < min) {
            this.value = min;
         }
      }

      return this.value;
   }

   public void setValue(double value) {
      value = clamp(value, this.getMinValue(), this.getMaxValue());
      value = (double)Math.round(value * ((double)1.0F / this.increment)) / ((double)1.0F / this.increment);
      this.value = value;
   }

   public double doubleValue() {
      return this.getValue();
   }

   public double getIncrement() {
      return this.increment;
   }

   public Double getConfigValue() {
      return this.getValue();
   }
}
