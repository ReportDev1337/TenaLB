package dev.tenacity.utils.server;

import dev.tenacity.utils.Utils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

public class ServerUtils implements Utils {
   public static ServerData lastServer;
   private static boolean redirecting;

   public static boolean serverCheck(String ip) {
      if (mc.getCurrentServerData() == null) {
         return false;
      } else {
         ip = ip.toLowerCase();
         String server = mc.isSingleplayer() ? "" : mc.getCurrentServerData().serverIP.toLowerCase();
         return server.endsWith("." + ip) || server.equals(ip);
      }
   }

   public static boolean isGeniuneHypixel() {
      return isOnHypixel() && !redirecting;
   }

   public static boolean isOnHypixel() {
      if (!mc.isSingleplayer() && mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
         String ip = mc.getCurrentServerData().serverIP.toLowerCase();
         if (ip.contains("hypixel")) {
            if (mc.thePlayer == null) {
               return true;
            } else {
               String brand = mc.thePlayer.getClientBrand();
               return brand != null && brand.startsWith("Hypixel BungeeCord");
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isInLobby() {
      if (mc.theWorld == null) {
         return true;
      } else {
         List<Entity> entities = mc.theWorld.getLoadedEntityList();

         for(int i = 0; i < entities.size(); ++i) {
            Entity entity = (Entity)entities.get(i);
            if (entity != null && entity.getName().equals("§e§lCLICK TO PLAY")) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean isHostsRedirectingHypixel() throws IOException {
      Path value = Paths.get(System.getenv("SystemDrive") + "\\Windows\\System32\\drivers\\etc\\hosts");
      if (Files.notExists(value, new LinkOption[0])) {
         return false;
      } else {
         BufferedReader reader = new BufferedReader(new FileReader(value.toFile()));
         Throwable var2 = null;

         try {
            String line;
            while((line = reader.readLine()) != null) {
               if (line.toLowerCase().contains("hypixel")) {
                  boolean var4 = true;
                  return var4;
               }
            }

            return false;
         } catch (Throwable var14) {
            var2 = var14;
            throw var14;
         } finally {
            if (reader != null) {
               if (var2 != null) {
                  try {
                     reader.close();
                  } catch (Throwable var13) {
                     var2.addSuppressed(var13);
                  }
               } else {
                  reader.close();
               }
            }

         }
      }
   }

   public static void updateRedirecting() throws IOException {
      redirecting = isHostsRedirectingHypixel();
   }

   public static boolean isOnSameTeam(EntityLivingBase ent) {
      if (mc.thePlayer != null && ent != null && mc.theWorld != null) {
         Scoreboard scoreboard = mc.theWorld.getScoreboard();
         ScorePlayerTeam myTeam = scoreboard.getPlayersTeam(mc.thePlayer.getName());
         ScorePlayerTeam theirTeam = scoreboard.getPlayersTeam(ent.getName());
         if (myTeam != null && theirTeam != null) {
            return myTeam.getRegisteredName().equals(theirTeam.getRegisteredName());
         } else {
            String myDisplay = mc.thePlayer.getDisplayName().getFormattedText();
            String theirDisplay = ent.getDisplayName().getFormattedText();
            String myColor = getTeamColor(myDisplay, mc.thePlayer.getName());
            String theirColor = getTeamColor(theirDisplay, ent.getName());
            return !myColor.isEmpty() && myColor.equals(theirColor);
         }
      } else {
         return false;
      }
   }

   private static String getTeamColor(String formattedDisplay, String playerName) {
      int nameIndex = formattedDisplay.lastIndexOf(playerName);
      if (nameIndex < 2) {
         return "";
      } else {
         String before = formattedDisplay.substring(0, nameIndex);

         for(int i = before.length() - 2; i >= 0; --i) {
            if (before.charAt(i) == 167) {
               char code = before.charAt(i + 1);
               if (code >= '0' && code <= '9' || code >= 'a' && code <= 'f') {
                  return "§" + code;
               }
            }
         }

         return "";
      }
   }
}
