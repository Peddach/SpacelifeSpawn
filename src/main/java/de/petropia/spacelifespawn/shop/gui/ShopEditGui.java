package de.petropia.spacelifespawn.shop.gui;

import de.petropia.spacelifespawn.SpacelifeSpawn;
import de.petropia.spacelifespawn.shop.Shop;
import de.petropia.turtleServer.api.chatInput.ChatInputBuilder;
import de.petropia.turtleServer.server.TurtleServer;
import de.petropia.turtleServer.server.user.PetropiaPlayer;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ShopEditGui {

    private final Shop shop;
    private final Player player;
    private final Gui gui;

    public ShopEditGui(Player player, Shop shop) {
        this.shop = shop;
        this.player = player;
        gui = Gui.gui()
                .rows(3)
                .disableAllInteractions()
                .title(Component.text(shop.getName(), NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD))
                .create();
        gui.setItem(16, createShopDeleteItem());
        gui.setItem(10, createNpcLocationItem());
        gui.setItem(13, createAddPlayerItem());
        addTrustedPlayerHeads(gui);
    }

    private GuiItem createShopDeleteItem(){
        Component title = Component.text("Shop auflösen", NamedTextColor.RED).decorate(TextDecoration.BOLD);
        Component lore1 = Component.text("Klicke um deinen Shop aufzulösen und", NamedTextColor.DARK_RED);
        Component lore2 = Component.text("alles zu löschen. Du erhälst kein Geld", NamedTextColor.DARK_RED);
        Component lore3 = Component.text("zurück. Diese Aktion kann nicht rückgängig gemacht werden!", NamedTextColor.DARK_RED);
        return ItemBuilder.from(Material.BARRIER)
                .name(title)
                .lore(
                        Component.empty(),
                        lore1,
                        lore2,
                        lore3,
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(event -> {
                    if(!shop.getOwner().equals(event.getWhoClicked().getUniqueId())){
                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Nur der Besitzer kann den Shop auflösen!", NamedTextColor.DARK_RED));
                        return;
                    }
                    event.getWhoClicked().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    event.getWhoClicked().playSound(Sound.sound(org.bukkit.Sound.ENTITY_ENDER_DRAGON_DEATH, Sound.Source.MASTER, 1.2F, 0.8F));
                    SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Dein Shop wurde aufgelöst!", NamedTextColor.DARK_RED));
                    shop.unrent();
                });
    }
    private GuiItem createAddPlayerItem(){
        Component title = Component.text("Mitglied hinzufügen", NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        Component lore1 = Component.text("Klicke zum hinzufügen eines Spielers. Dieser", NamedTextColor.GRAY);
        Component lore2 = Component.text("kann dann ebenfalls den Shop umgestalten, aber", NamedTextColor.GRAY);
        Component lore3 = Component.text("kein Geld auszahlen.", NamedTextColor.GRAY);
        return ItemBuilder.from(Material.TOTEM_OF_UNDYING)
                .name(title)
                .lore(
                        Component.empty(),
                        lore1,
                        lore2,
                        lore3,
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(event -> {
                    event.getWhoClicked().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    if(!shop.getOwner().equals(event.getWhoClicked().getUniqueId())){
                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Nur der Besitzer kann Mitglieder hinzufügen!", NamedTextColor.DARK_RED));
                        return;
                    }
                    if(shop.getTrustedPlayers().size() >= 3){
                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Du kannst nicht mehr als 3 Mitglieder im Shop haben. Entferne zuerst welche!", NamedTextColor.RED));
                        return;
                    }
                    new ChatInputBuilder(Component.text("Bitte gib den Namen des Spielers in den Chat ein:"), player)
                            .onCancel(() -> SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Abgebrochen!", NamedTextColor.RED)))
                            .onInputWithString(name -> {
                                if(player.getName().equalsIgnoreCase(name)){
                                    return;
                                }
                                TurtleServer.getMongoDBHandler().getPetropiaPlayerByUsername(name).thenAccept(petropiaPlayer -> Bukkit.getScheduler().runTask(SpacelifeSpawn.getInstance(), () -> {
                                    if(petropiaPlayer == null){
                                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Der Spieler " + name + " wurde nicht gefunden", NamedTextColor.RED));
                                        return;
                                    }
                                    if(shop.getTrustedPlayers().contains(UUID.fromString(petropiaPlayer.getUuid()))){
                                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Der Spieler " + name + " ist bereits Mitglied des Shops", NamedTextColor.RED));
                                        return;
                                    }
                                    shop.addTrustedPlayer(UUID.fromString(petropiaPlayer.getUuid()));
                                    SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text(petropiaPlayer.getUserName(), NamedTextColor.GOLD).append(Component.text(" ist nun Mitglied des Shops", NamedTextColor.GREEN)));
                                }));
                            })
                            .build();
                });
    }

    private GuiItem createNpcLocationItem(){
        Component title = Component.text("NPC versetzten", NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        Component lore1 = Component.text("Klicke um den Verkäufer in deinem Shop", NamedTextColor.GRAY);
        Component lore2 = Component.text("an deine aktuelle Stelle zu setzen", NamedTextColor.GRAY);
        return ItemBuilder.from(Material.LIGHTNING_ROD)
                .name(title)
                .lore(
                        Component.empty(),
                        lore1,
                        lore2,
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(event -> {
                    event.getWhoClicked().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    event.getWhoClicked().playSound(Sound.sound(org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1F, 1.1F));
                    if(!shop.isInShop(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())){
                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Der Verkäufer kann nur in deinem Shop gesetzt werden!", NamedTextColor.RED));
                        return;
                    }
                    SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Der Verkäufer wurde umgesetzt!", NamedTextColor.GREEN));
                    shop.setNpcLocation(player.getLocation());
                });
    }

    /**
     * Adds trusted players heads to the gui and open it afterwards
     * @param gui Gui
     */
    private void addTrustedPlayerHeads(Gui gui) {
        if (shop.getTrustedPlayers().size() == 0) {
            gui.open(player);
            return;
        }
        List<CompletableFuture<PetropiaPlayer>> playerFutures = new ArrayList<>();
        for (UUID uuid : shop.getTrustedPlayers()) {
            playerFutures.add(TurtleServer.getMongoDBHandler().getPetropiaPlayerByUUID(uuid.toString()));
        }
        CompletableFuture<List<PetropiaPlayer>> allFutures = CompletableFuture.allOf(playerFutures.toArray(new CompletableFuture[0])).thenApplyAsync(v -> playerFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
        allFutures.thenAccept(players -> Bukkit.getScheduler().runTask(SpacelifeSpawn.getInstance(), () -> {
            if(players.size() == 1){
                gui.setItem(22, getTrustedPlayerHead(players.get(0)));
                gui.open(player);
                return;
            }
            if(players.size() == 2){
                gui.setItem(21, getTrustedPlayerHead(players.get(0)));
                gui.setItem(23, getTrustedPlayerHead(players.get(1)));
                gui.open(player);
                return;
            }
            if(players.size() == 3){
                gui.setItem(21, getTrustedPlayerHead(players.get(0)));
                gui.setItem(22, getTrustedPlayerHead(players.get(1)));
                gui.setItem(23, getTrustedPlayerHead(players.get(2)));
                gui.open(player);
            }
        }));
    }

    private GuiItem getTrustedPlayerHead(PetropiaPlayer petropiaPlayer) {
        String texture = petropiaPlayer.getSkinTexture();
        String name = petropiaPlayer.getUserName();
        UUID uuid = UUID.fromString(petropiaPlayer.getUuid());
        return ItemBuilder.skull()
                .texture(texture, uuid)
                .name(Component.text(name, NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                .lore(
                        Component.empty(),
                        Component.text("Linksklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Mitglied entfernen", NamedTextColor.RED)),
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(event -> {
                    if(!event.isLeftClick()){
                        return;
                    }
                    if(!shop.getOwner().equals(player.getUniqueId())){
                        return;
                    }
                    gui.close(player);
                    shop.removeTrustedPlayer(uuid);
                    SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text(name, NamedTextColor.GOLD).append(Component.text(" wurde als Mitglied entfernt", NamedTextColor.RED)));
                });
    }
}
