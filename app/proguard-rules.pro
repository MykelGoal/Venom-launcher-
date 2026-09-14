# Venom Launcher - keep rules (minify is OFF by default; these are here for when you turn it on)

# AppWidgetHost / framework widget plumbing is all reflection-free but keep names anyway
-keep class com.venom.launcher.data.** { *; }

# kotlinx.serialization
-keepclassmembers class com.venom.launcher.** {
    *** Companion;
}
-keepclasseswithmembers class com.venom.launcher.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.venom.launcher.**$$serializer { *; }

# Device admin receiver is started by the system by name
-keep class com.venom.launcher.receiver.VenomDeviceAdmin { *; }
