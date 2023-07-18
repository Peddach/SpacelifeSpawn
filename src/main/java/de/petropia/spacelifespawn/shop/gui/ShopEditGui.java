package de.petropia.spacelifespawn.shop.gui;

import de.petropia.spacelifespawn.SpacelifeSpawn;
import de.petropia.spacelifespawn.shop.Shop;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;

public class ShopEditGui {

    private final Shop shop;
    private final Player player;

    public ShopEditGui(Player player, Shop shop) {
        this.shop = shop;
        this.player = player;
        Gui gui = Gui.gui()
                .rows(3)
                .disableAllInteractions()
                .title(Component.text(shop.getName(), NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .create();
        gui.setItem(17, createShopDeleteItem());
        gui.setItem(9, createNpcLocationItem());
        gui.open(player);
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

    private GuiItem createNpcLocationItem(){
        Component title = Component.text("NPC versetzten", NamedTextColor.RED).decorate(TextDecoration.BOLD);
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

}
