package dev.muon.questkilltask;

import net.fabricmc.api.ModInitializer;

public class QuestKillTaskFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        QuestKillTask.init();

        // Spell Engine compat — disabled, not yet ported to 26.1.2.
        // if (FabricLoader.getInstance().isModLoaded("spell_engine")) {
        //     QuestKillTask.LOG.info("Registering Spell Engine Heal event listener.");
        //     SpellEngineEventHandler.register();
        // }
    }
}
