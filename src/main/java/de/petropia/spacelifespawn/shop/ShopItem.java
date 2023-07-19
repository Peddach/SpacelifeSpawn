package de.petropia.spacelifespawn.shop;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Transient;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;

@Entity
public class ShopItem {
    @Transient
    private ItemStack item;

    private String stringRepresentation;
    private double buyPrice;

    /**
     * Morphia Constructor
     */
    private ShopItem(){}

    public ShopItem(ItemStack item, double price){
        this.item = item;
        this.buyPrice = price;
        stringRepresentation = toBase64(item.serializeAsBytes());
    }

    /**
     * Get the price for which a player buys that item from the shop
     * @return price as double
     */
    public double getBuyPrice(){
        return buyPrice;
    }

    /**
     * Set the price for which a player buys that item from the shop
     * @param buyPrice buyPrice as double
     */
    public void setBuyPrice(double buyPrice){
        this.buyPrice = buyPrice;
    }

    public ItemStack getItem() {
        if(item == null){
            item = ItemStack.deserializeBytes(fromBase64(stringRepresentation));
        }
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
        stringRepresentation = toBase64(item.serializeAsBytes());
    }

    private String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] fromBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}
