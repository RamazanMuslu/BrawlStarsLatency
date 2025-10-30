// AppVersionInfo.kt dosyası
package com.yorastudio.overlaylatency

/**
 * Uygulamanın sürüm kodunu ve sürüm adını tutan veri sınıfı.
 *
 * @property versionCode Uygulamanın dahili sürüm kodu (Int).
 * @property versionName Uygulamanın kullanıcılara görünen sürüm adı (String).
 */
data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String
)