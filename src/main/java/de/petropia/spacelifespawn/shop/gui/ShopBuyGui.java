package de.petropia.spacelifespawn.shop.gui;

import de.petropia.spacelifespawn.SpacelifeSpawn;
import de.petropia.spacelifespawn.shop.Shop;
import de.petropia.spacelifespawn.shop.ShopItem;
import de.petropia.turtleServer.api.chatInput.ChatInputBuilder;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShopBuyGui {

    private final Shop shop;
    private final Player player;
    private final PaginatedGui gui;

    public ShopBuyGui(Player player, Shop shop){
        this.player = player;
        this.shop = shop;
        gui = Gui.paginated()
                .disableAllInteractions()
                .title(Component.text(shop.getName(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .rows(5)
                .pageSize(36)
                .create();
        gui.setItem(38, createPreviousItem());
        if(shop.getOwner().equals(player.getUniqueId()) || shop.getTrustedPlayers().contains(player.getUniqueId())){
            gui.setItem(40, createItemAddItem());
        }
        gui.setItem(42, createNextItem());
        addItems();
        gui.open(player);
    }
    private GuiItem createPreviousItem(){
        Component name = Component.text("Vorherige Seite", NamedTextColor.GRAY).decorate(TextDecoration.BOLD);
        return ItemBuilder.from(Material.OCHRE_FROGLIGHT)
                .name(name)
                .asGuiItem(e -> {
                    gui.previous();
                    player.playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_OUT, Sound.Source.MASTER, 2F, 0.9F));
                });
    }

    private GuiItem createNextItem(){
        Component name = Component.text("Nächste Seite", NamedTextColor.GRAY).decorate(TextDecoration.BOLD);
        return ItemBuilder.from(Material.VERDANT_FROGLIGHT)
                .name(name)
                .asGuiItem(e -> {
                    gui.next();
                    player.playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_IN, Sound.Source.MASTER, 2F, 0.9F));
                });
    }

    private void addItems(){
        for(ShopItem item : shop.getShopItems()){
            ItemStack bukkitItem = item.getItem().clone();
            List<Component> lore =  bukkitItem.lore();
            if(lore == null){
                lore = new ArrayList<>();
            }
            lore.add(Component.empty());
            lore.add(Component.text("Preis: ", NamedTextColor.GRAY).append(Component.text(item.getBuyPrice(), NamedTextColor.GOLD)));
            if(!(shop.getOwner().equals(player.getUniqueId()) || shop.getTrustedPlayers().contains(player.getUniqueId()))) {
                lore.add(Component.text("Linksklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Kaufen", NamedTextColor.GOLD)));
            } else {
                lore.add(Component.text("Linksklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Item Löschen", NamedTextColor.RED)));
            }
            lore.add(Component.empty());
            bukkitItem.lore(lore);
            gui.addItem(ItemBuilder
                    .from(bukkitItem)
                    .asGuiItem(event -> {
                        if(event.isLeftClick() && (shop.getOwner().equals(player.getUniqueId()) || shop.getTrustedPlayers().contains(player.getUniqueId()))){
                            shop.removeShopItem(item);
                            SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Item entfernt", NamedTextColor.RED));
                            gui.close(player);
                            return;
                        }
                        if(event.isLeftClick()){
                            shop.buyItemForPlayer(item, player);
                            player.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1F, 1F));
                        }
                    })
            );
        }
    }

    private GuiItem createItemAddItem(){
        Component title = Component.text("Item zum Shop hinzufüge", NamedTextColor.GREEN).decorate(TextDecoration.BOLD);
        Component lore = Component.text("Fügt ein Item zum verkauf hinzu. Dieses sollte in einer Kiste in deinem Shop verfügbar sein.", NamedTextColor.GRAY);
        return ItemBuilder.from(Material.DIAMOND)
                .name(title)
                .lore(
                        Component.empty(),
                        lore,
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(event -> {
                    gui.close(player);
                    new ChatInputBuilder(Component.text("Bitte halte das Item welches du verkaufen möchtest in deiner Hand und gib den Preis in den Chat ein oder Abbrechen:", NamedTextColor.GREEN), player)
                           .onCancel(() -> SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Abgebrochen", NamedTextColor.RED)))
                           .mustBePositive(true)
                           .onInputWithDouble(price -> {
                               if(player.getInventory().getItemInMainHand().getType() == Material.AIR){
                                    SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Bitte halte das Item und seine korrekte Anzahl, die du verkaufen möchtest in der Hand", NamedTextColor.RED));
                                    return;
                               }
                               ShopItem item = new ShopItem(player.getInventory().getItemInMainHand(), price);
                               shop.addShopItem(item);
                               SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Das Item wurde hinzugefügt", NamedTextColor.GREEN));
                           })
                           .build();
                });
    }
}
