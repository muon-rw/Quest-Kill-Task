package dev.muon.questkilltask.mixin;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.FTBQuestsEventHandler;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.muon.questkilltask.DamageTracker;
import dev.muon.questkilltask.QuestKillTask;
import dev.muon.questkilltask.QuestProcessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FTBQuestsEventHandler.class, remap = false)
public class FTBQuestsEventHandlerMixin {
    @Unique
    private QuestProcessor questKillTask$questProcessor;

    /**
     * Completely replaces the original playerKill method with enhanced functionality.
     * This implementation adds support for:
     * - Team-based kill credit
     * - Healing/Support credit
     * - Tank/Damage-taken credit
     * - Potion/Effect tracking
     *
     * While this is functionally an @Overwrite and not great for compatibility,
     * It's unlikely other mods interact with this task.
     * This can be rewritten if issues arise.
     */

    // TODO: Clean up, some duplicate logic is being run
    @Inject(method = "playerKill", at = @At("HEAD"), cancellable = true)
    private void onPlayerKill(LivingEntity entity, DamageSource source, CallbackInfoReturnable<EventResult> cir) {
        if (source == null || entity.level().isClientSide || source.getEntity() == null || source.getEntity().level().isClientSide) {
            cir.setReturnValue(EventResult.pass());
            return;
        }
        if (questKillTask$questProcessor == null) {
            questKillTask$questProcessor = new QuestProcessor();
        }

        if (!questKillTask$questProcessor.shouldProcessKill(entity)) {
            cir.setReturnValue(EventResult.pass());
            return;
        }

        ServerQuestFile questFile = questKillTask$questProcessor.getQuestFile();
        if (questFile == null) {
            QuestKillTask.LOG.warn("Unable to retrieve Server Quest File!");
            cir.setReturnValue(EventResult.pass());
            return;
        }

        questKillTask$questProcessor.processDamagingTeams(entity, questFile);

        DamageTracker.clearEntityTracking(entity);
        cir.setReturnValue(EventResult.pass());
    }
}