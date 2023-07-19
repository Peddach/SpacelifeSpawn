package de.petropia.spacelifespawn.shop;

import com.destroystokyo.paper.profile.ProfileProperty;
import de.petropia.spacelifeCore.player.SpacelifeDatabase;
import de.petropia.spacelifeCore.player.SpacelifePlayer;
import de.petropia.spacelifeCore.teleport.CrossServerLocation;
import de.petropia.spacelifeCore.warp.Warp;
import de.petropia.spacelifespawn.SpacelifeSpawn;
import de.petropia.turtleServer.api.chatInput.ChatInputBuilder;
import de.petropia.turtleServer.server.TurtleServer;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopRentListener implements Listener {

    private static final List<Shop> shopsInRentingProcess = new ArrayList<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onSignClick(PlayerInteractEvent event){
        if(!event.getAction().isRightClick()){
            return;
        }
        if(event.getClickedBlock() == null){
            return;
        }
        Block block = event.getClickedBlock();
        if(!(block.getState() instanceof Sign)){
            return;
        }
        for(Shop shop : ShopRegistry.getShops()){
            if(!block.getLocation().equals(shop.getSignLocation())){
                continue;
            }
            if(shop.isRented()){
                return;
            }
            if(shopsInRentingProcess.contains(shop)){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(event.getPlayer(), Component.text("Jemand ist grade dabei diesen Shop zu mieten!", NamedTextColor.RED));
                return;
            }
            if(!isPlayerAbleToRent(event.getPlayer())){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(event.getPlayer(), Component.text("Du darfst nicht mehr als einen Shop besitzen!", NamedTextColor.RED));
                return;
            }
            SpacelifePlayer spacelifePlayer = SpacelifeDatabase.getInstance().getCachedPlayer(event.getPlayer().getUniqueId());
            int shopCost = SpacelifeSpawn.getInstance().getConfig().getInt("shopCost");
            if(spacelifePlayer.getMoney() < shopCost){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(event.getPlayer(), Component.text("Du hast nicht ausreichend Geld! Du benötigst mindestens " + shopCost + "$", NamedTextColor.DARK_RED));
                return;
            }
            shopsInRentingProcess.add(shop);
            event.getPlayer().playSound(Sound.sound(org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.MASTER, 1.2F, 1.2F));
            new ChatInputBuilder(Component.text("Bitte stelle dich vor oder in den Shop, wo dein Warp später sein soll und gib anschließend den Namen des Shops in den Chat ein! Der Name sollte nicht länger als 10 Zeichen sein, ein Wort sein und nur Buchstaben und Zahlen enthalten!", NamedTextColor.GREEN), event.getPlayer())
                    .onCancel(() -> {
                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(event.getPlayer(), Component.text("Miete wurde abgebrochen! Dir wurde kein Geld berechnet.", NamedTextColor.RED));
                        shopsInRentingProcess.remove(shop);
                    }
                    ).onInputWithString(str -> {
                        if(!str.matches("^[a-zA-Z0-9]{1,10}$")){
                            SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(event.getPlayer(), Component.text("Ungültiger Name!", NamedTextColor.RED));
                            shopsInRentingProcess.remove(shop);
                            return;
                        }
                        for(Warp warp : SpacelifeDatabase.getInstance().getWarps()){
                            if(warp.getName().equalsIgnoreCase(str)){
                                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(event.getPlayer(), Component.text("Der Warp existiert bereits!", NamedTextColor.RED));
                                shopsInRentingProcess.remove(shop);
                                return;
                            }
                        }
                        if(shop.isRented()){
                            SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(event.getPlayer(), Component.text("Dieser Shop ist bereits vermietet!", NamedTextColor.RED));
                            shopsInRentingProcess.remove(shop);
                            return;
                        }
                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(event.getPlayer(), Component.text("Du hast den Shop " + shop.getShopID() + " erfolgreich gemietet!", NamedTextColor.DARK_GREEN));
                        if(!spacelifePlayer.subtractMoney(shopCost)){
                            SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(event.getPlayer(), Component.text("Du hast nicht genug Geld!", NamedTextColor.RED));
                            shopsInRentingProcess.remove(shop);
                            return;
                        }
                        shopsInRentingProcess.remove(shop);
                        event.getPlayer().playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.Source.MASTER, 1.2F, 1.2F));
                        shop.rent(event.getPlayer(), str);
                        //warp stuff
                        String ownerUUID = event.getPlayer().getUniqueId().toString();
                        String ownerName = event.getPlayer().getName();
                        String ownerSkin = null;
                        for(ProfileProperty profileProperty : event.getPlayer().getPlayerProfile().getProperties()){
                            if(!profileProperty.getName().equalsIgnoreCase("textures")){
                                continue;
                            }
                            ownerSkin = profileProperty.getValue();
                            break;
                        }
                        if(ownerSkin == null){
                            return;
                        }
                        CrossServerLocation location = new CrossServerLocation(TurtleServer.getInstance().getCloudNetAdapter().getServerInstanceName(), event.getPlayer().getLocation());
                        Warp warp = new Warp(str, ownerUUID, ownerSkin, ownerName, location, Date.from(Instant.ofEpochSecond(shop.getRentedUntil())));
                        SpacelifeDatabase.getInstance().addWarp(warp);
                    }).build();
        }
    }

    private boolean isPlayerAbleToRent(Player player){
        UUID uuid = player.getUniqueId();
        for(Shop shop : ShopRegistry.getShops()){
            if(!shop.isRented()){
                continue;
            }
            if(shop.getOwner().equals(uuid)){
                return false;
            }
        }
        return true;
    }
}
