package de.petropia.spacelifespawn.shop.gui;

import de.petropia.spacelifeCore.player.SpacelifeDatabase;
import de.petropia.spacelifeCore.player.SpacelifePlayer;
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
import org.bukkit.Bukkit;
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
        gui.setItem(37, createPreviousItem());
        if(shop.getOwner().equals(player.getUniqueId()) || shop.getTrustedPlayers().contains(player.getUniqueId())){
            gui.setItem(39, createShopEditItem());
            gui.setItem(40, createItemAddItem());
            gui.setItem(41, createMoneyWithdrawItem());
        }
        gui.setItem(43, createNextItem());
        addItems();
        gui.open(player);
    }
    private GuiItem createPreviousItem(){
        Component name = Component.text("Vorherige Seite", NamedTextColor.GRAY).decorate(TextDecoration.BOLD);
        return ItemBuilder.from(Material.OCHRE_FROGLIGHT)
                .name(name)
                .asGuiItem(e -> {
                    gui.previous();
                    player.playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_OUT, Sound.Source.MASTER, 2F, 1.2F));
                });
    }

    private GuiItem createNextItem(){
        Component name = Component.text("Nächste Seite", NamedTextColor.GRAY).decorate(TextDecoration.BOLD);
        return ItemBuilder.from(Material.VERDANT_FROGLIGHT)
                .name(name)
                .asGuiItem(e -> {
                    gui.next();
                    player.playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_IN, Sound.Source.MASTER, 2F, 1.2F));
                });
    }

    private void addItems(){
        for(ShopItem item : shop.getShopItems()){
            if(item.getItem().getType() == Material.AIR){
                shop.removeShopItem(item);
                continue;
            }
            ItemStack bukkitItem = item.getItem().clone();
            List<Component> lore =  bukkitItem.lore();
            if(lore == null){
                lore = new ArrayList<>();
            }
            lore.add(Component.empty());
            if(item.getBuyPrice() > 0D){
                lore.add(Component.text("Verkaufspreis: ", NamedTextColor.GRAY).append(Component.text(item.getBuyPrice(), NamedTextColor.GOLD)));
            }
            if(item.getSellPrice() > 0D){
                lore.add(Component.text("Ankaufspreis: ", NamedTextColor.GRAY).append(Component.text(item.getSellPrice(), NamedTextColor.GOLD)));
            }
            lore.add(Component.empty());
            if(!(shop.getOwner().equals(player.getUniqueId()) || shop.getTrustedPlayers().contains(player.getUniqueId()))) {
                if(item.getBuyPrice() > 0D){
                    lore.add(Component.text("Rechtsklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Kaufen", NamedTextColor.GOLD)));
                }
                if(item.getSellPrice() > 0D){
                    lore.add(Component.text("Linksklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Verkaufen", NamedTextColor.GREEN)));
                }
                if(item.getSellPrice() == 0D && item.getBuyPrice() == 0D){
                    lore.add(Component.text("Kein Preis festgelegt!", NamedTextColor.RED));
                }
            } else {
                lore.add(Component.text("Linksklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Item Löschen", NamedTextColor.RED)));
                lore.add(Component.text("Rechtsklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Bearbeiten", NamedTextColor.GREEN)));
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
                        if(event.isRightClick()  && (shop.getOwner().equals(player.getUniqueId()) || shop.getTrustedPlayers().contains(player.getUniqueId()))){
                            gui.close(player);
                            Bukkit.getScheduler().runTaskLater(SpacelifeSpawn.getInstance(), () -> new ShopItemEditGui(player, shop, item), 2);
                            return;
                        }
                        if(event.isRightClick()){
                            if(item.getBuyPrice() == 0D){
                                return;
                            }
                            shop.buyItemForPlayer(item, player);
                            return;
                        }
                        if(event.isLeftClick()){
                            if(item.getSellPrice() == 0D){
                                return;
                            }
                            shop.sellItemForPlayer(player, item);
                        }
                    })
            );
        }
    }

    private GuiItem createItemAddItem(){
        Component title = Component.text("Item zum Shop hinzufügen", NamedTextColor.GREEN).decorate(TextDecoration.BOLD);
        Component lore1 = Component.text("Fügt ein Item zum verkauf hinzu. Dieses sollte", NamedTextColor.GRAY);
        Component lore2 = Component.text("in einer Kiste in deinem Shop verfügbar sein.", NamedTextColor.GRAY);
        return ItemBuilder.from(Material.DIAMOND)
                .name(title)
                .lore(
                        Component.empty(),
                        lore1,
                        lore2,
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

    private GuiItem createShopEditItem(){
        return ItemBuilder.from(Material.REPEATER)
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .lore(
                        Component.empty(),
                        Component.text("Passe die Einstellungen deines Shops an.", NamedTextColor.GRAY),
                        Component.text("Diese können auch mit /shop edit erreicht werden.", NamedTextColor.GRAY),
                        Component.empty()
                )
                .name(Component.text("Einstellungen").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .asGuiItem(event -> {
                    event.getWhoClicked().closeInventory();
                    Bukkit.getScheduler().runTaskLater(SpacelifeSpawn.getInstance(), () -> new ShopEditGui(player, shop), 1);
                });
    }

    private GuiItem createMoneyWithdrawItem(){
        return ItemBuilder.from(Material.SUNFLOWER)
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .lore(
                        Component.empty(),
                        Component.text("Hebe dein Geld aus dem Shop ab oder zahle welche ein.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("Aktuelles Guthaben: ", NamedTextColor.GRAY).append(Component.text(shop.getMoney()).color(NamedTextColor.GOLD)),
                        Component.empty(),
                        Component.text("Linksklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Geld auszahlen", NamedTextColor.GREEN)),
                        Component.text("Rechtsklick", NamedTextColor.GRAY).append(Component.text(" >> ", NamedTextColor.DARK_GRAY)).append(Component.text("Geld einzahlen", NamedTextColor.GOLD)),
                        Component.empty()
                )
                .name(Component.text("Geld abheben").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .asGuiItem(event -> {
                    event.getWhoClicked().closeInventory();
                    SpacelifePlayer spacelifePlayer = SpacelifeDatabase.getInstance().getCachedPlayer(player.getUniqueId());
                    if(event.isLeftClick()){
                        if(shop.getMoney() == 0D){
                            SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Du musst erst geld verdienen um dir welches auszalen zu lassen!", NamedTextColor.RED));
                            return;
                        }
                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player,
                                Component.text("Du hast ",NamedTextColor.GRAY)
                                        .append(Component.text(shop.getMoney() + "$", NamedTextColor.GOLD))
                                        .append(Component.text(" erhalten", NamedTextColor.GRAY)));
                        spacelifePlayer.addMoney(shop.getMoney());
                        shop.setMoney(0);
                        player.playSound(Sound.sound(org.bukkit.Sound.ITEM_GOAT_HORN_SOUND_1, Sound.Source.MASTER, 1.5F, 1.15F));
                    }
                    if(event.isRightClick()){
                        new ChatInputBuilder(Component.text("Gib den Einzahlbetrag oder 'Abbrechen' in den Chat ein:", NamedTextColor.GRAY), player)
                                .onCancel(() -> SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Abgebrochen", NamedTextColor.RED)))
                                .mustBePositive(true)
                                .onInputWithDouble(amount -> {
                                    if(!spacelifePlayer.subtractMoney(amount)){
                                        SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Du hast nicht genug Geld", NamedTextColor.RED));
                                        return;
                                    }
                                    shop.setMoney(shop.getMoney() + amount);
                                    SpacelifeSpawn.getInstance().getMessageUtil().sendMessage(player, Component.text("Geld erfolgreich eingezahl", NamedTextColor.GREEN));
                                })
                                .build();
                    }
                });
    }
}
