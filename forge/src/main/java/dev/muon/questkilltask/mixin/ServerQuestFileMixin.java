package dev.muon.questkilltask.mixin;

import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.muon.questkilltask.QuestKillTask;
import dev.muon.questkilltask.QuestProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerQuestFile.class, remap = false)
public class ServerQuestFileMixin {

    @Inject(method = "markDirty", at = @At("TAIL"))
    private void onMarkDirty(CallbackInfo ci) {
        QuestProcessor.invalidateKillTasks();
    }
}