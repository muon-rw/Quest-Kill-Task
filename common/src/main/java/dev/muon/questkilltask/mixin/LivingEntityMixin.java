package dev.muon.questkilltask.mixin;

import dev.muon.questkilltask.DamageTracker;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
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
            return;
        }

    }

    @Inject(method = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void onEffectAdded(MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!(source instanceof ServerPlayer supporter)) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) {
            return;
        }

        MobEffect effect = effectInstance.getEffect();
        boolean isEffectivelyBeneficial = isEffectBeneficialForTarget(effect, target);

        if (isEffectivelyBeneficial) {
            DamageTracker.trackHealing(target, supporter.getUUID());
        } else {
            DamageTracker.trackDamage(target, supporter.getUUID());
        }
    }

    @Unique
    private boolean isEffectBeneficialForTarget(MobEffect effect, LivingEntity target) {
        boolean isBeneficial = effect.isBeneficial();

        if (effect == MobEffects.HEAL || effect == MobEffects.HARM) {
            isBeneficial = isBeneficial != target.isInvertedHealAndHarm();
        }

        return isBeneficial;
    }
}