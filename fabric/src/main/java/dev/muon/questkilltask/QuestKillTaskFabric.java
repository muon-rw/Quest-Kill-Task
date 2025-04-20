package dev.muon.questkilltask;

import dev.muon.questkilltask.compat.SpellEngineEventHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class QuestKillTaskFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        QuestKillTask.init();

        if (FabricLoader.getInstance().isModLoaded("spell_engine")) {
            QuestKillTask.LOG.info("Registering Spell Engine Heal event listener.");
            SpellEngineEventHandler.register();
        } 
    }
}
