plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    
    // 🚨 MASTER FIX: 15.0.0 सर्वर पर नहीं है, इसलिए स्टेबल 14.0.2 यूज़ कर रहे हैं।
    id("com.chaquo.python") version "14.0.2" apply false
}
