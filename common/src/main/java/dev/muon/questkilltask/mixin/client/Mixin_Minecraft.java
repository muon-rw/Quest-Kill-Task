package dev.muon.questkilltask.mixin.client;

import dev.muon.questkilltask.QuestKillTask;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_Minecraft {
    
    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(CallbackInfo info) {
        QuestKillTask.LOG.info("This line is printed by an example mod common mixin!");
        QuestKillTask.LOG.info("MC Version: {}", Minecraft.getInstance().getVersionType());
    }
}