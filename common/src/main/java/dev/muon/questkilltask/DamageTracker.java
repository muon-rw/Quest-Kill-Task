package dev.muon.questkilltask;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.stream.Stream;

public class DamageTracker {
    private static class WeakSet<T> extends WeakHashMap<T, Boolean> {
        public boolean add(T element) {
            return put(element, Boolean.TRUE) == null;
        }

        public Set<T> toStrongSet() {
            return new HashSet<>(keySet());
        }

        public boolean contains(Object o) {
            return containsKey(o);
        }

        public Stream<T> stream() {
            return keySet().stream();
        }
    }
    private static final Map<LivingEntity, WeakSet<UUID>> DAMAGE_CONTRIBUTORS = new WeakHashMap<>();
    private static final Map<LivingEntity, WeakSet<UUID>> DAMAGE_RECEIVERS = new WeakHashMap<>();
    private static final Map<LivingEntity, WeakSet<UUID>> HEAL_CONTRIBUTORS = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> LAST_INTERACTION_TIME = new WeakHashMap<>();

    private static final long INTERACTION_TIMEOUT = 60000L; // 1 minute in milliseconds
    private static final long CLEANUP_INTERVAL = 30000L; // 30 seconds
    private static long lastCleanupTime = 0L;

    public static void trackDamage(LivingEntity victim, UUID contributorId) {
        long currentTime = System.currentTimeMillis();
        attemptCleanup(currentTime);

        DAMAGE_CONTRIBUTORS.computeIfAbsent(victim, k -> new WeakSet<>()).add(contributorId);
        LAST_INTERACTION_TIME.put(victim, currentTime);
        QuestKillTask.LOG.debug("Tracked damage from contributor {} to entity {}", contributorId, victim);
    }

    public static void trackDamageTaken(LivingEntity attacker, UUID victimId) {
        long currentTime = System.currentTimeMillis();
        attemptCleanup(currentTime);

        DAMAGE_RECEIVERS.computeIfAbsent(attacker, k -> new WeakSet<>()).add(victimId);
        LAST_INTERACTION_TIME.put(attacker, currentTime);
        QuestKillTask.LOG.debug("Tracked damage received by {} from entity {}", victimId, attacker);
    }

    public static void trackHealing(LivingEntity target, UUID healerId) {
        long currentTime = System.currentTimeMillis();
        attemptCleanup(currentTime);

        HEAL_CONTRIBUTORS.computeIfAbsent(target, k -> new WeakSet<>()).add(healerId);
        LAST_INTERACTION_TIME.put(target, currentTime);
        QuestKillTask.LOG.debug("Tracked healing from {} to entity {}", healerId, target);
    }

    private static void attemptCleanup(long currentTime) {
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanupTimedOutEntries(currentTime);
            lastCleanupTime = currentTime;
        }
    }

    private static void cleanupTimedOutEntries(long currentTime) {
        Iterator<Map.Entry<LivingEntity, Long>> it = LAST_INTERACTION_TIME.entrySet().iterator();
        int removedCount = 0;

        while (it.hasNext()) {
            Map.Entry<LivingEntity, Long> entry = it.next();
            if (currentTime - entry.getValue() > INTERACTION_TIMEOUT) {
                LivingEntity entity = entry.getKey();
                DAMAGE_CONTRIBUTORS.remove(entity);
                DAMAGE_RECEIVERS.remove(entity);
                HEAL_CONTRIBUTORS.remove(entity);
                it.remove();
                removedCount++;
            }
        }

    }

    public static record KillContributors(
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

        Set<UUID> damagers = DAMAGE_CONTRIBUTORS.getOrDefault(entity, new WeakSet<>()).toStrongSet();
        Set<UUID> tanks = DAMAGE_RECEIVERS.getOrDefault(entity, new WeakSet<>()).toStrongSet();
        Set<UUID> healers = new HashSet<>();

        WeakSet<UUID> healerCandidates = HEAL_CONTRIBUTORS.getOrDefault(entity, new WeakSet<>());
        for (UUID damager : damagers) {
            healers.addAll(
                    healerCandidates.stream()
                            .filter(healer -> hasHealedPlayer(healer, damager))
                            .toList()
            );
        }


        return new KillContributors(damagers, healers, tanks);
    }

    private static boolean hasHealedPlayer(UUID healer, UUID target) {
        return HEAL_CONTRIBUTORS.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof ServerPlayer)
                .filter(entry -> ((ServerPlayer) entry.getKey()).getUUID().equals(target))
                .anyMatch(entry -> entry.getValue().contains(healer));
    }

    public static void clearEntityTracking(LivingEntity entity) {
        DAMAGE_CONTRIBUTORS.remove(entity);
        DAMAGE_RECEIVERS.remove(entity);
        HEAL_CONTRIBUTORS.remove(entity);
        LAST_INTERACTION_TIME.remove(entity);
    }
}