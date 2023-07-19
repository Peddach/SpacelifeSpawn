package de.petropia.spacelifespawn.spawnprotection;

import de.petropia.spacelifeCore.teleport.StaticTeleportPoints;
import de.petropia.spacelifespawn.shop.Shop;
import de.petropia.spacelifespawn.shop.ShopRegistry;
import io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class SpawnProtectionListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        if(isPlayerPermittedToEdit(event.getBlock().getLocation(), event.getPlayer())){
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void playerInteractlistener(PlayerInteractEvent event){
        Material material = event.getPlayer().getInventory().getItemInMainHand().getType();
        List<Material> forbiddenMats = List.of(
          Material.ARMOR_STAND,
          Material.PAINTING,
          Material.ITEM_FRAME,
          Material.GLOW_ITEM_FRAME,
          Material.MINECART,
          Material.HOPPER_MINECART,
          Material.TNT_MINECART,
          Material.FURNACE_MINECART,
          Material.CHEST_MINECART
        );
        if(forbiddenMats.contains(material)){
            event.setCancelled(true);
        }
        if(event.isBlockInHand()){ //event is Blockplacement and should be handled by the BlockPlaceEvent
            return;
        }
        if(event.getInteractionPoint() == null){
            return;
        }
        if(isPlayerPermittedToEdit(event.getInteractionPoint(), event.getPlayer())){
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event){
        event.setCancelled(true);
        if(!(event.getEntity() instanceof Player player)){
            return;
        }
        if(event.getCause().equals(EntityDamageEvent.DamageCause.VOID)){
            player.teleport(StaticTeleportPoints.SPAWN.convertToBukkitLocation());
        }
    }

    @EventHandler
    public void onWaterPalce(PlayerBucketEmptyEvent event){
        if(isPlayerPermittedToEdit(event.getBlock().getLocation(), event.getPlayer())){
            return;
        }
        event.setCancelled(true);
    }
    @EventHandler
    public void onWaterPalce(PlayerBucketFillEvent event){
        if(isPlayerPermittedToEdit(event.getBlock().getLocation(), event.getPlayer())){
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void manipulate(PlayerArmorStandManipulateEvent e)
    {
        if(!e.getRightClicked().isVisible())
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFlowerPot(PlayerFlowerPotManipulateEvent event){
        if(isPlayerPermittedToEdit(event.getFlowerpot().getLocation(), event.getPlayer())){
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPistonEvent(BlockPistonExtendEvent event){
        event.setCancelled(true);
    }
    @EventHandler
    public void onPistonEvent(BlockPistonRetractEvent event){
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
