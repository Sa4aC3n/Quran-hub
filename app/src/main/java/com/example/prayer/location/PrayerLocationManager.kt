package com.example.prayer.location

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.example.prayer.model.LocationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Manages device GPS location detection, reverse geocoding, and built-in offline Islamic cities catalog.
 */
class PrayerLocationManager(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("prayer_location_prefs", Context.MODE_PRIVATE)
    private val locationManager: LocationManager? = try {
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    } catch (e: Exception) {
        null
    }

    data class CityPreset(
        val nameAr: String,
        val countryAr: String,
        val latitude: Double,
        val longitude: Double,
        val defaultMethodId: String
    )

    companion object {
        val PRESET_CITIES = listOf(
            CityPreset("القاهرة", "مصر", 30.0444, 31.2357, "egypt"),
            CityPreset("الإسكندرية", "مصر", 31.2001, 29.9187, "egypt"),
            CityPreset("الجيزة", "مصر", 30.0131, 31.2089, "egypt"),
            CityPreset("المنصورة", "مصر", 31.0409, 31.3785, "egypt"),
            CityPreset("طنطا", "مصر", 30.7865, 31.0004, "egypt"),
            CityPreset("أسيوط", "مصر", 27.1801, 31.1837, "egypt"),
            CityPreset("أسوان", "مصر", 24.0889, 32.8998, "egypt"),
            CityPreset("مكة المكرمة", "المملكة العربية السعودية", 21.4225, 39.8262, "makkah"),
            CityPreset("المدينة المنورة", "المملكة العربية السعودية", 24.5247, 39.5692, "makkah"),
            CityPreset("الرياض", "المملكة العربية السعودية", 24.7136, 46.6753, "makkah"),
            CityPreset("جدة", "المملكة العربية السعودية", 21.4858, 39.1925, "makkah"),
            CityPreset("الدمام", "المملكة العربية السعودية", 26.4207, 50.0888, "makkah"),
            CityPreset("دبي", "الإمارات العربية المتحدة", 25.2048, 55.2708, "dubai"),
            CityPreset("أبوظبي", "الإمارات العربية المتحدة", 24.4539, 54.3773, "dubai"),
            CityPreset("الشارقة", "الإمارات العربية المتحدة", 25.3463, 55.4209, "dubai"),
            CityPreset("مدينة الكويت", "الكويت", 29.3759, 47.9774, "kuwait"),
            CityPreset("الدوحة", "قطر", 25.2854, 51.5310, "qatar"),
            CityPreset("المنامة", "البحرين", 26.2285, 50.5860, "makkah"),
            CityPreset("مسقط", "سلطنة عمان", 23.5859, 58.4059, "makkah"),
            CityPreset("القدس الشريف", "فلسطين", 31.7683, 35.2137, "egypt"),
            CityPreset("غزة", "فلسطين", 31.5017, 34.4668, "egypt"),
            CityPreset("عمّان", "الأردن", 31.9454, 35.9284, "egypt"),
            CityPreset("بيروت", "لبنان", 33.8938, 35.5018, "egypt"),
            CityPreset("دمشق", "سوريا", 33.5138, 36.2765, "egypt"),
            CityPreset("حلب", "سوريا", 36.2021, 37.1343, "egypt"),
            CityPreset("بغداد", "العراق", 33.3152, 44.3661, "karachi"),
            CityPreset("البصرة", "العراق", 30.5081, 47.7835, "karachi"),
            CityPreset("أربيل", "العراق", 36.1911, 44.0092, "karachi"),
            CityPreset("طرابلس", "ليبيا", 32.8872, 13.1913, "egypt"),
            CityPreset("بنغازي", "ليبيا", 32.1167, 20.0667, "egypt"),
            CityPreset("تونس", "تونس", 36.8065, 10.1815, "mwl"),
            CityPreset("الجزائر", "الجزائر", 36.7538, 3.0588, "mwl"),
            CityPreset("الرباط", "المغرب", 34.0209, -6.8416, "mwl"),
            CityPreset("الدار البيضاء", "المغرب", 33.5731, -7.5898, "mwl"),
            CityPreset("مراكش", "المغرب", 31.6295, -7.9811, "mwl"),
            CityPreset("الخرطوم", "السودان", 15.5007, 32.5599, "egypt"),
            CityPreset("نواكشوط", "موريتانيا", 18.0735, -15.9582, "mwl"),
            CityPreset("صنعاء", "اليمن", 15.3694, 44.1910, "makkah"),
            CityPreset("إسطنبول", "تركيا", 41.0082, 28.9784, "diyanet"),
            CityPreset("أنقرة", "تركيا", 39.9334, 32.8597, "diyanet"),
            CityPreset("جاكرتا", "إندونيسيا", -6.2088, 106.8456, "mwl"),
            CityPreset("كوالالمبور", "ماليزيا", 3.1390, 101.6869, "mwl"),
            CityPreset("لندن", "المملكة المتحدة", 51.5074, -0.1278, "mwl"),
            CityPreset("باريس", "فرنسا", 48.8566, 2.3522, "mwl"),
            CityPreset("نيويورك", "الولايات المتحدة", 40.7128, -74.0060, "isna")
        )
    }

    private val _currentLocation = MutableStateFlow(loadSavedLocation())
    val currentLocation: StateFlow<LocationInfo> = _currentLocation.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private fun loadSavedLocation(): LocationInfo {
        val lat = prefs.getFloat("lat", 30.0444f).toDouble()
        val lng = prefs.getFloat("lng", 31.2357f).toDouble()
        val city = prefs.getString("city", "القاهرة") ?: "القاهرة"
        val country = prefs.getString("country", "مصر") ?: "مصر"
        val isAuto = prefs.getBoolean("is_auto", true)
        return LocationInfo(
            latitude = lat,
            longitude = lng,
            cityName = city,
            countryName = country,
            isAutoGps = isAuto
        )
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    suspend fun refreshCurrentLocationGps(): LocationInfo = withContext(Dispatchers.IO) {
        _isLocating.value = true
        try {
            if (!hasLocationPermission()) {
                _isLocating.value = false
                return@withContext _currentLocation.value
            }

            var bestLocation: Location? = null

            val lm = locationManager
            if (lm != null) {
                // 1. Try GPS Provider
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    try {
                        val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (gpsLoc != null) bestLocation = gpsLoc
                    } catch (e: SecurityException) { /* ignore */ }
                }

                // 2. Try Network Provider
                if (bestLocation == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    try {
                        val netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        if (netLoc != null) bestLocation = netLoc
                    } catch (e: SecurityException) { /* ignore */ }
                }

                // 3. Try Passive
                if (bestLocation == null) {
                    try {
                        val passLoc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                        if (passLoc != null) bestLocation = passLoc
                    } catch (e: SecurityException) { /* ignore */ }
                }
            }

            val lat = bestLocation?.latitude ?: _currentLocation.value.latitude
            val lng = bestLocation?.longitude ?: _currentLocation.value.longitude

            // Reverse Geocoding
            val geoResult = reverseGeocode(lat, lng)
            val updated = LocationInfo(
                latitude = lat,
                longitude = lng,
                cityName = geoResult.first,
                countryName = geoResult.second,
                isAutoGps = true
            )

            saveLocation(updated)
            _currentLocation.value = updated
            _isLocating.value = false
            return@withContext updated
        } catch (e: Exception) {
            _isLocating.value = false
            return@withContext _currentLocation.value
        }
    }

    fun setManualCity(cityPreset: CityPreset) {
        val updated = LocationInfo(
            latitude = cityPreset.latitude,
            longitude = cityPreset.longitude,
            cityName = cityPreset.nameAr,
            countryName = cityPreset.countryAr,
            isAutoGps = false
        )
        saveLocation(updated)
        _currentLocation.value = updated
    }

    fun setCustomCoordinates(lat: Double, lng: Double, cityName: String, countryName: String) {
        val updated = LocationInfo(
            latitude = lat,
            longitude = lng,
            cityName = cityName,
            countryName = countryName,
            isAutoGps = false
        )
        saveLocation(updated)
        _currentLocation.value = updated
    }

    private fun saveLocation(info: LocationInfo) {
        prefs.edit()
            .putFloat("lat", info.latitude.toFloat())
            .putFloat("lng", info.longitude.toFloat())
            .putString("city", info.cityName)
            .putString("country", info.countryName)
            .putBoolean("is_auto", info.isAutoGps)
            .apply()
    }

    private fun reverseGeocode(lat: Double, lng: Double): Pair<String, String> {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(appContext, Locale("ar"))
                val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "موقعي الحالي"
                    val country = addr.countryName ?: "المنطقة"
                    return Pair(city, country)
                }
            }
        } catch (e: Exception) {
            // Fallback to closest preset city
        }

        // Find closest preset city by euclidean distance
        var closest = PRESET_CITIES.first()
        var minDist = Double.MAX_VALUE
        for (city in PRESET_CITIES) {
            val dLat = city.latitude - lat
            val dLng = city.longitude - lng
            val dist = dLat * dLat + dLng * dLng
            if (dist < minDist) {
                minDist = dist
                closest = city
            }
        }
        return Pair(closest.nameAr, closest.countryAr)
    }
}
