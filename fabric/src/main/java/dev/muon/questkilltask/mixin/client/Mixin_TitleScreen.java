package dev.muon.questkilltask.mixin.client;

import dev.muon.questkilltask.QuestKillTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class Mixin_TitleScreen {
    
    @Inject(at = @At("HEAD"), method = "init()V")
    private void init(CallbackInfo info) {
        QuestKillTask.LOG.info("This line is printed by an example mod mixin from Fabric!");
        QuestKillTask.LOG.info("MC Version: {}", Minecraft.getInstance().getVersionType());
    }
}