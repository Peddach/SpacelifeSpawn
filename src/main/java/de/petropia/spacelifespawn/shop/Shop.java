package de.petropia.spacelifespawn.shop;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.profile.ProfileProperty;
import com.github.juliarn.npclib.bukkit.util.BukkitPlatformUtil;
import de.petropia.spacelifeCore.player.SpacelifeDatabase;
import de.petropia.spacelifeCore.warp.Warp;
import de.petropia.spacelifespawn.SpacelifeSpawn;
import de.petropia.spacelifespawn.database.ShopDatabase;
import de.petropia.spacelifespawn.shop.gui.ShopNpcListener;
import de.petropia.turtleServer.server.TurtleServer;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Transient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bson.types.ObjectId;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
public class Shop implements Cloneable {
    @Id
    private ObjectId id;
    @Indexed
    private String shopID;
    private String name;
    private String owner;
    private String ownerName;
    private boolean rented;
    private long rentedUntil;
    private List<UUID> trustedPlayers;
    private int x1;
    private int y1;
    private int z1;
    private int x2;
    private int y2;
    private int z2;
    private int signX;
    private int singY;
    private int signZ;
    @Transient
    private Npc<World, Player, ItemStack, Plugin> npc;
    @Transient
    private ArmorStand tag;
    private double npcX;
    private double npcY;
    private double npcZ;
    private float npcPitch;
    private float npcYaw;
    /**
     * Constructor for Morphia
     */
    private Shop(){}

    /**
     * I know, a hell of a constructor, but its used only once.
     * @param id ID of the shop
     * @param signX Sign coordinate x
     * @param singY Sign coordinate y
     * @param signZ Sign coordinate z
     * @param x1 First corner x
     * @param y1 First corner y
     * @param z1 First corner z
     * @param x2 Second corner x
     * @param y2 Second corner y
     * @param z2 Second corner z
     */
    public Shop(String id, int signX, int singY, int signZ, int x1, int y1, int z1, int x2, int y2, int z2){
        this.shopID = id;
        this.signX = signX;
        this.singY = singY;
        this.signZ = signZ;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        trustedPlayers = new ArrayList<>();
    }

