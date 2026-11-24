-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers,allowobfuscation class * {
    @kotlinx.serialization.Serializable <fields>;
    @com.google.gson.annotations.SerializedName <fields>;
}