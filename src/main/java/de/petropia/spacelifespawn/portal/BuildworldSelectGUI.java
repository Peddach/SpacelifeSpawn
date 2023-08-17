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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class BuildworldSelectGUI {

    private final Player player;
    private final Gui gui;

    public BuildworldSelectGUI(Player player) {
        this.player = player;
        gui = Gui.gui()
                .title(Component.text("Bauwelt auswählen", NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD))
                .rows(3)
                .disableAllInteractions()
                .create();
        gui.setCloseGuiAction(ServerPortal.getCloseAction());
        fillGui();
    }

    private void fillGui(){
        String taskName = SpacelifeSpawn.getInstance().getConfig().getString("BauweltTask");
        if(taskName == null){
            SpacelifeSpawn.getInstance().getLogger().warning("No Task in config defined for bauwelt!!!!");
            return;
        }
        SpacelifeSpawn.getInstance().getCloudNetAdapter().cloudServiceProviderInstance().servicesByTaskAsync(taskName)
                .thenAccept(serviceInfoSnapshots -> {
                    final List<ServiceInfoSnapshot> services = serviceInfoSnapshots.stream().filter(serviceInfoSnapshot -> serviceInfoSnapshot.readProperty(BridgeDocProperties.IS_ONLINE)).toList();
                    HashMap<String, BuildworldStatusDTO> worldStatus = new HashMap<>();
                    services.forEach(service -> {
                        ChannelMessage response = ChannelMessage.builder()
                                .channel("bauworld_status")
                                .message("world_status")
                                .target(ChannelMessageTarget.Type.SERVICE, service.name())
                                .build()
                                .sendSingleQuery();
                        DataBuf dataBuf = response.content();
                        BuildworldStatusDTO dto = dataBuf.readObject(BuildworldStatusDTO.class);
                        worldStatus.put(response.sender().name(), dto);
                    });
                    Bukkit.getScheduler().runTask(SpacelifeSpawn.getInstance(), () -> {
                        if(services.size() == 0){
                            gui.setItem(13, ServerPortal.createNoServerFound());
                            gui.setItem(8, ServerPortal.createLeaveItem());
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
                            gui.setItem(slots[i], createServerItem(services.get(i), i + 1, worldStatus.get(services.get(i).name()).plotCount()));
                        }
                        gui.setItem(8, ServerPortal.createLeaveItem());
                        gui.open(player);
                    });
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private GuiItem createServerItem(ServiceInfoSnapshot service, int number, int plotCount) {
        return ItemBuilder.skull()
                .texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjEyYTAzYTRjMTFiNGQ0NzI0NzJlN2U0NTkzZDJlMTI2YTYyNTllMzNjYzgxZjQ0ZWIwNWNmMDQyZDA3Njk2NyJ9fX0=")
                .name(Component.text("Bauwelt-" + number, NamedTextColor.GRAY).decorate(TextDecoration.BOLD))
                .lore(
                        Component.empty(),
                        Component.text("Spielerzahl: ", NamedTextColor.GRAY).append(Component.text(service.readProperty(BridgeDocProperties.ONLINE_COUNT), NamedTextColor.GOLD)),
                        Component.text("Grundstücke: ", NamedTextColor.GRAY).append(Component.text(plotCount).color(NamedTextColor.GOLD)),
                        Component.empty(),
                        Component.text("Linksklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Farmwelt betreten", NamedTextColor.GREEN)),
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(event -> {
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
}
