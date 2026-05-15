package ru.raydroid.plugin.impl.weather.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.ui.Icon
import kotlin.math.roundToInt

@Serializable
internal data class WeatherState(
    val apiKey: String = "",
    val city: City? = null,
    val weather: WeatherSnapshot? = null
)

@Serializable
internal data class City(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
) {
    fun itemId(): CommandItemId = CommandItemId(
        "weather.city.${name.normalizeItemId()}.${country.orEmpty().normalizeItemId()}." +
            "${admin1.orEmpty().normalizeItemId()}.${latitude.formatCoordinate()}.${longitude.formatCoordinate()}"
    )

    fun displayName(): String =
        listOfNotNull(name, admin1?.takeIf { it != name }, country)
            .joinToString(", ")

    fun subtitle(): String =
        "${latitude.formatCoordinate()}, ${longitude.formatCoordinate()}"

    fun keywords(): List<String> =
        listOfNotNull(name, admin1, country)
}

@Serializable
internal data class WeatherSnapshot(
    val temperature: Double,
    val windSpeed: Double,
    val weatherCode: Int? = null,
    val description: String? = null
) {
    fun summary(): String =
        "${temperature.roundToInt()} C, ${description ?: weatherCode.description()}, wind ${windSpeed.roundToInt()} km/h"

    fun icon(): Icon = when (weatherCode) {
        in 200..299 -> Icon.Builtin("Thunderstorm")
        in 300..399, in 500..599 -> Icon.Builtin("WaterDrop")
        in 600..699 -> Icon.Builtin("AcUnit")
        in 700..799 -> Icon.Builtin("WbCloudy")
        800 -> Icon.Builtin("WbSunny")
        in 801..899 -> Icon.Builtin("WbCloudy")
        else -> Icon.Builtin("DeviceThermostat")
    }
}

@Serializable
internal data class GeocodingResult(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String? = null,
    val state: String? = null
) {
    fun toCity(): City = City(
        name = name,
        latitude = lat,
        longitude = lon,
        country = country,
        admin1 = state
    )
}

@Serializable
internal data class OpenWeatherResponse(
    val main: OpenWeatherMain,
    val wind: OpenWeatherWind,
    val weather: List<OpenWeatherCondition> = emptyList()
)

@Serializable
internal data class OpenWeatherMain(
    @SerialName("temp")
    val temperature: Double,
)

@Serializable
internal data class OpenWeatherWind(
    @SerialName("speed")
    val windSpeed: Double
)

@Serializable
internal data class OpenWeatherCondition(
    val id: Int,
    val description: String? = null
)

private fun Int?.description(): String = when (this) {
    in 200..299 -> "thunderstorm"
    in 300..399 -> "drizzle"
    in 500..599 -> "rain"
    in 600..699 -> "snow"
    in 700..799 -> "mist"
    800 -> "clear"
    in 801..899 -> "clouds"
    null -> "weather"
    else -> "weather code $this"
}

private fun String.normalizeItemId(): String =
    trim()
        .lowercase()
        .map { char ->
            if (char.isLetterOrDigit()) char else '-'
        }
        .joinToString("")
        .trim('-')

internal fun Double.formatCoordinate(): String =
    (this * 100.0).roundToInt().let { value ->
        val sign = if (value < 0) "-" else ""
        val absolute = if (value < 0) -value else value
        "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
    }
