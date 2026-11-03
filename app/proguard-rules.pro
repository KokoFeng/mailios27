# JavaMail / activation use reflection and provider files
-keep class javax.** { *; }
-keep class com.sun.** { *; }
-keep class myjava.** { *; }
-keep class org.apache.harmony.** { *; }
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.security.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn sun.misc.**
# keep line numbers for crash reports, hide original file names
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# libraries referenced by Tink (androidx.security) and others that are not present on Android
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.joda.time.**
-ignorewarnings
