plugins {
    id("net.neoforged.moddev.legacyforge")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

val localJeiDirectory = file("run/dependencies")
repositories { flatDir { dirs(localJeiDirectory) } }

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    // Legacy Forge must remap the published JEI jar before the development client loads it.
    if (localJeiDirectory.resolve("jei-1.20.1-forge-15.56.0.205.jar").isFile) {
        add("modRuntimeOnly", "mezz.jei:jei-1.20.1-forge:15.56.0.205")
    }
}

mixin {
    add(sourceSets.main.get(), "tabletop_stuff.refmap.json")
    config("tabletop_stuff.forge.mixins.json")
}

legacyForge {
    version = "1.20.1-${property("loader.version")}"
    runs {
        register("client") {
            client()
            systemProperty("tabletop_stuff.debug", "true")
            systemProperty("tabletop_stuff.devSkin", "true")
            gameDirectory = file("run")
        }
    }
    mods {
        register("tabletop_stuff") { sourceSet(sourceSets.main.get()) }
    }
}

apply(from = rootProject.file("build.common.gradle"))

tasks.processResources {
    exclude("META-INF/neoforge.mods.toml", "tabletop_stuff.mixins.json")
}

tasks.named("createMinecraftArtifacts") { dependsOn("stonecutterGenerate") }
tasks.jar {
    manifest.attributes["MixinConfigs"] = "tabletop_stuff.forge.mixins.json"
    finalizedBy("reobfJar")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    dependsOn("build", "reobfJar")
    from(tasks.named<Jar>("reobfJar").flatMap { it.archiveFile })
    into(rootProject.file("builds/1.20.1/Forge"))
}