    /**
     * Check if a position is in the shop
     * @param x x coord
     * @param y y coord
     * @param z z coord
     * @return true if inside
     */
    public boolean isInShop(int x, int y, int z){
        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        double minZ = Math.min(z1, z2);
        double maxX = Math.max(x1, x2);
        double maxY = Math.max(y1, y2);
        double maxZ = Math.max(z1, z2);
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * Shows if the ShopCommand is rented
     * @return true if rented
     */
    public boolean isRented(){
        return rented;
    }

    /**
     * Shows how long the shop is rented
     * @return date as unix timestamp
     */
    public long getRentedUntil(){
        return rentedUntil;
    }

    /**
     * Sets a new owner and the duration
     * @param player Player who is new owner
     */
    public void rent(Player player, String name){
        this.owner = player.getUniqueId().toString();
        this.rentedUntil = Instant.ofEpochSecond(Instant.now().getEpochSecond())
                .plus(SpacelifeSpawn.getInstance().getConfig().getInt("weeksToRend")* 7L, ChronoUnit.DAYS)
                .getEpochSecond();
        this.ownerName = player.getName();
        this.rented = true;
        this.name = name;
        this.npcX = (x1 + x2) / 2.0;
        this.npcY = Math.min(y1, y2);
        this.npcZ = (z1 + z2) / 2.0;
        this.npcPitch = 0;
        this.npcYaw = -90F;
        ShopDatabase.getInstance().saveShop(this);
        spawnNPC();
    }

    public void unrent() {
        SpacelifeSpawn.getInstance().getLogger().info(shopID + " is expired!");
        for(Warp warp : SpacelifeDatabase.getInstance().getWarps()){
            if(warp.getName().equalsIgnoreCase(name)){
                SpacelifeDatabase.getInstance().removeWarp(warp.getName());
                break;
            }
        }
        removeNPC();
        this.owner = null;
        this.ownerName = null;
        this.rented = false;
        this.rentedUntil = 0;
        this.name = null;
        ShopDatabase.getInstance().saveShop(this);

        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = Bukkit.getWorld("world").getBlockAt(x, y, z);
                    block.setType(Material.AIR, false);
                    block.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, block.getLocation(), 4, 0.5, 0.5, 0.5);
                }
            }
        }
    }

    protected void onLoad(){
        writeSigns();
        if(rented){
           spawnNPC();
        }
    }

    private void spawnNPC() {
        TurtleServer.getMongoDBHandler().getPetropiaPlayerByUUID(owner).thenAccept(petropiaPlayer -> Bukkit.getServer().getScheduler().runTask(SpacelifeSpawn.getInstance(), () -> {
            ProfileProperty profileProperty = new ProfileProperty() {
                @Override
                public @NotNull String name() {
                    return "textures";
                }
                @Override
                public @NotNull String value() {
                    if(petropiaPlayer == null){
                        SpacelifeSpawn.getInstance().getLogger().info("Skin for " + ownerName + " not found!");
                        return "ewogICJ0aW1lc3RhbXAiIDogMTY4OTYxMzExMjc5OSwKICAicHJvZmlsZUlkIiA6ICJjMDZmODkwNjRjOGE0OTExOWMyOWVhMWRiZDFhYWI4MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNSEZfU3RldmUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiBmYWxzZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2U4ZjJjNTE0ZDBlMTE4MmQxZTk5ZTg5ZGM2ODA0MDEwNjdlNGJjOTgxMTM2Mzc2ZTY3YmQ1YmY3MTdkNmZhYWEiCiAgICB9LAogICAgIkNBUEUiIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzIzNDBjMGUwM2RkMjRhMTFiMTVhOGIzM2MyYTdlOWUzMmFiYjIwNTFiMjQ4MWQwYmE3ZGVmZDYzNWNhN2E5MzMiCiAgICB9CiAgfQp9=";
                    }
                    return petropiaPlayer.getSkinTexture();
                }
                @Override
                public @Nullable String signature() {
                    if(petropiaPlayer == null){
                        return null;
                    } else {
                        return petropiaPlayer.getSkinTextureSignature();
                    }
                }
            };
            npc = SpacelifeSpawn.getNpcPlatform().newNpcBuilder()
                    .flag(Npc.LOOK_AT_PLAYER, true)
                    .profile(Profile.resolved(
                            shopID,
                            UUID.randomUUID(),
                            Set.of(profileProperty)))
                    .position(BukkitPlatformUtil.positionFromBukkitLegacy(new Location(Bukkit.getWorld("world"), npcX, npcY, npcZ, npcYaw, npcPitch)))
                    .buildAndTrack();
                Location location = new Location(Bukkit.getWorld("world"), npcX, npcY - 0.15, npcZ, npcYaw, npcPitch);
                tag = location.getWorld().spawn(location, ArmorStand.class);
                tag.setGravity(false);
                tag.setCanPickupItems(false);
                tag.customName(Component.text(name).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                tag.setCustomNameVisible(true);
                tag.setVisible(false);
            Bukkit.getOnlinePlayers().forEach(player -> ShopNpcListener.registerScoreboardTeam(player.getScoreboard(), this));
        }));
    }

    public void onDisable(){
        removeNPC();
    }

    private void removeNPC(){
        if(npc != null) {
            npc.unlink();
        }
        if(tag != null){
            tag.remove();
        }
    }

    public void setNpcLocation(Location location) {
        this.npcX = location.getX();
        this.npcY = location.getY();
        this.npcZ = location.getZ();
        this.npcYaw = location.getYaw();
        this.npcPitch = location.getPitch();
        removeNPC();
        spawnNPC();
        ShopDatabase.getInstance().saveShop(this);
    }
    public void updateSigns(){
        writeSigns();
    }

    private void writeSigns(){
        Block block = getSignLocation().getBlock();
        if(!(block.getState() instanceof Sign sign1)){
            SpacelifeSpawn.getInstance().getLogger().warning("No Sign found for Shop " + shopID);
            return;
        }
        Sign sign2 = getSecondSign();
        if (sign2 == null){
            SpacelifeSpawn.getInstance().getLogger().warning("Could not find second Sing for " + shopID);
            return;
        }
        if(rented){
            sign1.line(0, Component.text("Besitzer:").decorate(TextDecoration.BOLD).color(TextColor.fromCSSHexString("#202947")));
            sign1.line(1, Component.text(ownerName).color(TextColor.fromCSSHexString("#3E0003")));
            sign1.line(2, Component.text("Shopname:").decorate(TextDecoration.BOLD).color(TextColor.fromCSSHexString("#202947")));
            sign1.line(3, Component.text(name).color(TextColor.fromCSSHexString("#3E0003")));

            sign2.line(1, Component.text("Laufzeit:").decorate(TextDecoration.BOLD).color(TextColor.fromCSSHexString("#202947")));
            sign2.line(2, Component.text(convertUnixTimestamp(rentedUntil)).color(TextColor.fromCSSHexString("#3E0003")));
            sign1.update(true);
            sign2.update(true);
            return;
        }
        sign1.line(0, Component.empty());
        sign1.line(1, Component.text("Klicke zum").decorate(TextDecoration.BOLD, TextDecoration.ITALIC).color(TextColor.fromCSSHexString("#32821")));
        sign1.line(2, Component.text("mieten").decorate(TextDecoration.BOLD, TextDecoration.ITALIC).color(TextColor.fromCSSHexString("#32821")));
        sign1.line(3, Component.empty());
        sign2.line(0, Component.empty());
        sign2.line(1, Component.text("Kosten:").color(TextColor.fromCSSHexString("#D5645")));
        sign2.line(2, Component.text(SpacelifeSpawn.getInstance().getConfig().getInt("shopCost") + "$").decorate(TextDecoration.BOLD).color(TextColor.fromCSSHexString("#75c454")));
        sign2.line(3, Component.empty());
        sign1.update(true);
        sign2.update(true);
    }

    /**
     * Get the sign next to the main sign
     * @return The next sign
     */
    private @Nullable Sign getSecondSign(){
        Block block = getSignLocation().getBlock();
        Block[] blocks = new Block[] {
                block.getRelative(1, 0 ,0),
                block.getRelative(-1, 0 ,0),
                block.getRelative(0, 0 ,-1),
                block.getRelative(0, 0 ,1)
        };
        for(Block i : blocks){
            if(i.getState() instanceof Sign sign){
                return sign;
            }
        }
        return null;
    }
    /**
     * Calculate time until the timespamp
     * @param timestamp Timestamp when shp expires
     * @return Converted String
     */
    public String convertUnixTimestamp(long timestamp) {
        long currentTimestamp = Instant.now().getEpochSecond();
        long secondsRemaining = timestamp - currentTimestamp;
        long months = secondsRemaining / 2592000;
        if (months > 0) {
            return months + (months == 1 ? " Monat" : " Monate");
        }
        long weeks = secondsRemaining / 604800;
        if (weeks > 0) {
            return weeks + (weeks == 1 ? " Woche" : " Wochen");
        }
        long days = secondsRemaining / 86400;
        if (days > 0) {
            return days + (days == 1 ? " Tag" : " Tage");
        }
        long hours = secondsRemaining / 3600;
        if (hours > 0) {
            return hours + (hours == 1 ? " Stunde" : " Stunden");
        }
        long minutes = secondsRemaining / 60;
        if (minutes > 0) {
            return minutes + (minutes == 1 ? " Minute" : " Minuten");
        }
        return secondsRemaining + (secondsRemaining == 1 ? " Sekunde" : " Sekunden");
    }

    public UUID getOwner(){
        if(owner == null){
            return null;
        }
        return UUID.fromString(owner);
    }

    public String getName(){
        return name;
    }

    public String getOwnerName(){
        return ownerName;
    }

    public String getShopID(){
        return shopID;
    }

    public Npc<World, Player, ItemStack, Plugin> getNPC(){
        return npc;
    }

    public List<UUID> getTrustedPlayers(){
        if(trustedPlayers == null){
            trustedPlayers = new ArrayList<>();
        }
        return new ArrayList<>(trustedPlayers);
    }

    /**
     * Add a player who can build in the shop
     * @param player UUID of Player who should be able to build
     */
    public void addTrustedPlayer(UUID player){
        trustedPlayers.add(player);
        ShopDatabase.getInstance().saveShop(this);
    }

    /**
     * Remove a player who can build in the shop
     * @param player UUID of Player who should not be able to build anymore
     */
    public void removeTrustedPlayer(UUID player){
        if(trustedPlayers.remove(player)){
            ShopDatabase.getInstance().saveShop(this);
        }
    }

    public Location getSignLocation(){
        return new Location(Bukkit.getWorld("world"), signX, singY, signZ);
    }

    @Override
    public Shop clone() {
        try {
            Shop clone = (Shop) super.clone();
            if(clone.trustedPlayers != null){
                clone.trustedPlayers = new ArrayList<>(this.trustedPlayers);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
