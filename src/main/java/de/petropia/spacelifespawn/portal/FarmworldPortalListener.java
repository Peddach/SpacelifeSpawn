package de.petropia.spacelifespawn.portal;

import de.petropia.spacelifespawn.SpacelifeSpawn;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

import java.util.ArrayList;
import java.util.List;

public class FarmworldPortalListener implements Listener {

    private static final int x1 = getIntFromConfig("x1");
    private static final int z1 = getIntFromConfig("z1");
    private static final int x2 = getIntFromConfig("x2");
    private static final int z2 = getIntFromConfig("z2");
    private static final List<Player> blockList = new ArrayList<>();
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
        new FarmworldSelectGUI(player);
        blockList.add(player);
    }

    private boolean checkIsInBounds(Location location){
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= Math.min(x1, x2) && x <= Math.max(x1, x2)
                && z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
    }

    private static int getIntFromConfig(String name){
        return SpacelifeSpawn.getInstance().getConfig().getInt("Farmwelt." + name);
    }

    public static List<Player> getBlockList(){
        return blockList;
    }
}
