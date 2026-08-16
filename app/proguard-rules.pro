# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# SQLCipher (kept via its own consumer rules; defense in depth)
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class id.rona.app.**$$serializer { *; }
-keepclassmembers class id.rona.app.** { *** Companion; }
-keepclasseswithmembers class id.rona.app.** { kotlinx.serialization.KSerializer serializer(...); }
