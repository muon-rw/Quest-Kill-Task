package dev.muon.questkilltask;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class DamageTracker {
    private static final Map<LivingEntity, Set<UUID>> DAMAGE_CONTRIBUTORS = new WeakHashMap<>();
    private static final Map<LivingEntity, Set<UUID>> DAMAGE_RECEIVERS = new WeakHashMap<>();
    private static final Map<UUID, Map<UUID, Long>> HEAL_CONTRIBUTORS = new HashMap<>();
    private static final Map<LivingEntity, Long> LAST_INTERACTION_TIME = new WeakHashMap<>();

    private static final long INTERACTION_TIMEOUT = 60000L;
    private static final long CLEANUP_INTERVAL = 30000L;
    private static long lastCleanupTime = 0L;

    public static void trackDamage(LivingEntity victim, UUID contributorId) {
        long currentTime = System.currentTimeMillis();
        attemptCleanup(currentTime);

        DAMAGE_CONTRIBUTORS.computeIfAbsent(victim, k -> new HashSet<>()).add(contributorId);
        LAST_INTERACTION_TIME.put(victim, currentTime);
        QuestKillTask.LOG.debug("Tracked damage from contributor {} to entity {}", contributorId, victim);
    }

    public static void trackDamageTaken(LivingEntity attacker, UUID victimId) {
        long currentTime = System.currentTimeMillis();
        attemptCleanup(currentTime);

        DAMAGE_RECEIVERS.computeIfAbsent(attacker, k -> new HashSet<>()).add(victimId);
        LAST_INTERACTION_TIME.put(attacker, currentTime);
        QuestKillTask.LOG.debug("Tracked damage received by {} from entity {}", victimId, attacker);
    }

    public static void trackHealing(LivingEntity target, UUID healerId) {
        if (!(target instanceof ServerPlayer)) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        attemptCleanup(currentTime);

        HEAL_CONTRIBUTORS.computeIfAbsent(target.getUUID(), k -> new HashMap<>()).put(healerId, currentTime);
        QuestKillTask.LOG.debug("Tracked healing from {} to player {}", healerId, target);
    }

    private static void attemptCleanup(long currentTime) {
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanupTimedOutEntries(currentTime);
            lastCleanupTime = currentTime;
        }
    }

    private static void cleanupTimedOutEntries(long currentTime) {
        Iterator<Map.Entry<LivingEntity, Long>> it = LAST_INTERACTION_TIME.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<LivingEntity, Long> entry = it.next();
            if (currentTime - entry.getValue() > INTERACTION_TIMEOUT) {
                LivingEntity entity = entry.getKey();
                DAMAGE_CONTRIBUTORS.remove(entity);
                DAMAGE_RECEIVERS.remove(entity);
                it.remove();
            }
        }

        Iterator<Map.Entry<UUID, Map<UUID, Long>>> healIt = HEAL_CONTRIBUTORS.entrySet().iterator();
        while (healIt.hasNext()) {
            Map<UUID, Long> healers = healIt.next().getValue();
            healers.entrySet().removeIf(entry -> currentTime - entry.getValue() > INTERACTION_TIMEOUT);
            if (healers.isEmpty()) {
                healIt.remove();
            }
        }
    }

    public record KillContributors(
            Set<UUID> damagers,
            Set<UUID> healers,
            Set<UUID> tanks
    ) {}

    public static KillContributors getKillContributors(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return new KillContributors(Set.of(), Set.of(), Set.of());
        }

        long currentTime = System.currentTimeMillis();
        if (LAST_INTERACTION_TIME.getOrDefault(entity, 0L) + INTERACTION_TIMEOUT < currentTime) {
            clearEntityTracking(entity);
            return new KillContributors(Set.of(), Set.of(), Set.of());
        }

        Set<UUID> damagers = new HashSet<>(DAMAGE_CONTRIBUTORS.getOrDefault(entity, Set.of()));
        Set<UUID> tanks = new HashSet<>(DAMAGE_RECEIVERS.getOrDefault(entity, Set.of()));
        Set<UUID> healers = new HashSet<>();

        collectRecentHealers(damagers, currentTime, healers);
        collectRecentHealers(tanks, currentTime, healers);

        return new KillContributors(damagers, healers, tanks);
    }

    private static void collectRecentHealers(Set<UUID> participants, long currentTime, Set<UUID> outHealers) {
        for (UUID participant : participants) {
            Map<UUID, Long> healedBy = HEAL_CONTRIBUTORS.get(participant);
            if (healedBy == null) {
                continue;
            }
            for (Map.Entry<UUID, Long> entry : healedBy.entrySet()) {
                if (currentTime - entry.getValue() <= INTERACTION_TIMEOUT) {
                    outHealers.add(entry.getKey());
                }
            }
        }
    }

    public static void clearEntityTracking(LivingEntity entity) {
        DAMAGE_CONTRIBUTORS.remove(entity);
        DAMAGE_RECEIVERS.remove(entity);
        LAST_INTERACTION_TIME.remove(entity);
    }
}
