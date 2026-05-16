# Handy — ProGuard keep rules (expand with Hilt / ONNX / JNI as features land)
-keepattributes *Annotation*
-dontwarn javax.annotation.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
