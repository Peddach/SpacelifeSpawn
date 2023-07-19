package de.petropia.spacelifespawn.shop.gui;

import com.github.juliarn.npclib.api.event.InteractNpcEvent;
import com.github.juliarn.npclib.api.event.ShowNpcEvent;
import com.github.juliarn.npclib.api.protocol.enums.EntityAnimation;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import de.petropia.spacelifespawn.SpacelifeSpawn;
import de.petropia.spacelifespawn.shop.Shop;
import de.petropia.spacelifespawn.shop.ShopRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class ShopNpcListener implements Listener {

    public void handleNpcShow(ShowNpcEvent.Post event) {
        event.npc().platform().packetFactory().createEntityMetaPacket(EntityMetadataFactory.skinLayerMetaFactory(), true).schedule((Player) event.player(), event.npc());
        event.npc().platform().packetFactory().createAnimationPacket(EntityAnimation.SWING_MAIN_ARM).schedule((Player) event.player(), event.npc());
    }

    public void handlNpcClick(InteractNpcEvent event){
        Player player = event.player();
        Bukkit.getScheduler().runTask(SpacelifeSpawn.getInstance(), () -> {
            for(Shop shop : ShopRegistry.getShops()){
                if(!shop.isRented()){
                    continue;
                }
                if(shop.isInShop(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())){
                    new ShopBuyGui(player, shop);
                }
                return;
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // create a new scoreboard for the player if the player uses the main scoreboard
        ScoreboardManager manager = player.getServer().getScoreboardManager();
        if (player.getScoreboard().equals(manager.getMainScoreboard())) {
            player.setScoreboard(manager.getNewScoreboard());
        }
        // we have to register each entity to the players scoreboard
        for (Shop shop : ShopRegistry.getShops()) {
            if (!shop.isRented()) {
               continue;
            }
            registerScoreboardTeam(player.getScoreboard(), shop);
        }
    }

    public static void registerScoreboardTeam(Scoreboard scoreboard, Shop shop) {
        // check if a team for this entity is already created
        String teamName = "shopz";
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        // set the name tag visibility of the team
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        // register the spawned entity to the team
        if(!team.getEntries().contains(shop.getNPC().profile().name())){
            team.addEntries(shop.getNPC().profile().name());
        }

    }
}
