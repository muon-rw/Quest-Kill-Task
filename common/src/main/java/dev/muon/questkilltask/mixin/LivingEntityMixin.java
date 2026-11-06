package dev.muon.questkilltask.mixin;

import dev.muon.questkilltask.DamageTracker;
import dev.muon.questkilltask.PlayerAttribution;
import dev.muon.questkilltask.QuestKillTask;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * Runs for every death, including ones with no attacker, so damagers, healers and tanks get credit without
     * relying on the killing blow. We wait for the {@code dead = true} store rather than HEAD so a death that
     * NeoForge's LivingDeathEvent cancels (totems, revives, boss phases) or a repeat {@code die} call is ignored.
     */
    @Inject(method = "die", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;dead:Z", opcode = Opcodes.PUTFIELD))
    private void questKillTask$creditContributorsOnDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity victim = (LivingEntity) (Object) this;
        if (!victim.level().isClientSide()) {
            QuestKillTask.onEntityDeath(victim, source);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"))
    private void questKillTask$trackDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity victim = (LivingEntity) (Object) this;
        if (victim.level().isClientSide()) {
            return;
        }

        ServerPlayer attackingPlayer = PlayerAttribution.resolvePlayer(source.getEntity());
        if (attackingPlayer == null) {
            attackingPlayer = PlayerAttribution.resolvePlayer(source.getDirectEntity());
        }
        if (attackingPlayer != null) {
            DamageTracker.trackDamage(victim, attackingPlayer.getUUID());
            return;
        }

        if (victim instanceof ServerPlayer player && source.getEntity() instanceof LivingEntity attacker) {
            DamageTracker.trackDamageTaken(attacker, player.getUUID());
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void questKillTask$trackEffect(MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer supporter = PlayerAttribution.resolvePlayer(source);
        if (supporter == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) {
            return;
        }

        if (questKillTask$isEffectBeneficialForTarget(effectInstance.getEffect(), target)) {
            DamageTracker.trackHealing(target, supporter.getUUID());
        } else {
            DamageTracker.trackDamage(target, supporter.getUUID());
        }
    }

    @Unique
    private boolean questKillTask$isEffectBeneficialForTarget(Holder<MobEffect> effect, LivingEntity target) {
        boolean isBeneficial = effect.value().isBeneficial();

        if (effect.is(MobEffects.HEAL) || effect.is(MobEffects.HARM)) {
            isBeneficial = isBeneficial != target.isInvertedHealAndHarm();
        }

        return isBeneficial;
    }
}
