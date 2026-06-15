-injars bukkit/build/libs/GrimnatorAC-bukkit.jar
-outjars bukkit/build/libs/GrimnatorAC-obf.jar

-dontoptimize
-dontpreverify

-repackageclasses ''
-overloadaggressively
-useuniqueclassmembernames

-keep public class com.grimnatorac.platform.bukkit.GrimnatorACBukkitLoaderPlugin {
    *;
}

-keep class org.bukkit.** { *; }
-keep class io.papermc.** { *; }