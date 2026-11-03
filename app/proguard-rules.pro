# Regras oficiais R8/ProGuard para gerar native/obfuscation symbol table na Google Play Console
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Preserva classes de Billing e modelos de domínio
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.api.** { *; }
-keep class com.rdf112.sweep.** { *; }
-keep class com.rdf112.sweep.domain.** { *; }
