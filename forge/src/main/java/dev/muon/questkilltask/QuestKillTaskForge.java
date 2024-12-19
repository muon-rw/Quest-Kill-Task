package dev.muon.questkilltask;

import dev.muon.questkilltask.platform.ForgePlatformHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(QuestKillTask.MOD_ID)
public class QuestKillTaskForge {

    public QuestKillTaskForge() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        QuestKillTask.init();
        QuestKillTask.setHelper(new ForgePlatformHelper());
    }
}