package dev.muon.questkilltask;

import dev.ftb.mods.ftblibrary.platform.Platform;
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

    public QuestProcessor() {
    }

    public static void markDirty() {
        killTasks = null;
    }

    private void initKillTasks() {
        ServerQuestFile instance = ServerQuestFile.getInstance();
        if (killTasks == null && instance != null) {
            killTasks = instance.collect(KillTask.class);
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

        return hasContributors;
    }

    public ServerQuestFile getQuestFile() {
        ServerQuestFile questFile = ServerQuestFile.getInstance();
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

    public void processDamagingTeams(LivingEntity entity, ServerQuestFile questFile, DamageSource source) {
        Set<UUID> processedTeams = new HashSet<>();

        // FTB's own onPlayerKilledEntity will credit the killer's team for direct player kills
        // (matching this exact gate). Pre-mark that team so our contributor loop skips it —
        // FTB stays in the call chain (mod compat preserved), no team gets +2.
        if (source != null && source.getEntity() instanceof ServerPlayer killer
                && !Platform.get().misc().isFakePlayer(killer)) {
            TeamData killerTeam = questFile.getOrCreateTeamData(killer.getUUID());
            if (killerTeam != null && !killerTeam.isLocked()) {
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

    private void processPlayerContribution(UUID playerUUID, LivingEntity entity, ServerQuestFile questFile,
                                           Set<UUID> processedTeams) {
        ServerPlayer player = questFile.server.getPlayerList().getPlayer(playerUUID);
        if (player == null) {
            return;
        }

        TeamData playerTeam = questFile.getOrCreateTeamData(player.getUUID());
        if (!isValidTeam(playerTeam)) {
            return;
        }

        if (!processedTeams.add(playerTeam.getTeamId())) {
            return;
        }

        updateTeamKillTasks(playerTeam, entity);
    }

    private boolean isValidTeam(TeamData playerTeam) {
        return playerTeam != null && !playerTeam.isLocked();
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
