package de.petropia.spacelifespawn.portal;

import de.petropia.spacelifespawn.SpacelifeSpawn;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class ServerPortal implements Listener {

    private static final List<Player> blockList = new ArrayList<>();

    private final Consumer<Player> playerInPortalConsumer;
    private final int x1, z1, x2, z2;

    public ServerPortal(int x1, int z1, int x2, int z2, Consumer<Player> playerInPortalConsumer){
        this.playerInPortalConsumer = playerInPortalConsumer;
        this.x1 = x1;
        this.x2 = x2;
        this.z1 = z1;
        this.z2 = z2;
        Bukkit.getPluginManager().registerEvents(this, SpacelifeSpawn.getInstance());
    }

    @EventHandler
    public void onPortalEnterEvent(EntityPortalEnterEvent event){
        if(event.getEntity().getType() != EntityType.PLAYER){
            return;
        }
        Player player = (Player) event.getEntity();
        if(!checkIsInBounds(player.getLocation())){
            return;
        }
        if(blockList.contains(player)){
            return;
        }
        playerInPortalConsumer.accept(player);
        blockList.add(player);
    }

    private boolean checkIsInBounds(Location location){
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= Math.min(x1, x2) && x <= Math.max(x1, x2)
                && z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent event){
        Bukkit.getScheduler().runTaskLater(SpacelifeSpawn.getInstance(), () -> blockList.remove((Player) event.getPlayer()), 20);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        blockList.remove(event.getPlayer());
    }

    public static GuiAction<InventoryCloseEvent> getCloseAction(){
        return event -> event.getPlayer().setVelocity(new Vector(new Random().nextDouble(0, 2.2) - 1, 1, new Random().nextDouble(0, 2.2) - 1));
    }

    public static GuiItem createNoServerFound(){
        return ItemBuilder.from(Material.BARRIER)
                .name(Component.text("Kein Server erreichbar :(", NamedTextColor.RED).decorate(TextDecoration.BOLD))
                .lore(
                        Component.empty(),
                        Component.text("Versuche es später erneut oder informiere das Team", NamedTextColor.GRAY),
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem();
    }

    public static GuiItem createLeaveItem(){
        return ItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
                .name(Component.text("Verlassen", NamedTextColor.RED))
                .lore(
                        Component.empty()
                )
                .asGuiItem(event -> event.getWhoClicked().closeInventory());
    }
}
