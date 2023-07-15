package de.petropia.spacelifespawn;

import de.petropia.spacelifespawn.database.ShopDatabase;
import de.petropia.spacelifespawn.database.commands.ShopCommand;
import de.petropia.spacelifespawn.shop.ShopRegistry;
import de.petropia.spacelifespawn.shop.ShopRentListener;
import de.petropia.spacelifespawn.spawnprotection.SpawnProtectionListener;
import de.petropia.turtleServer.api.PetropiaPlugin;

public class SpacelifeSpawn extends PetropiaPlugin {


    private static SpacelifeSpawn instance;

    @Override
    public void onEnable() {
        super.onEnable();
        saveDefaultConfig();
        saveConfig();
        reloadConfig();
        instance = this;
        new ShopDatabase();
        loadShops();
        getServer().getPluginManager().registerEvents(new SpawnProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new ShopRentListener(), this);
        this.getCommand("shop").setExecutor(new ShopCommand());
        this.getCommand("shop").setTabCompleter(new ShopCommand());
    }

    private void loadShops() {
        ShopDatabase.getInstance().loadAllShops().forEach(ShopRegistry::registerShop);
        ShopRegistry.tickShops();
    }

    public static SpacelifeSpawn getInstance() {
        return instance;
    }
}
