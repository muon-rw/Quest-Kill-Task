package dev.muon.questkilltask;

import dev.muon.questkilltask.compat.IronSpellsEventHandler;
import dev.muon.questkilltask.platform.ExamplePlatformHelperNeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(QuestKillTask.MOD_ID)
public class QuestKillTaskNeoforge {

    public QuestKillTaskNeoforge(IEventBus eventBus) {
        QuestKillTask.init();
        QuestKillTask.setHelper(new ExamplePlatformHelperNeoForge());

        if (ModList.get().isLoaded("irons_spellbooks")) {
            QuestKillTask.LOG.info("Registering Iron's Spells 'n Spellbooks event handler.");
            IronSpellsEventHandler.register();
        } 
    }
}