package de.petropia.spacelifespawn.jobs;

import com.github.juliarn.npclib.api.event.InteractNpcEvent;
import com.github.juliarn.npclib.api.protocol.enums.EntityAnimation;
import de.petropia.spacelifeCore.player.JobStats;
import de.petropia.spacelifeCore.player.SpacelifeDatabase;
import de.petropia.spacelifeCore.player.SpacelifePlayer;
import de.petropia.spacelifespawn.SpacelifeSpawn;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class JobNPCListener {

    private static final List<Player> blackList = new ArrayList<>();

    public void onNpcClick(InteractNpcEvent event){
        Bukkit.getScheduler().runTask(SpacelifeSpawn.getInstance(), () -> {
            JobNPC npc = JobNPC.getJobNPCS().stream().filter(n -> n.getNpc().equals(event.npc())).findFirst().orElse(null);
            if(npc == null){
                return;
            }
            Player player = event.player();
            if(blackList.contains(player)){
                return;
            }
            blackList.add(player);
            Bukkit.getScheduler().runTaskLater(SpacelifeSpawn.getInstance(), () -> blackList.remove(player), 20);
            SpacelifePlayer spacelifePlayer = SpacelifeDatabase.getInstance().getCachedPlayer(player.getUniqueId());
            JobStats jobStats = spacelifePlayer.getJobStats().get(npc.getJobID());
            if(jobStats == null){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Du hast diesen Beruf noch nie ausgeführt!"));
                return;
            }
            double money = jobStats.getCurrentMoney();
            if(money <= 0){
                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Du hast kein Geld zum Auszahlen!", NamedTextColor.RED));
                return;
            }
            spacelifePlayer.addMoney(money);
            spacelifePlayer.getJobStats().get(npc.getJobID()).resetCurrentMoney();
            moneyGainAminmation(money, player);
            for(int i = 0; i < 40; i += 5){
                EntityAnimation animation;
                if(i % 10 == 0){
                    animation = EntityAnimation.SWING_OFF_HAND;
                } else {
                    animation = EntityAnimation.SWING_MAIN_ARM;
                }
                Bukkit.getScheduler().runTaskLater(SpacelifeSpawn.getInstance(), () -> event.npc().platform().packetFactory().createAnimationPacket(animation).schedule(player, event.npc()), i);
            }
        });
    }

    private void moneyGainAminmation(double amount, Player player){
        int tickDuration = 40;
        int steps = tickDuration / 2;
        double currentAmount = 0;
        DecimalFormat format = new DecimalFormat("#.##");
        for(int i = 1; i < tickDuration; i += 2){
            currentAmount = currentAmount + (amount / steps);
            double finalCurrentAmount = currentAmount;
            int finalI = i;
            Bukkit.getScheduler().runTaskLater(SpacelifeSpawn.getInstance(), () -> {
                player.showTitle(Title.title(
                        Component.empty(),
                        Component.text(format.format(finalCurrentAmount) + "$",NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                        Title.Times.times(
                                Duration.ZERO,
                                Duration.ofMillis(50*4),
                                Duration.ZERO
                        )));
                player.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_BIT, Sound.Source.MASTER, 1F, finalI * 0.0375F));
            }, i);
        }
        Bukkit.getScheduler().runTaskLater(SpacelifeSpawn.getInstance(), () -> player.showTitle(Title.title(
                Component.empty(),
                Component.text(format.format(amount) + "$",NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                Title.Times.times(
                        Duration.ZERO,
                        Duration.ofSeconds(1),
                        Duration.ofMillis(300)
                ))), tickDuration + 2);
    }
}
