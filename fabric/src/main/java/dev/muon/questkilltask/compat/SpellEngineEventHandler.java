package dev.muon.questkilltask.compat;

import dev.muon.questkilltask.DamageTracker;
import dev.muon.questkilltask.QuestKillTask;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.spell.event.SpellEvents;

public class SpellEngineEventHandler {

    private static final SpellEngineEventHandler INSTANCE = new SpellEngineEventHandler();

    public static void register() {
        SpellEvents.HEAL.register(INSTANCE::onSpellHeal);
    }

    private void onSpellHeal(SpellEvents.HealEvent.Args args) {
        LivingEntity caster = args.caster();
        LivingEntity target = args.target();
        if (caster != null && target != null && !caster.level().isClientSide()) {
            DamageTracker.trackHealing(target, caster.getUUID());
        }
    }
} 