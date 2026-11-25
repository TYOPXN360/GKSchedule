# Add project specific ProGuard rules here.
-keepclassmembers class com.classapp.schedule.data.** { *; }
-keep @androidx.room.Entity class * { *; }

# ponytail: ONNX Runtime 的 JNI 层按类名反射取 ai.onnxruntime.* （OrtSession.run 里 GetMethodID），
# 一旦被混淆 java_class 就是 null，JNI 直接 abort 整个进程（不是抛异常，是 native crash）。
# 报错形态：JNI DETECTED ERROR IN APPLICATION: java_class == null / in call to GetMethodID
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**
