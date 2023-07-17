package de.petropia.spacelifespawn.database.commands;

import de.petropia.spacelifespawn.SpacelifeSpawn;
import de.petropia.spacelifespawn.database.ShopDatabase;
import de.petropia.spacelifespawn.shop.Shop;
import de.petropia.spacelifespawn.shop.ShopRegistry;
import de.petropia.spacelifespawn.shop.gui.ShopEditGui;
import de.petropia.turtleServer.api.util.MessageUtil;
import de.petropia.turtleServer.api.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)){
            return false;
        }
        if(args.length == 1 && args[0].equalsIgnoreCase("info")){
            if(!player.hasPermission("spacelifespawn.command.shop.info")){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Keine Rechte!", NamedTextColor.RED));
                return false;
            }
            Location loc = player.getLocation();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            for(Shop shop : ShopRegistry.getShops()){
                if(!shop.isInShop(x, y, z)){
                    continue;
                }
                MessageUtil msg = SpacelifeSpawn.getInstance().getMessageUtil();
                msg.sendMessage(player, Component.text("--- Shop info ---", NamedTextColor.GRAY));
                msg.sendMessage(player, getShopInfoMsg("ShopID: ", shop.getShopID()));
                msg.sendMessage(player, getShopInfoMsg("Shopname: ", shop.getName()));
                msg.sendMessage(player, getShopInfoMsg("Besitzer: ", shop.getOwnerName()));
                msg.sendMessage(player, getShopInfoMsg("BesitzerID: ", shop.getOwner()));
                msg.sendMessage(player, getShopInfoMsg("Gemietet: ", String.valueOf(shop.isRented())));
                msg.sendMessage(player, getShopInfoMsgWithDate("Gemietet bis: ", shop.getRentedUntil()));
                msg.sendMessage(player, Component.text("--- Shop info ---", NamedTextColor.GRAY));
                return true;
            }
            SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Du befindest dich in keinem Shop", NamedTextColor.RED));
            return false;
        }
        if(args.length == 1 && args[0].equalsIgnoreCase("edit")){
            if(!player.hasPermission("spacelifespawn.command.shop.edit")){
                return false;
            }
            for(Shop shop : ShopRegistry.getShops()){
                if(!shop.isRented()){
                    continue;
                }
                if(!shop.getOwner().equals(player.getUniqueId())){
                    continue;
                }
                Location location = player.getLocation();
                if(!shop.isInShop(location.getBlockX(), location.getBlockY(), location.getBlockZ())){
                    continue;
                }
                new ShopEditGui(player, shop);
            }
            SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Dieser Shop gehört nicht dir oder du bist kein Mitglied!", NamedTextColor.RED));
        }
        if(args.length >= 1 && args[0].equalsIgnoreCase("add")){
            if(!player.hasPermission("spacelifespawn.command.shop.add")){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Leider keine Rechte!", NamedTextColor.RED));
                return false;
            }
            if(args.length != 5){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Gib 4 argumente an: id Schild:x/y/z Ecke1:x/y/z Ecke2:x/y/z", NamedTextColor.RED));
                return false;
            }
            String id = args[1];
            String signLoc = args[2];
            String corner1Loc = args[3];
            String corner2Loc = args[4];
            String[] splitSignLoc = signLoc.split("/");
            if(splitSignLoc.length != 3){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Fehler bei der Schildkoordinate!", NamedTextColor.RED));
                return false;
            }
            int signX = Integer.parseInt(splitSignLoc[0]);
            int signY = Integer.parseInt(splitSignLoc[1]);
            int signZ = Integer.parseInt(splitSignLoc[2]);
            String[] splitCorner1Loc = corner1Loc.split("/");
            if(splitCorner1Loc.length != 3){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Fehler bei der Ecke1!", NamedTextColor.RED));
                return false;
            }
            int cor1X = Integer.parseInt(splitCorner1Loc[0]);
            int cor1Y = Integer.parseInt(splitCorner1Loc[1]);
            int cor1Z = Integer.parseInt(splitCorner1Loc[2]);
            String[] splitCorner2Loc = corner2Loc.split("/");
            if(splitCorner2Loc.length != 3){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Fehler bei der Ecke2!", NamedTextColor.RED));
                return false;
            }
            int cor2X = Integer.parseInt(splitCorner2Loc[0]);
            int cor2Y = Integer.parseInt(splitCorner2Loc[1]);
            int cor2Z = Integer.parseInt(splitCorner2Loc[2]);
            Shop shop = new Shop(id, signX, signY, signZ, cor1X, cor1Y, cor1Z, cor2X, cor2Y, cor2Z);
            ShopRegistry.registerShop(shop);
            ShopDatabase.getInstance().saveShop(shop);
            SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Shop registriert!", NamedTextColor.GREEN));
            return true;
        }
        if(args.length == 1 && args[0].equalsIgnoreCase("list")){
            if(!player.hasPermission("spacelifespawn.command.shop.list")){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Du hast keine Rechte dazu!", NamedTextColor.RED));
                return false;
            }
            for(Shop shop : ShopRegistry.getShops()){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player,
                        Component.text(shop.getShopID(), NamedTextColor.GOLD)
                        .append(Component.text(" - Vermietet: ", NamedTextColor.GRAY))
                                .append(Component.text(String.valueOf(shop.isRented()), NamedTextColor.GRAY)));
            }
            return true;
        }
        if(args.length >= 1 && args[0].equalsIgnoreCase("tp")){
            if(!player.hasPermission("spacelifespawn.command.shop.tp")){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Du hast dazu keine Rechte", NamedTextColor.RED));
                return false;
            }
            if(args.length != 2){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Bitte gib eine ShopID an"));
                return false;
            }
            String shopID = args[1];
            Shop shop = ShopRegistry.getShopByID(shopID);
            if(shop == null){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Dieser Shop existiert nicht!", NamedTextColor.RED));
                return false;
            }
            player.teleport(shop.getSignLocation());
            return true;
        }
        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Bitte gib ein Argument an!", NamedTextColor.RED));
        return false;
    }

    private Component getShopInfoMsg(String key, String value){
        if(value == null){
            value = "unbekannt";
        }
        return Component.text(key, NamedTextColor.GRAY).append(Component.text(value, NamedTextColor.GOLD));
    }

    private Component getShopInfoMsg(String key, UUID value){
        String strval;
        if(value == null){
            strval = "unbekannt";
        } else {
            strval = value.toString();
        }
        return Component.text(key, NamedTextColor.GRAY).append(Component.text(strval, NamedTextColor.GOLD));
    }
    private Component getShopInfoMsgWithDate(String key, double value){
        String strval;
        if(value == 0){
            strval = "unbekannt";
        } else {
            strval = TimeUtil.unixTimestampToString((int) value);
        }
        return Component.text(key, NamedTextColor.GRAY).append(Component.text(strval, NamedTextColor.GOLD));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) {
            return null;
        }
        if (args.length == 1) {
            if (player.hasPermission("spacelifespawn.command.shop.info")) {
                completions.add("info");
            }
            if (player.hasPermission("spacelifespawn.command.shop.add")) {
                completions.add("add");
            }
            if (player.hasPermission("spacelifespawn.command.shop.list")) {
                completions.add("list");
            }
            if(player.hasPermission("spacelifespawn.command.shop.tp")){
                completions.add("tp");
            }
            if(player.hasPermission("spacelifespawn.command.shop.edit")){
                completions.add("edit");
            }
        }
        if(args.length == 2 && args[0].equalsIgnoreCase("tp")){
            for(Shop shop : ShopRegistry.getShops()){
                completions.add(shop.getShopID());
            }
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], completions, new ArrayList<>());
    }
}
