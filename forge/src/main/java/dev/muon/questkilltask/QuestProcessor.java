package dev.muon.questkilltask;

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

    public QuestProcessor(List<KillTask> killTasks) {
        QuestProcessor.killTasks = null;
    }

    public static void invalidateKillTasks() {
        killTasks = null;  // Clear the cache
    }

    private void initKillTasks() {
        if (killTasks == null && ServerQuestFile.INSTANCE != null) {
            killTasks = ServerQuestFile.INSTANCE.collect(KillTask.class);
        }
    }

    public boolean shouldProcessKill(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return false;
        }

        initKillTasks();

        if (killTasks == null || killTasks.isEmpty()) {
            return false;
        }

        DamageTracker.KillContributors contributors = DamageTracker.getKillContributors(entity);
        boolean hasContributors = !contributors.damagers().isEmpty() ||
                !contributors.healers().isEmpty() ||
                !contributors.tanks().isEmpty();

        if (!hasContributors) {
            return false;
        }
        return true;
    }

    public ServerQuestFile getQuestFile() {
        ServerQuestFile questFile = ServerQuestFile.INSTANCE;
        if (questFile == null) {
            return null;
        }

        initKillTasks();
        if (killTasks == null || killTasks.isEmpty()) {
            return null;
        }

        return questFile;
    }

    public TeamData getKillerTeam(DamageSource source, ServerQuestFile questFile) {
        if (!(source.getEntity() instanceof ServerPlayer killer)) {
            return null;
        }

        TeamData killerTeam = questFile.getOrCreateTeamData(killer);
        return killerTeam;
    }

    public void processDamagingTeams(LivingEntity entity, ServerQuestFile questFile, TeamData killerTeam) {
        Set<UUID> processedTeams = new HashSet<>();
        DamageTracker.KillContributors contributors = DamageTracker.getKillContributors(entity);

        Set<UUID> allContributors = new HashSet<>();
        allContributors.addAll(contributors.damagers());
        allContributors.addAll(contributors.healers());
        allContributors.addAll(contributors.tanks());

        for (UUID playerUUID : allContributors) {
            processPlayerContribution(playerUUID, entity, questFile, killerTeam, processedTeams);
        }
    }

    private void processPlayerContribution(UUID playerUUID, LivingEntity entity, ServerQuestFile questFile,
                                           TeamData killerTeam, Set<UUID> processedTeams) {
        ServerPlayer player = questFile.server.getPlayerList().getPlayer(playerUUID);
        if (player == null) {
            return;
        }

        TeamData playerTeam = questFile.getOrCreateTeamData(player);
        if (!isValidTeam(playerTeam, killerTeam)) {
            return;
        }

        if (!processedTeams.add(playerTeam.getTeamId())) {
            return;
        }

        updateTeamKillTasks(playerTeam, entity);
    }

    private boolean isValidTeam(TeamData playerTeam, TeamData killerTeam) {
        if (playerTeam == null || playerTeam.isLocked()) {
            return false;
        }

        // We override the logic for now
        /*
        if (killerTeam != null && killerTeam.getTeamId().equals(playerTeam.getTeamId())) {
            QuestKillTask.LOG.info("Skipping killer's team (will be handled by original logic)");
            return false;
        }
        */

        return true;
    }

    private void updateTeamKillTasks(TeamData team, LivingEntity entity) {
        for (KillTask task : killTasks) {
            if (canCompleteTask(team, task)) {
                task.kill(team, entity);
            }
        }
    }

    private boolean canCompleteTask(TeamData team, KillTask task) {
        return team.getProgress(task) < task.getMaxProgress() &&
                team.canStartTasks(task.getQuest());
    }
}