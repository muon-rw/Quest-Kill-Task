package dev.muon.questkilltask;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class PlayerAttribution {
    private static final int MAX_OWNER_DEPTH = 5;

    private static final List<Function<Entity, @Nullable Entity>> OWNER_LOOKUPS = new CopyOnWriteArrayList<>(List.of(
            entity -> entity instanceof OwnableEntity ownable ? ownable.getOwner() : null,
            entity -> entity instanceof TraceableEntity traceable ? traceable.getOwner() : null
    ));

    private PlayerAttribution() {
    }

    public static void registerOwnerLookup(Function<Entity, @Nullable Entity> lookup) {
        OWNER_LOOKUPS.add(lookup);
    }

    @Nullable
    public static ServerPlayer resolvePlayer(@Nullable Entity entity) {
        Entity current = entity;
        for (int depth = 0; current != null && depth < MAX_OWNER_DEPTH; depth++) {
            if (current instanceof ServerPlayer player) {
                return player;
            }
            Entity owner = findOwner(current);
            if (owner == null || owner == current) {
                return null;
            }
            current = owner;
        }
        return null;
    }

    @Nullable
    private static Entity findOwner(Entity entity) {
        for (Function<Entity, @Nullable Entity> lookup : OWNER_LOOKUPS) {
            Entity owner = lookup.apply(entity);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }
}
