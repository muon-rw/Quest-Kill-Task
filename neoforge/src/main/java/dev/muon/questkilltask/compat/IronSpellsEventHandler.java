package dev.muon.questkilltask.compat;

import dev.muon.questkilltask.DamageTracker;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

public class IronSpellsEventHandler {

    public static void register() {
        NeoForge.EVENT_BUS.register(IronSpellsEventHandler.class);
    }

    @SubscribeEvent
    public static void onSpellHeal(SpellHealEvent event) {
        LivingEntity caster = event.getEntity();
        LivingEntity target = event.getTargetEntity();
        if (caster != null && target != null && !caster.level().isClientSide()) {
            DamageTracker.trackHealing(target, caster.getUUID());
        }
    }
} 