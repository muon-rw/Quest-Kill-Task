package dev.muon.questkilltask;

import net.fabricmc.api.ModInitializer;

public class QuestKillTaskFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        QuestKillTask.LOG.info("Hello Fabric world!");
        QuestKillTask.init();
    }
}
