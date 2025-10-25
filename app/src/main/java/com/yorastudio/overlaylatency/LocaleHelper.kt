package com.yorastudio.overlaylatency

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Uygulamanın dil ayarlarını yöneten yardımcı sınıf.
 * Seçilen dili SharedPreferences'a kaydeder ve Context'i bu dile göre yapılandırır.
 */
object LocaleHelper {

    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"
    private const val PREFS_NAME = "OverlayPrefs" // SharedPreferences adı, OverlayService ile aynı olmalı

    // Kaydedilen dili SharedPreferences'tan alır
    fun getLanguage(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Varsayılan dil olarak 'en' (İngilizce) döndürüyoruz, çünkü values/strings.xml İngilizce olacak
        return prefs.getString(SELECTED_LANGUAGE, "en") ?: "en"
    }

    // Seçilen dili SharedPreferences'a kaydeder
    fun setLanguage(context: Context, language: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SELECTED_LANGUAGE, language).apply()
    }

    // Uygulamanın Context'ini belirli bir dile göre yapılandırır
    // Bu fonksiyon, bir aktivite başladığında veya uygulama ilk açıldığında kullanılır.
    fun onAttach(context: Context): Context {
        val language = getLanguage(context) // Kaydedilen dili al
        return setLocale(context, language) // Context'i bu dile göre ayarla
    }

    // Uygulamanın Context'ini belirli bir dile göre yeniden yapılandırır
    // Bu fonksiyon, dil değiştirildiğinde Context'i güncellemek için kullanılır.
    // Özellikle Service gibi yerlerde yeni bir Context oluşturmak için kullanılır.
    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale) // Varsayılan Locale'i ayarla (genel sistem için)

        val config = Configuration(context.resources.configuration) // Mevcut konfigürasyonu kopyala

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0 (API 24) ve üzeri için
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList) // Varsayılan LocaleList'i ayarla
            config.setLocales(localeList)
        } else {
            // Eski Android sürümleri için
            @Suppress("DEPRECATION") // Eski API kullanımı için uyarıyı gizle
            config.locale = locale
        }

        // Yeni konfigürasyonla yeni bir Context oluştur ve döndür
        return context.createConfigurationContext(config)
    }

    // Servis veya diğer yerlerde güncel kaynaklara erişmek için
    // Bu, dil değiştiğinde doğru stringleri çekmek için kullanılır.
    // Özellikle Service'lerde bu Context'in resources'ına ihtiyacımız olur.
    fun getLocaleAwareResources(context: Context): Resources {
        val language = getLanguage(context)
        val locale = Locale(language)
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return context.createConfigurationContext(config).resources
    }
}