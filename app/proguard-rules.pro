# Handy — ProGuard keep rules (expand with Hilt / ONNX / JNI as features land)
-keepattributes *Annotation*
-dontwarn javax.annotation.**

# ONNX Runtime (ECAPA / Silero / anti-spoof) — release R8 jinak rozbije OrtSession.SessionOptions
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
