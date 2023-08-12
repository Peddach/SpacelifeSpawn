package de.petropia.spacelifespawn.shop.gui;

import de.petropia.spacelifespawn.SpacelifeSpawn;
import de.petropia.spacelifespawn.database.ShopDatabase;
import de.petropia.spacelifespawn.shop.Shop;
import de.petropia.spacelifespawn.shop.ShopItem;
import de.petropia.turtleServer.api.chatInput.ChatInputBuilder;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShopItemEditGui {

    private final Player player;
    private final Shop shop;
    private final ShopItem shopItem;
    private final Gui gui;
    public ShopItemEditGui(Player player, Shop shop, ShopItem item) {
        this.player = player;
        this.shop = shop;
        this.shopItem = item;
        gui = Gui.gui()
                .disableAllInteractions()
                .title(Component.text("Item bearbeiten", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .type(GuiType.HOPPER)
                .create();
        gui.setItem(2, createDisplayItem());
        gui.setItem(0, createSetBuyPrice());
        gui.setItem(4, createSetSellPrice());
        SpacelifeSpawn.getInstance().getLogger().info("Open edit gui");
        gui.open(player);
    }

    private GuiItem createDisplayItem(){
        ItemStack itemStack = shopItem.getItem().clone();
        List<Component> lore = itemStack.lore();
        if(lore == null){
            lore = new ArrayList<>();
        }
        lore.add(Component.empty());
        lore.add(Component.text("Verkaufspreis: ", NamedTextColor.GRAY).append(Component.text(shopItem.getBuyPrice(), NamedTextColor.GOLD)));
        lore.add(Component.text("Ankaufspreis: ", NamedTextColor.GRAY).append(Component.text(shopItem.getSellPrice(), NamedTextColor.GOLD)));
        lore.add(Component.empty());
        itemStack.lore(lore);
        return ItemBuilder.from(itemStack).asGuiItem();
    }

    private GuiItem createSetBuyPrice(){
        return ItemBuilder.from(Material.GLOWSTONE_DUST)
                .name(Component.text("Verkaufspreis ändern", NamedTextColor.GOLD))
                .lore(
                        Component.empty(),
                        Component.text("Passe den Verkaufspreis an", NamedTextColor.GRAY),
                        Component.text("Setze ihn auf 0 um Verkaufen zu deaktivieren", NamedTextColor.GRAY),
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(event -> {
                   gui.close(player);
                   new ChatInputBuilder(Component.text("Gib 'Abbrechen' oder den neuen Peis in den Chat ein", NamedTextColor.GRAY), player)
                           .mustBePositive(true)
                           .onCancel(() -> gui.open(player))
                           .onInputWithDouble(amount -> {
                               if(amount >= 1_000_000){
                                   SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Dieser Preis scheint etwas hoch zu sein!", NamedTextColor.RED));
                                   return;
                               }
                               amount = Math.floor(amount * 100) / 100;
                               shopItem.setBuyPrice(amount);
                               ShopDatabase.getInstance().saveShop(shop);
                               SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Preis geändert!", NamedTextColor.GREEN));
                               new ShopItemEditGui(player, shop, shopItem);
                           })
                           .build();
                });
    }

    private GuiItem createSetSellPrice(){
        return ItemBuilder.from(Material.REDSTONE)
                .name(Component.text("Ankaufspreis ändern", NamedTextColor.GOLD))
                .lore(
                        Component.empty(),
                        Component.text("Passe den Ankaufspreis an", NamedTextColor.GRAY),
                        Component.text("Setze ihn auf 0 um Ankaufen zu deaktivieren", NamedTextColor.GRAY),
                        Component.empty()
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(event -> {
                    gui.close(player);
                    new ChatInputBuilder(Component.text("Gib 'Abbrechen' oder den neuen Peis in den Chat ein", NamedTextColor.GRAY), player)
                            .mustBePositive(true)
                            .onCancel(() -> gui.open(player))
                            .onInputWithDouble(amount -> {
                                if(amount >= 1_000_000){
                                    SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Dieser Preis scheint etwas hoch zu sein!", NamedTextColor.RED));
                                    return;
                                }
                                amount = Math.floor(amount * 100) / 100;
                                shopItem.setSellPrice(amount);
                                ShopDatabase.getInstance().saveShop(shop);
                                SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Preis geändert!", NamedTextColor.GREEN));
                                new ShopItemEditGui(player, shop, shopItem);
                            })
                            .build();
                });
    }
}
