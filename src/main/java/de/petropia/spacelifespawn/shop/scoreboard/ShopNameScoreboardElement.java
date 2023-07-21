package de.petropia.spacelifespawn.shop.scoreboard;

import de.petropia.spacelifeCore.scoreboard.element.RegionScoreboardElement;
import de.petropia.spacelifespawn.shop.Shop;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShopNameScoreboardElement extends RegionScoreboardElement {
    private final Shop shop;

    public ShopNameScoreboardElement(Shop shop){
        this.shop = shop;
    }

    @Override
    public BoundingBox getRegion() {
        return shop.getBoundingBox();
    }

    @Override
    public List<String> getContent(Player player) {
        if(shop.isRented()){
            return List.of(shop.getName());
        }
        return List.of(shop.getShopID());
    }

    @Override
    public String getTitle() {
        if(shop.isRented()){
            return "Shop Name";
        }
        return "Shop ID";
    }

    @Override
    public @Nullable String getPermission() {
        return null;
    }

    @Override
    public int getPriority() {
        return 160;
    }
}
