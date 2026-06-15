import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import versioning.BuildConfig

plugins {
    id("com.gradleup.shadow")
}

tasks.named<ShadowJar>("shadowJar") {
    minimize()
    archiveFileName = "${rootProject.name}-${project.name}-${rootProject.version}.jar"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    if (BuildConfig.relocate) {
        if (BuildConfig.shadePE) {
            relocate("io.github.retrooper.packetevents", "com.grimnatorac.shaded.io.github.retrooper.packetevents")
            relocate("com.github.retrooper.packetevents", "com.grimnatorac.shaded.com.github.retrooper.packetevents")
            relocate("net.kyori", "com.grimnatorac.shaded.kyori") // use PE's built-in adventure instead when not shading PE
        }
        relocate("club.minnced", "com.grimnatorac.shaded.discord-webhooks")
        relocate("org.slf4j", "com.grimnatorac.shaded.slf4j") // Required by discord-webhooks
        relocate("github.scarsz.configuralize", "com.grimnatorac.shaded.configuralize")
        relocate("com.github.puregero", "com.grimnatorac.shaded.com.github.puregero")
        relocate("com.google.code.gson", "com.grimnatorac.shaded.gson")
        relocate("alexh", "com.grimnatorac.shaded.maps")
        relocate("it.unimi.dsi.fastutil", "com.grimnatorac.shaded.fastutil")
        relocate("okhttp3", "com.grimnatorac.shaded.okhttp3")
        relocate("okio", "com.grimnatorac.shaded.okio")
        relocate("org.yaml.snakeyaml", "com.grimnatorac.shaded.snakeyaml")
        relocate("org.json", "com.grimnatorac.shaded.json")
        relocate("org.intellij", "com.grimnatorac.shaded.intellij")
        relocate("org.jetbrains", "com.grimnatorac.shaded.jetbrains")
        relocate("org.incendo", "com.grimnatorac.shaded.incendo")
        relocate("io.leangen.geantyref", "com.grimnatorac.shaded.geantyref") // Required by cloud
        relocate("com.zaxxer", "com.grimnatorac.shaded.zaxxer") // Database history
    }
    mergeServiceFiles()
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}
