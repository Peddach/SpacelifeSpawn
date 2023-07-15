package de.petropia.spacelifespawn.spawnprotection;

import de.petropia.spacelifespawn.shop.Shop;
import de.petropia.spacelifespawn.shop.ShopRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityInteractEvent;

public class SpawnProtectionListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        if(isPlayerPermittedToEdit(event.getBlock().getLocation(), event.getPlayer())){
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        if(isPlayerPermittedToEdit(event.getBlock().getLocation(), event.getPlayer())){
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(EntityInteractEvent event){
        if (!(event instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        if(isPlayerPermittedToEdit(event.getBlock().getLocation(), player)){
            return;
        }
        event.setCancelled(true);
    }

    /**
     * Check if player is permitted to break a block
     * @param location Location of Block
     * @param player Player who wants to break
     * @return true if permitted, otherwise false
     */
    private boolean isPlayerPermittedToEdit(Location location, Player player){
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        for(Shop shop : ShopRegistry.getShops()){
            if(!shop.isRented()){
                continue;
            }
            if((shop.getOwner().equals(player.getUniqueId()) || shop.getTrustedPlayers().contains(player.getUniqueId())) && shop.isInShop(x, y, z)){
                return true;
            }
        }
        return false;
    }
}
