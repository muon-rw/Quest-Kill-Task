import dev.muon.questkilltask.gradle.Properties
import dev.muon.questkilltask.gradle.Versions
import me.modmuss50.mpp.PublishModTask
import org.apache.tools.ant.filters.LineContains
import org.gradle.jvm.tasks.Jar
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

plugins {
    id("conventions.loader")
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
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
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
    maven {
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven {
        url = uri("https://maven.ftb.dev/snapshots")
        content {
            includeGroup("dev.latvian.mods")
            includeGroup("dev.ftb.mods")
        }
    }
    maven {
        url = uri("https://maven.ftb.dev/releases")
        content {
            includeGroup("dev.latvian.mods")
            includeGroup("dev.ftb.mods")
        }
    }
    maven("https://code.redspace.io/releases")
    maven("https://code.redspace.io/snapshots")
}

dependencies {
    implementation("dev.ftb.mods:ftb-quests-neoforge:${Versions.FTB_QUESTS}")
    implementation("dev.ftb.mods:ftb-library-neoforge:${Versions.FTB_LIBRARY}")
    implementation("dev.ftb.mods:ftb-teams-neoforge:${Versions.FTB_TEAMS}")


    compileOnly("io.redspace:irons_spellbooks:${Versions.IRONS_SPELLS}")
}

neoForge {
    version = Versions.NEOFORGE
    parchment {
        minecraftVersion = Versions.PARCHMENT_MINECRAFT
        mappingsVersion = Versions.PARCHMENT
    }
    addModdingDependenciesTo(sourceSets["test"])

    val at = project(":common").file("src/main/resources/${Properties.MOD_ID}.cfg")
    if (at.exists())
        setAccessTransformers(at)
    validateAccessTransformers = true

    runs {
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
            systemProperty("neoforge.enabledGameTestNamespaces", Properties.MOD_ID)
        }
        create("client") {
            client()
            ideName = "NeoForge Client (:${project.name})"
            gameDirectory.set(file("runs/client"))
            sourceSet = sourceSets["test"]
            jvmArguments.set(setOf("-Dmixin.debug.verbose=true", "-Dmixin.debug.export=true"))
        }
        create("server") {
            server()
            ideName = "NeoForge Server (:${project.name})"
            gameDirectory.set(file("runs/server"))
            programArgument("--nogui")
            sourceSet = sourceSets["test"]
            jvmArguments.set(setOf("-Dmixin.debug.verbose=true", "-Dmixin.debug.export=true"))
        }
    }

    mods {
        register(Properties.MOD_ID) {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["test"])
        }
    }
}

tasks {
    named<ProcessResources>("processResources").configure {
        filesMatching("*.mixins.json") {
            filter<LineContains>("negate" to true, "contains" to setOf("refmap"))
        }
    }
}

publishMods {
    file.set(tasks.named<Jar>("jar").get().archiveFile)
    modLoaders.add("neoforge")
    changelog = rootProject.file("CHANGELOG.md").readText()
    displayName = "Neoforge-${Versions.MOD}+${Versions.MINECRAFT}"
    version = "${Versions.MOD}+${Versions.MINECRAFT}-neoforge"
    type = STABLE

    curseforge {
        projectId = Properties.CURSEFORGE_PROJECT_ID
        accessToken = providers.environmentVariable("CF_TOKEN")

        minecraftVersions.add(Versions.MINECRAFT)
        javaVersions.add(JavaVersion.VERSION_21)

        client = true
        server = true

        requires {
            slug = "ftb-quests-forge"
        }
    }
}

tasks.withType<PublishModTask>().configureEach {
    mustRunAfter(rootProject.subprojects.map { "${it.path}:publishAllPublicationsToMuonRepository" })
}

tasks.register("sendToModpack") {
    group = "publishing"
    val jarTask = tasks.named<Jar>("jar")
    dependsOn(jarTask)
    val builtJar = jarTask.flatMap { it.archiveFile }
    val modsDir = Path.of("C:\\Users\\renpi\\curseforge\\minecraft\\Instances\\Raven's High Fantasy\\mods")
    val versionedPrefix = jarTask.get().archiveFileName.get().substringBefore(project.version.toString())
    val deployedJar = modsDir.resolve("${versionedPrefix}dev.jar")
    doLast {
        // Writing into the deployed jar corrupts a running pack's open handle; replacing the file leaves the pack on the old one.
        val staged = deployedJar.resolveSibling("${deployedJar.fileName}.staged")
        Files.copy(builtJar.get().asFile.toPath(), staged, StandardCopyOption.REPLACE_EXISTING)
        try {
            Files.deleteIfExists(deployedJar)
        } catch (e: FileSystemException) {
            Files.delete(staged)
            throw GradleException("${deployedJar.fileName} is held open by a running pack; close it and rerun", e)
        }
        Files.move(staged, deployedJar)
        println("Mod JAR sent to modpack folder: ${deployedJar.fileName}")

        // FML picks the published jar over the dev jar, so we disable the published one while testing.
        Files.list(modsDir).use { files ->
            files.filter { it.fileName.toString().startsWith(versionedPrefix) && it.fileName.toString().endsWith(".jar") && it != deployedJar }
                .toList()
                .forEach { versioned ->
                    Files.move(versioned, versioned.resolveSibling("${versioned.fileName}.disabled"), StandardCopyOption.REPLACE_EXISTING)
                    println("Disabled published jar: ${versioned.fileName}")
                }
        }
    }
}