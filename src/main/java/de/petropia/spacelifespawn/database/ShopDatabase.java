package de.petropia.spacelifespawn.database;

import de.petropia.spacelifeCore.player.SpacelifeDatabase;
import de.petropia.spacelifespawn.SpacelifeSpawn;
import de.petropia.spacelifespawn.shop.Shop;
import dev.morphia.Datastore;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.stream.Collectors;

public class ShopDatabase {

    private static ShopDatabase instance;
    private Datastore datastore;

    public ShopDatabase(){
        if(instance != null){
            return;
        }
        instance = this;
        datastore = SpacelifeDatabase.getInstance().getDatastore();
        datastore.getMapper().map(Shop.class);
        datastore.ensureIndexes();
    }

    public static ShopDatabase getInstance() {
        return instance;
    }

    public void saveShop(Shop shop){
        final Shop clonedShop = shop.clone(); //Thread safty
        Bukkit.getServer().getScheduler().runTaskAsynchronously(SpacelifeSpawn.getInstance(), () -> datastore.save(clonedShop));
    }

    /**
     * Loads all Shops from Database in Sync
     * @return List of all Shops
     */
    public List<Shop> loadAllShops(){
        return datastore.find(Shop.class).stream().collect(Collectors.toList());
    }
}
