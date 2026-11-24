# Settings discover annotated properties and actions through Kotlin reflection.
# Storage and adapters also instantiate first-party classes through reflection.
-keep class com.futo.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keep class kotlin.Metadata { *; }
