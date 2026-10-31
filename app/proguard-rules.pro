# Keep Attributes for Reflection & Generic Signatures
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod,InnerClasses

# Keep Data Entities & Models
-keep class com.example.smsforwarder.data.** { *; }
-keepclassmembers class com.example.smsforwarder.data.** { *; }

-keep class com.example.smsforwarder.rules.** { *; }
-keepclassmembers class com.example.smsforwarder.rules.** { *; }

# Keep Room Database
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-dontwarn androidx.room.**

# Keep WorkManager Workers
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.smsforwarder.delivery.DeliveryWorker { *; }

# Keep Gson Classes & Reflection
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.code.gson.annotations.SerializedName <fields>;
}

# Keep Enums
-keepclassmembers enum * { *; }
