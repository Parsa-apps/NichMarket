# Myket Billing Client
-keep class ir.myket.billingclient.** { *; }
-keep interface ir.myket.billingclient.** { *; }
-keep class com.android.vending.billing.** { *; }
-keepclassmembers class ir.myket.billingclient.util.** {
    public <fields>;
    public <methods>;
}

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
dontnote kotlinx.serialization.**
-keepclassmembers class com.parsaapps.nichmarket.**$$serializer { *; }
-keepclassmembers class com.parsaapps.nichmarket.** {
    *** Companion;
}
-keepclasseswithmembers class com.parsaapps.nichmarket.** {
    kotlinx.serialization.KSerializer serializer(...);
}
