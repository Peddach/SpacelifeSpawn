package de.petropia.spacelifespawn;

import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.event.InteractNpcEvent;
import com.github.juliarn.npclib.api.event.ShowNpcEvent;
import com.github.juliarn.npclib.bukkit.BukkitPlatform;
import com.github.juliarn.npclib.bukkit.BukkitVersionAccessor;
import com.github.juliarn.npclib.bukkit.BukkitWorldAccessor;
import de.petropia.spacelifespawn.database.ShopDatabase;
import de.petropia.spacelifespawn.database.commands.ShopCommand;
import de.petropia.spacelifespawn.shop.Shop;
import de.petropia.spacelifespawn.shop.ShopRegistry;
import de.petropia.spacelifespawn.shop.ShopRentListener;
import de.petropia.spacelifespawn.shop.gui.ShopNpcListener;
import de.petropia.spacelifespawn.spawnprotection.SpawnProtectionListener;
import de.petropia.turtleServer.api.PetropiaPlugin;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class SpacelifeSpawn extends PetropiaPlugin {
    private static SpacelifeSpawn instance;

    private static Platform<World, Player, ItemStack, Plugin> npcPlatform;

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
        getServer().getPluginManager().registerEvents(new ShopNpcListener(), this);
        this.getCommand("shop").setExecutor(new ShopCommand());
        this.getCommand("shop").setTabCompleter(new ShopCommand());
        npcPlatform = BukkitPlatform.bukkitNpcPlatformBuilder()
                .extension(this)
                .debug(true)
                .actionController(builder ->builder
                        .flag(NpcActionController.TAB_REMOVAL_TICKS, 10)
                        .flag(NpcActionController.IMITATE_DISTANCE, 20))
                .versionAccessor(BukkitVersionAccessor.versionAccessor())
                .worldAccessor(BukkitWorldAccessor.nameBasedAccessor())
                .build();
        npcPlatform.eventBus().subscribe(ShowNpcEvent.Post.class, event -> new ShopNpcListener().handleNpcShow(event));
        npcPlatform.eventBus().subscribe(InteractNpcEvent.class, event -> new ShopNpcListener().handlNpcClick(event));
    }

    @Override
    public void onDisable(){
        ShopRegistry.getShops().forEach(Shop::onDisable);
    }

    private void loadShops() {
        ShopDatabase.getInstance().loadAllShops().forEach(ShopRegistry::registerShop);
        ShopRegistry.tickShops();
    }

    public static SpacelifeSpawn getInstance() {
        return instance;
    }

    public static Platform<World, Player, ItemStack, Plugin> getNpcPlatform() {
        return npcPlatform;
    }
}
