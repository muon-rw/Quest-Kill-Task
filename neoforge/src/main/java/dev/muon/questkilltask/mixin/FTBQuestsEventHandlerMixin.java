package dev.muon.questkilltask.mixin;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.FTBQuestsEventHandler;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import dev.muon.questkilltask.DamageTracker;
import dev.muon.questkilltask.QuestProcessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = FTBQuestsEventHandler.class, remap = false)
public class FTBQuestsEventHandlerMixin {
    @Shadow(remap = false)
    private List<KillTask> killTasks;

    @Unique
    private QuestProcessor questKillTask$questProcessor;

    /**
     * Replaces the original playerKill method with enhanced functionality.
     * This implementation adds support for:
     * - Healing/Support credit
     * - Tank/Damage-taken credit
     * - Potion/Effect tracking
     *
     * While this is functionally an @Overwrite and not great for compatibility,
     * It's unlikely other mods interact with this task.
     * This can be rewritten if issues arise.
     */
    @Inject(method = "playerKill", at = @At("HEAD"), cancellable = true)
    private void onPlayerKill(LivingEntity entity, DamageSource source, CallbackInfoReturnable<EventResult> cir) {
        if (questKillTask$questProcessor == null) {
            questKillTask$questProcessor = new QuestProcessor(killTasks);
        }

        if (!questKillTask$questProcessor.shouldProcessKill(entity)) {
            cir.setReturnValue(EventResult.pass());
            return;
        }

        ServerQuestFile questFile = questKillTask$questProcessor.getQuestFile();
        if (questFile == null) {
            cir.setReturnValue(EventResult.pass());
            return;
        }

        TeamData killerTeam = questKillTask$questProcessor.getKillerTeam(source, questFile);
        questKillTask$questProcessor.processDamagingTeams(entity, questFile, killerTeam);

        DamageTracker.clearEntityTracking(entity);
        cir.setReturnValue(EventResult.pass());
    }
}