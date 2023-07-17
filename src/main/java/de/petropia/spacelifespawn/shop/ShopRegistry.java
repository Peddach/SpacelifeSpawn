package de.petropia.spacelifespawn.shop;

import de.petropia.spacelifespawn.SpacelifeSpawn;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Register new shops and load already existing ones
 */
public class ShopRegistry {

    private static final List<Shop> SHOPS = new ArrayList<>();

    public static void registerShop(Shop shop){
        SHOPS.add(shop);
        shop.onLoad();
        SpacelifeSpawn.getInstance().getLogger().info("Registered Shop: " + shop.getShopID());
    }
    public static List<Shop> getShops(){
        return new ArrayList<>(SHOPS);
    }

    public static Shop getShopByID(String id){
        for(Shop shop : SHOPS){
            if(!shop.getShopID().equals(id)){
                continue;
            }
            return shop;
        }
        return null;
    }

    public static void tickShops() {
        Bukkit.getScheduler().runTaskTimer(SpacelifeSpawn.getInstance(), () -> {
            Instant now = Instant.now();
            SHOPS.forEach(shop -> {
                shop.updateSigns();
                if(!shop.isRented()){
                    return;
                }
                if(shop.getRentedUntil() < now.getEpochSecond()){
                    shop.unrent();
                }
            });
        }, 200, 20);
    }
}
