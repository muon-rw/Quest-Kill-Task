package dev.muon.questkilltask;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(QuestKillTask.MOD_ID)
public class QuestKillTaskNeoforge {

    public QuestKillTaskNeoforge(IEventBus eventBus) {
        QuestKillTask.init();

        // Iron's Spells compat — disabled, not yet ported to 26.1.2.
        // if (ModList.get().isLoaded("irons_spellbooks")) {
        //     QuestKillTask.LOG.info("Registering Iron's Spells 'n Spellbooks event handler.");
        //     IronSpellsEventHandler.register();
        // }
    }
}
