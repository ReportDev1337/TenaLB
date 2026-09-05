package dev.tenacity.module;

import dev.tenacity.utils.objects.Drag;
import dev.tenacity.utils.objects.Scroll;
import lombok.Generated;

public enum Category {
   COMBAT("Combat", "c"),
   MOVEMENT("Movement", "f"),
   RENDER("Render", "d"),
   PLAYER("Player", "e"),
   EXPLOIT("Exploit", "a"),
   MISC("Misc", "b"),
   SCRIPTS("Scripts", "g");

   public final String name;
   public final String icon;
   public final int posX;
   public final boolean expanded;
   private final Scroll scroll = new Scroll();
   private final Drag drag;
   public int posY = 20;

   private Category(String name, String icon) {
      this.name = name;
      this.icon = icon;
      this.posX = 20 + Module.categoryCount * 120;
      this.drag = new Drag((float)this.posX, (float)this.posY);
      this.expanded = true;
      ++Module.categoryCount;
   }

   @Generated
   public Scroll getScroll() {
      return this.scroll;
   }

   @Generated
   public Drag getDrag() {
      return this.drag;
   }
}
