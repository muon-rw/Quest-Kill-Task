package dev.muon.questkilltask;

import dev.muon.questkilltask.platform.ExamplePlatformHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class QuestKillTask {
    public static final String MOD_ID = "questkilltask";
    public static final String MOD_NAME = "Quest Kill Task";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private static ExamplePlatformHelper helper;
    private static BiConsumer<LivingEntity, DamageSource> deathListener = (victim, source) -> {};

    public static void init() {

    }

    public static void setDeathListener(BiConsumer<LivingEntity, DamageSource> listener) {
        deathListener = listener;
    }

    public static void onEntityDeath(LivingEntity victim, DamageSource source) {
        deathListener.accept(victim, source);
    }

    public static ExamplePlatformHelper getHelper() {
        return helper;
    }

    public static void setHelper(ExamplePlatformHelper helper) {
        QuestKillTask.helper = helper;
    }
}