package dev.muon.questkilltask.mixin;

import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.muon.questkilltask.QuestProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BaseQuestFile.class, remap = false)
public class BaseQuestFileMixin {

    @Inject(method = "clearCachedData",
            at = @At(value = "HEAD"))
    private void onMarkDirty(CallbackInfo ci) {
        QuestProcessor.markDirty();
    }
}
