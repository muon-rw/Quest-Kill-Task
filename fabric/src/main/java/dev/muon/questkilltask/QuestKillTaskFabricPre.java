package dev.muon.questkilltask;

import dev.muon.questkilltask.platform.ExamplePlatformHelperFabric;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class QuestKillTaskFabricPre implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        QuestKillTask.setHelper(new ExamplePlatformHelperFabric());
    }
}
