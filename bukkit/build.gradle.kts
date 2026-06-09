import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission
import versioning.BuildConfig

plugins {
    `maven-publish`
    grimnatorac.`base-conventions`
    grimnatorac.`shadow-conventions`
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
    id("xyz.jpenilla.run-paper") version "3.0.0-beta.1"
}

val proguardClasspath: Configuration by configurations.creating

repositories {
    if (BuildConfig.mavenLocalOverride) mavenLocal()

    exclusive("https://repo.papermc.io/repository/maven-public/", { name = "papermc" }) {
        includeGroup("io.papermc.paper")
        includeGroup("net.md-5")
    }

    exclusive("https://libraries.minecraft.net", { mavenContent { releasesOnly() } }) {
        includeModule("com.mojang", "brigadier")
    }

    exclusive("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        includeGroup("me.clip")
    }

    exclusive("https://repo.grim.ac/snapshots") {
        includeGroup("ac.grim.grimac")
        includeGroup("com.github.retrooper")
    }

    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }

    mavenCentral()
}

dependencies {
    proguardClasspath("com.guardsquare:proguard-base:7.9.1")

    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)

    if (BuildConfig.shadePE) {
         implementation(libs.packetevents.spigot)
    } else {
        compileOnly(libs.packetevents.spigot)
    }
    implementation(libs.cloud.paper)
    implementation(libs.adventure.platform.bukkit)
    implementation(libs.grim.bukkit.internal)

    implementation(project(":common"))
    shadow(project(":common"))

    // Test dependencies
    testImplementation(testlibs.junitJupiter)
    testRuntimeOnly(testlibs.junitPlatformLauncher)
}

bukkit {
    name = "GrimnatorAC"
    author = "GrimnatorAC"
    main = "com.grimnatorac.platform.bukkit.GrimnatorACBukkitLoaderPlugin"
    website = "https://grim.ac/"
    apiVersion = "1.13"
    foliaSupported = true

    if (!BuildConfig.shadePE) {
        depend = listOf("packetevents")
    }

    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "Essentials",
        "ViaVersion",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot",
        "floodgate",
        "FastLogin",
        "PlaceholderAPI",
        "sqlite-jdbc",
        "mysql-jdbc",
        "postgresql-jdbc",
        "mongodb-driver",
        "jedis",
    )

    commands {
        register("exploitcheck") {
            description = "Manually trigger Translation Key Probe exploit detection"
            usage = "/exploitcheck"
            permission = "grimnatorac.exploitcheck"
        }
    }

    permissions {
        register("grimnatorac.exploitcheck") {
            description = "Use /exploitcheck command to manually test probe"
            default = Permission.Default.OP
        }
        register("grimnatorac.alerts") {
            description = "Receive alerts for violations"
            default = Permission.Default.OP
        }
        register("grimnatorac.alerts.enable-on-join") {
             description = "Enable alerts on join"
            default = Permission.Default.OP
        }
        register("grimnatorac.performance") {
            description = "Check performance metrics"
            default = Permission.Default.OP
        }
        register("grimnatorac.profile") {
             description = "Check user profile"
            default = Permission.Default.OP
        }
        register("grimnatorac.brand") {
            description = "Show client brands on join"
            default = Permission.Default.OP
        }
        register("grimnatorac.brand.enable-on-join") {
             description = "Enable showing client brands on join"
            default = Permission.Default.OP
        }
        register("grimnatorac.sendalert") {
            description = "Send cheater alert"
            default = Permission.Default.OP
        }
        register("grimnatorac.nosetback") {
            description = "Disable setback"
            default = Permission.Default.FALSE
        }
        register("grimnatorac.nomodifypacket") {
            description = "Disable modifying packets"
            default = Permission.Default.FALSE
        }
        register("grimnatorac.exempt") {
             default = Permission.Default.FALSE
        }
        register("grimnatorac.verbose") {
            description = "Receive verbose alerts for violations"
            default = Permission.Default.OP
        }
        register("grimnatorac.verbose.enable-on-join") {
            description = "Enable verbose alerts on join"
            default = Permission.Default.FALSE
        }
        register("grimnatorac.list") {
            description = "Shows lists of specific data"
            default = Permission.Default.FALSE
        }
    }
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["shadowJar"])
}

tasks {
    test {
        useJUnitPlatform()
    }

    runServer {
        val javaToolchains = project.extensions.getByType<JavaToolchainService>()
        javaLauncher = javaToolchains.launcherFor {
            vendor = JvmVendorSpec.JETBRAINS
            languageVersion = JavaLanguageVersion.of(25)
        }
        systemProperties(mapOf("paper.explicit-flush" to "true"))
        minecraftVersion("26.1.2")
    }

    shadowJar {
        exclude("META-INF/services/javax.annotation.processing.Processor")
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
    }

    register<JavaExec>("obfuscate") {
        dependsOn(shadowJar)

        classpath(proguardClasspath)
        mainClass.set("proguard.ProGuard")

        val inputJar = shadowJar.get().archiveFile.get().asFile.absolutePath
        val outputJar = layout.buildDirectory.file("libs/${project.name}-${project.version}-obfuscated.jar").get().asFile.absolutePath
        val javaHome = System.getProperty("java.home")

        val argsList = mutableListOf(
            "-injars", inputJar,
            "-outjars", outputJar,
            "-libraryjars", "$javaHome/jmods"
        )

        // BURASI DÜZELDİ: Tüm kütüphaneler yerine sadece compileOnly olan (Paper API vb.)
        // dış kütüphaneleri süzüp ProGuard'a library olarak paslıyoruz.
        val compileLibs = configurations.compileClasspath.get().files
        val runtimeLibs = configurations.runtimeClasspath.get().files
        val trulyExternalLibs = compileLibs - runtimeLibs

        trulyExternalLibs.forEach { file ->
            if (file.exists() && file.extension == "jar") {
                argsList.add("-libraryjars")
                argsList.add(file.absolutePath)
            }
        }

        argsList.add("-include")
        argsList.add("proguard-rules.pro")

        args(argsList)
    }
}
