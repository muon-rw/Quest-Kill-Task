package dev.muon.questkilltask;

import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class QuestProcessor {
    private static List<KillTask> killTasks;

    public static void markDirty() {
        killTasks = null;
    }

    public static void onEntityDeath(LivingEntity victim, DamageSource source) {
        if (!shouldProcessKill(victim)) {
            return;
        }
        ServerQuestFile questFile = getQuestFile();
        if (questFile == null) {
            QuestKillTask.LOG.warn("Unable to retrieve Server Quest File!");
            return;
        }
        processDamagingTeams(victim, questFile, source);
        DamageTracker.clearEntityTracking(victim);
    }

    private static void initKillTasks() {
        if (killTasks == null && ServerQuestFile.INSTANCE != null) {
            killTasks = ServerQuestFile.INSTANCE.collect(KillTask.class);
        }
    }

    private static boolean shouldProcessKill(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return false;
        }

        initKillTasks();
        if (killTasks == null || killTasks.isEmpty()) {
            return false;
        }

        DamageTracker.KillContributors contributors = DamageTracker.getKillContributors(entity);
        return !contributors.damagers().isEmpty()
                || !contributors.healers().isEmpty()
                || !contributors.tanks().isEmpty();
    }

    private static ServerQuestFile getQuestFile() {
        ServerQuestFile questFile = ServerQuestFile.INSTANCE;
        if (questFile == null) {
            QuestKillTask.LOG.warn("Could not get Server Quest File instance!");
            return null;
        }

        initKillTasks();
        if (killTasks == null || killTasks.isEmpty()) {
            QuestKillTask.LOG.warn("No kill tasks were present in the current quest file!");
            return null;
        }

        return questFile;
    }

    private static void processDamagingTeams(LivingEntity entity, ServerQuestFile questFile, DamageSource source) {
        Set<UUID> processedTeams = new HashSet<>();

        // FTB's own death handler already credits the killer's team for direct, non-fake player kills (this exact
        // gate), so we skip that team rather than cancel FTB's handler, which keeps other mods' hooks on it working.
        if (source != null && source.getEntity() instanceof ServerPlayer killer && !PlayerHooks.isFake(killer)) {
            TeamData killerTeam = questFile.getOrCreateTeamData(killer);
            if (isValidTeam(killerTeam)) {
                processedTeams.add(killerTeam.getTeamId());
            }
        }

        DamageTracker.KillContributors contributors = DamageTracker.getKillContributors(entity);
        Set<UUID> allContributors = new HashSet<>();
        allContributors.addAll(contributors.damagers());
        allContributors.addAll(contributors.healers());
        allContributors.addAll(contributors.tanks());

        for (UUID playerUUID : allContributors) {
            processPlayerContribution(playerUUID, entity, questFile, processedTeams);
        }
    }

    private static void processPlayerContribution(UUID playerUUID, LivingEntity entity, ServerQuestFile questFile,
                                                  Set<UUID> processedTeams) {
        ServerPlayer player = questFile.server.getPlayerList().getPlayer(playerUUID);
        if (player == null) {
            return;
        }

        TeamData playerTeam = questFile.getOrCreateTeamData(player);
        if (!isValidTeam(playerTeam) || !processedTeams.add(playerTeam.getTeamId())) {
            return;
        }

        updateTeamKillTasks(playerTeam, entity);
    }

    private static boolean isValidTeam(TeamData playerTeam) {
        return playerTeam != null && !playerTeam.isLocked();
    }

    private static void updateTeamKillTasks(TeamData team, LivingEntity entity) {
        for (KillTask task : killTasks) {
            if (canCompleteTask(team, task)) {
                task.kill(team, entity);
            }
        }
    }

    private static boolean canCompleteTask(TeamData team, KillTask task) {
        return team.getProgress(task) < task.getMaxProgress()
                && team.canStartTasks(task.getQuest());
    }
}
