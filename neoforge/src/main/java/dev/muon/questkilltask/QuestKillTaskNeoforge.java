package dev.muon.questkilltask;

import dev.muon.questkilltask.compat.IronSpellsEventHandler;
import dev.muon.questkilltask.mixin.LivingEntityAccessor;
import dev.muon.questkilltask.platform.ExamplePlatformHelperNeoForge;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@Mod(QuestKillTask.MOD_ID)
public class QuestKillTaskNeoforge {

    public QuestKillTaskNeoforge(IEventBus eventBus) {
        QuestKillTask.init();
        QuestKillTask.setHelper(new ExamplePlatformHelperNeoForge());
        QuestKillTask.setDeathListener(QuestProcessor::onEntityDeath);

        // Some mods (such as Cataclysm) copy-paste LivingEntity#die without calling super.die, so our common
        // LivingEntity#die mixin never runs for them; they still invoke CommonHooks.onLivingDeath.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, QuestKillTaskNeoforge::creditContributorsOnLivingDeath);

        if (ModList.get().isLoaded("irons_spellbooks")) {
            QuestKillTask.LOG.info("Registering Iron's Spells 'n Spellbooks event handler.");
            IronSpellsEventHandler.register();
        }
    }

    private static void creditContributorsOnLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.level().isClientSide()
                && !victim.isRemoved()
                && !((LivingEntityAccessor) victim).questKillTask$isDead()) {
            QuestKillTask.onEntityDeath(victim, event.getSource());
        }
    }
}
