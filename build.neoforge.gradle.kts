plugins {
    id("net.neoforged.moddev")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = property("loader.version") as String
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
    exclude("META-INF/mods.toml")
}

tasks.named("createMinecraftArtifacts") { dependsOn("stonecutterGenerate") }

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    dependsOn("build")
    from(tasks.jar.flatMap { it.archiveFile })
    into(rootProject.file("builds/1.21.1/NeoForge"))
}
