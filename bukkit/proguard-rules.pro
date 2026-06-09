# =======================================================================
# GrimnatorAC Tam Stabil ProGuard Kuralları (V2)
# =======================================================================

# 1. UYARILARI SUSTUR
-ignorewarnings
-dontwarn **
-dontnote **

# 2. SADECE OBFUSCATION YAP (Gereksiz kod silme işlemlerini kapat)
-dontshrink
-dontoptimize
-allowaccessmodification

# 3. METADATA VE NİTELİKLERİ KORU
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses,LineNumberTable,SourceFile,LocalVariableTable,LocalVariableTypeTable

# 4. BUKKIT ANA YÜKLEYİCİSİNİ KORU
-keep class com.grimnatorac.platform.bukkit.GrimnatorACBukkitLoaderPlugin { *; }

# 5. GÖMÜLÜ (SHADED) KÜTÜPHANELERİ KORU
-keep class com.grimnatorac.shaded.** { *; }

# 6. KRİTİK: LMAX DISRUPTOR KÜTÜPHANESİNİ KORU
# Bu kütüphane AtomicFieldUpdater (String ile değişken arama) kullandığı için
# hem sınıfların hem de içindeki field/metotların isimleri aynen korunmalıdır.
-keep class com.lmax.disruptor.** { *; }
-keepclassmembers class com.lmax.disruptor.** { *; }

# 7. API PAKETLERİNİ KORU (Eklentiler arası event iletişimi için)
-keep class ac.grim.grimac.api.** { *; }
-keep class com.grimnatorac.api.** { *; }

# 8. ENUM YAPILARINI KORU
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep enum * { *; }

# 9. EVENT HANDLER METOTLARINI KORU
-keepclassmembers class * {
    @org.bukkit.event.EventHandler <methods>;
}