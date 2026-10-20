# youtubedl-android maps yt-dlp JSON with Jackson via reflection.
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn com.fasterxml.jackson.databind.**
-dontwarn java.beans.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.io.**
