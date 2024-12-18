import dev.muon.questkilltask.gradle.Properties
import dev.muon.questkilltask.gradle.Versions
import me.modmuss50.mpp.PublishModTask

plugins {
    id("conventions.common")
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
}

sourceSets {
    create("generated") {
        resources {
            srcDir("src/generated/resources")
        }
    }
}

repositories {
    maven("https://maven.blamejared.com/")
    maven("https://maven.wispforest.io/releases")
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.fabricmc.net")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.ladysnake.org/releases")
    maven("https://maven.parchmentmc.org")
    maven {
        url = uri("https://maven.saps.dev/releases")
        content {
            includeGroup("dev.latvian.mods")
            includeGroup("dev.ftb.mods")
        }
    }
}

neoForge {
    neoFormVersion = Versions.NEOFORM
    parchment {
        minecraftVersion = Versions.PARCHMENT_MINECRAFT
        mappingsVersion = Versions.PARCHMENT
    }
    addModdingDependenciesTo(sourceSets["test"])

    val at = file("src/main/resources/${Properties.MOD_ID}.cfg")
    if (at.exists())
        setAccessTransformers(at)
    validateAccessTransformers = true
}

dependencies {
    compileOnly("io.github.llamalad7:mixinextras-common:${Versions.MIXIN_EXTRAS}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${Versions.MIXIN_EXTRAS}")
    compileOnly("net.fabricmc:sponge-mixin:${Versions.FABRIC_MIXIN}")

    implementation("dev.ftb.mods:ftb-quests:${Versions.FTB_QUESTS}")
    implementation("dev.ftb.mods:ftb-library:${Versions.FTB_LIBRARY}")
    implementation("dev.ftb.mods:ftb-teams:${Versions.FTB_TEAMS}")
}

configurations {
    register("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    register("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    register("commonTestResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

artifacts {
    add("commonJava", sourceSets["main"].java.sourceDirectories.singleFile)
    add("commonResources", sourceSets["main"].resources.sourceDirectories.singleFile)
    add("commonResources", sourceSets["generated"].resources.sourceDirectories.singleFile)
    add("commonTestResources", sourceSets["test"].resources.sourceDirectories.singleFile)
}

publishMods {
    changelog = rootProject.file("CHANGELOG.md").readText()
    displayName = "v${Versions.MOD} (Minecraft ${Versions.MINECRAFT})"
    version = "${Versions.MOD}+${Versions.MINECRAFT}"
    type = STABLE

    github {
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        repository = Properties.GITHUB_REPO
        tagName = "${Versions.MOD}+${Versions.MINECRAFT}"
        commitish = Properties.GITHUB_COMMITISH

        allowEmptyFiles = true
    }
}