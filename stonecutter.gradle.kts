plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.147" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.147" apply false
}

stonecutter active "1.20.1" /* [SC] DO NOT EDIT */

tasks.register("buildAll") {
    group = "build"
    description = "Build and collect both mod JARs."
    dependsOn(":1.20.1:buildAndCollect", ":1.21.1:buildAndCollect")
}
