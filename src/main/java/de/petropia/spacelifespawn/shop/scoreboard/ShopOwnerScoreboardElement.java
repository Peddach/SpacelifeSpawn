package de.petropia.spacelifespawn.shop.scoreboard;

import de.petropia.spacelifeCore.scoreboard.element.RegionScoreboardElement;
import de.petropia.spacelifespawn.shop.Shop;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShopOwnerScoreboardElement extends RegionScoreboardElement {

    private final Shop shop;

    public ShopOwnerScoreboardElement(Shop shop){
        this.shop = shop;
    }

    @Override
    public BoundingBox getRegion() {
        return shop.getBoundingBox();
    }

    @Override
    public List<String> getContent(Player player) {
        if(shop.isRented()){
            return List.of(shop.getOwnerName());
        }
        return List.of("Nicht vermietet");
    }

    @Override
    public String getTitle() {
        return "Shop Besitzer";
    }

    @Override
    public @Nullable String getPermission() {
        return null;
    }

    @Override
    public int getPriority() {
        return 150;
    }
}
