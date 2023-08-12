package de.petropia.spacelifespawn.jobs;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.profile.ProfileProperty;
import com.github.juliarn.npclib.bukkit.util.BukkitPlatformUtil;
import de.petropia.spacelifespawn.SpacelifeSpawn;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JobNPC {

    private static final List<JobNPC> jobNPCS = new ArrayList<>();

    private final String jobID;

    private Npc<World, Player, ItemStack, Plugin> npc;
    public JobNPC(Location location, String jobID, String name, String texture, String signature){
        spawnNpc(name, texture, signature, location);
        jobNPCS.add(this);
        this.jobID = jobID;
    }

    private void spawnNpc(String name, String texture, String signature, Location location){
        ProfileProperty profileProperty = new ProfileProperty() {
            @Override
            public @NotNull String name() {
                return "textures";
            }
            @Override
            public @NotNull String value() {
                return texture;
            }
            @Override
            public @Nullable String signature() {
                return signature;
            }
        };
        npc = SpacelifeSpawn.getNpcPlatform().newNpcBuilder()
                .flag(Npc.LOOK_AT_PLAYER, true)
                .flag(Npc.SNEAK_WHEN_PLAYER_SNEAKS, true)
                .flag(Npc.HIT_WHEN_PLAYER_HITS, true)
                .profile(Profile.resolved(
                        name,
                        UUID.randomUUID(),
                        Set.of(profileProperty)))
                .position(BukkitPlatformUtil.positionFromBukkitLegacy(location))
                .buildAndTrack();
    }

    public String getJobID(){
        return jobID;
    }

    public Npc<World, Player, ItemStack, Plugin> getNpc(){
        return npc;
    }

    public static List<JobNPC> getJobNPCS(){
        return jobNPCS;
    }
}
