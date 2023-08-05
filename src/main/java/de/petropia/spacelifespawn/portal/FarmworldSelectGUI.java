package de.petropia.spacelifespawn.portal;

import de.petropia.spacelifeCore.SpacelifeCore;
import de.petropia.spacelifeCore.player.SpacelifeDatabase;
import de.petropia.spacelifeCore.player.SpacelifePlayer;
import de.petropia.spacelifeCore.player.SpacelifePlayerLoadingListener;
import de.petropia.spacelifeCore.teleport.BlockAnyActionListener;
import de.petropia.spacelifespawn.SpacelifeSpawn;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.util.Vector;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FarmworldSelectGUI {

    private final Player player;
    private final Gui gui;

    public FarmworldSelectGUI(Player player){
        this.player = player;
        this.gui = Gui.gui()
                .title(Component.text("Farmwelt auswählen", NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD))
                .rows(3)
                .disableAllInteractions()
                .create();
        gui.setCloseGuiAction(event -> {
            Bukkit.getScheduler().runTaskLater(SpacelifeSpawn.getInstance(), () -> FarmworldPortalListener.getBlockList().remove((Player) event.getPlayer()), 20);
            event.getPlayer().setVelocity(new Vector(new Random().nextDouble(0, 2.2) - 1, 1, new Random().nextDouble(0, 2.2) - 1));
        });
        fillGui();
    }

    private void fillGui(){
        String taskName = SpacelifeSpawn.getInstance().getConfig().getString("FarmweltTask");
        if(taskName == null){
            SpacelifeSpawn.getInstance().getLogger().warning("No Task in config defined for farmworld!!!!");
            return;
        }
        SpacelifeSpawn.getInstance().getCloudNetAdapter().cloudServiceProviderInstance().servicesByTaskAsync(taskName)
                .thenAccept(serviceInfoSnapshots -> {
                    final List<ServiceInfoSnapshot> services = serviceInfoSnapshots.stream().filter(serviceInfoSnapshot -> serviceInfoSnapshot.readProperty(BridgeDocProperties.IS_ONLINE)).toList();
                    Map<String, Boolean> serviceStatus = new HashMap<>();
                    Map<String, Long> serviceNextDelete = new HashMap<>();
                    services.forEach(service -> {
                        ChannelMessage response = ChannelMessage.builder()
                                .channel("farmworld_status")
                                .message("world_status")
                                .target(ChannelMessageTarget.Type.SERVICE, service.name())
                                .build()
                                .sendSingleQuery();
                        DataBuf dataBuf = response.content();
                        serviceStatus.put(service.name(),dataBuf.readBoolean());
                        serviceNextDelete.put(service.name(), dataBuf.readLong());
                    });
                    Bukkit.getScheduler().runTask(SpacelifeSpawn.getInstance(), () -> {
                        if(services.size() == 0){
                            gui.setItem(13, createNoServerFound());
                            gui.setItem(8, createLeaveItem());
                            gui.open(player);
                            return;
                        }
                        int[] slots;
                        if(services.size() == 1) slots = new int[]{13};
                        else if(services.size() == 2) slots = new int[]{11, 15};
                        else if(services.size() == 3) slots = new int[]{11, 13, 15};
                        else if(services.size() == 4) slots = new int[]{10, 12, 14, 16};
                        else slots = new int[] {8, 10, 11, 12, 13, 14, 15, 16, 17};
                        for (int i = 0; i < services.size() && i < slots.length; i++) {
                            gui.setItem(slots[i], createServerItem(services.get(i), i + 1, serviceStatus.get(services.get(i).name()), serviceNextDelete.get(services.get(i).name())));
                        }
                        gui.setItem(8, createLeaveItem());
                        gui.open(player);
                    });
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private GuiItem createServerItem(ServiceInfoSnapshot service, int number, boolean status, long nextDelete) {
        return ItemBuilder.skull()
                .texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDM4Y2YzZjhlNTRhZmMzYjNmOTFkMjBhNDlmMzI0ZGNhMTQ4NjAwN2ZlNTQ1Mzk5MDU1NTI0YzE3OTQxZjRkYyJ9fX0=")
                .name(Component.text("Farmwelt-" + number, NamedTextColor.GRAY).decorate(TextDecoration.BOLD))
                .lore(
                        Component.empty(),
                        Component.text("Spielerzahl: ", NamedTextColor.GRAY).append(Component.text(service.readProperty(BridgeDocProperties.ONLINE_COUNT), NamedTextColor.GOLD)),
                        Component.text("Version: ", NamedTextColor.GRAY).append(Component.text(formatVersion(service.readProperty(BridgeDocProperties.VERSION)), NamedTextColor.GOLD)),
                        Component.text("Status: ", NamedTextColor.GRAY).append(formatIsOnline(status)),
                        Component.text("Nächster Reset: ", NamedTextColor.GRAY).append(formatNextReset(nextDelete)),
                        Component.empty(),
                        Component.text("Linksklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Farmwelt betreten", NamedTextColor.GREEN)),
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(event -> {
                    if(!status){
                        player.playSound(Sound.sound(org.bukkit.Sound.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1F, 1F));
                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Du kannst der Farmwelt erst beitreten wenn sie resettet ist. Versuche es später erneut", NamedTextColor.RED));
                        return;
                    }
                    SpacelifePlayer spacelifePlayer = SpacelifeDatabase.getInstance().getCachedPlayer(player.getUniqueId());
                    BlockAnyActionListener.blockPlayer(player);
                    SpacelifePlayerLoadingListener.blockInvSave(player);
                    spacelifePlayer.saveInventory().thenAccept(v -> {
                        Bukkit.getScheduler().runTaskLater(SpacelifeCore.getInstance(), () -> SpacelifeCore.getInstance().getCloudNetAdapter().sendPlayerToServer(player, service.name()), 10);
                        Bukkit.getScheduler().runTaskLater(SpacelifeCore.getInstance(), () -> {
                            if (player.isOnline()) {
                                player.kick(Component.text("Etwas ist schief gelaufen!", NamedTextColor.RED));
                            }
                        }, 3 * 20);
                    }).exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });
                });

    }

    private Component formatNextReset(long nextDelete) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Berlin"));
        Instant instant = Instant.ofEpochSecond(nextDelete);
        return Component.text(formatter.format(instant), NamedTextColor.GOLD);
    }

    private GuiItem createNoServerFound(){
        return ItemBuilder.from(Material.BARRIER)
                .name(Component.text("Keine Farmwelt online :(", NamedTextColor.RED).decorate(TextDecoration.BOLD))
                .lore(
                        Component.empty(),
                        Component.text("Versuche es später erneut oder informiere das Team", NamedTextColor.GRAY),
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem();
    }

    private GuiItem createLeaveItem(){
        return ItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
                .name(Component.text("Verlassen", NamedTextColor.RED))
                .lore(
                        Component.empty()
                )
                .asGuiItem(event -> event.getWhoClicked().closeInventory());
    }

    private String formatVersion(String rawVersion){
        if(rawVersion == null){
            return "Unbekannt";
        }
        Pattern pattern = Pattern.compile("\\b\\d+\\.\\d+(\\.\\d+)?\\b");
        Matcher matcher = pattern.matcher(rawVersion);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return "Unbekannt";
        }
    }

    private Component formatIsOnline(boolean status){
        return status ? Component.text("online", NamedTextColor.GREEN) : Component.text("Wird resettet", NamedTextColor.RED);
    }
}
