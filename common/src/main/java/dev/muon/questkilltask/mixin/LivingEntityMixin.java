package dev.muon.questkilltask.mixin;

import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.muon.questkilltask.DamageTracker;
import dev.muon.questkilltask.QuestKillTask;
import dev.muon.questkilltask.QuestProcessor;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private static QuestProcessor questKillTask$processor;

    /**
     * Death pipeline hook. Fires for every entity death — including command kills, fall damage,
     * starvation, etc. — so contributors with no final-blow credit (damager, healer, tank) still
     * get task progress. The killer's team (when applicable) is intentionally left to FTB's own
     * onPlayerKilledEntity hook; QuestProcessor pre-marks it to avoid double-crediting.
     */
    @Inject(method = "die", at = @At("HEAD"), remap = false)
    private void questKillTask$onDie(DamageSource source, CallbackInfo ci) {
        LivingEntity victim = (LivingEntity) (Object) this;
        if (victim.level().isClientSide()) {
            return;
        }

        if (questKillTask$processor == null) {
            questKillTask$processor = new QuestProcessor();
        }

        if (!questKillTask$processor.shouldProcessKill(victim)) {
            return;
        }

        ServerQuestFile questFile = questKillTask$processor.getQuestFile();
        if (questFile == null) {
            QuestKillTask.LOG.warn("Unable to retrieve Server Quest File!");
            return;
        }

        questKillTask$processor.processDamagingTeams(victim, questFile, source);
        DamageTracker.clearEntityTracking(victim);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), remap = false)
    private void onDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity victim = (LivingEntity) (Object) this;
        if (victim.level().isClientSide()) {
            return;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            DamageTracker.trackDamage(victim, player.getUUID());
            return;
        }

        if (source.getEntity() instanceof TamableAnimal tameable && tameable.getOwner() instanceof ServerPlayer owner) {
            DamageTracker.trackDamage(victim, owner.getUUID());
            return;
        }

        if (victim instanceof ServerPlayer player && source.getEntity() instanceof LivingEntity attacker) {
            DamageTracker.trackDamageTaken(attacker, player.getUUID());
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), remap = false)
    private void onEffectAdded(MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!(source instanceof ServerPlayer supporter)) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) {
            return;
        }

        Holder<MobEffect> effectHolder = effectInstance.getEffect();
        boolean isEffectivelyBeneficial = questKillTask$isEffectBeneficialForTarget(effectHolder, target);

        if (isEffectivelyBeneficial) {
            DamageTracker.trackHealing(target, supporter.getUUID());
        } else {
            DamageTracker.trackDamage(target, supporter.getUUID());
        }
    }

    @Unique
    private boolean questKillTask$isEffectBeneficialForTarget(Holder<MobEffect> effectHolder, LivingEntity target) {
        boolean isBeneficial = effectHolder.value().isBeneficial();

        if (effectHolder.is(MobEffects.INSTANT_HEALTH) || effectHolder.is(MobEffects.INSTANT_DAMAGE)) {
            isBeneficial = isBeneficial != target.isInvertedHealAndHarm();
        }

        return isBeneficial;
    }
}
