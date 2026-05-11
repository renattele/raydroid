package ru.raydroid.plugin.impl.weather

import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandService
import ru.raydroid.plugin.api.ui.Alignment
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.FontSize
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.LazyList
import ru.raydroid.plugin.api.ui.Modifier
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.Text
import ru.raydroid.plugin.api.ui.actions
import kotlin.math.roundToInt

class WeatherCommand : CommandService() {
    private val json = Json { ignoreUnknownKeys = true }
    private var state = WeatherState()
    private var loaded = false
    private var suggestions = emptyList<City>()
    private var isSearching = false
    private var showSelectedCity = false

    override suspend fun cachedItems(
        requestedItems: List<CommandItemId>?,
        chunkSize: Int
    ) = flow<List<CommandListItem>> {
        ensureLoaded()
        refreshSelectedWeather()
        render()
        emit(emptyList())
    }

    override fun CommandListScope.content() {
        val weather = state.weather
        entry(
            id = CommandItemId.CommandRoot,
            title = UiText.Plain(state.city?.displayName() ?: "Weather"),
            description = UiText.Plain(weather?.summary() ?: "Choose a city"),
            icon = weather?.icon() ?: WeatherIcon
        )
    }

    override fun RayScope.fullscreen() {
        if (state.city != null && (query.isBlank() || showSelectedCity)) {
            WeatherEmptyViewOffset()
        }
        LazyList(
            isLoading = isSearching,
            searchBarPlaceholder = UiText.Plain("Search city")
        ) {
            if (query.isNotBlank() && !showSelectedCity) suggestions.forEach { city ->
                item(
                    id = city.itemId(),
                    title = UiText.Plain(city.displayName()),
                    subtitle = UiText.Plain(city.subtitle()),
                    icon = Icon.Builtin("LocationCity"),
                    keywords = city.keywords(),
                    modifier = Modifier.actions {
                        action(
                            title = UiText.Plain("Choose"),
                            icon = Icon.Builtin("Check"),
                            primary = true
                        ) {
                            selectCity(city)
                        }
                    }
                )
            }
            emptyView(
                title = UiText.Plain(emptyTitle()),
                description = UiText.Plain(emptyDescription()),
                icon = if (query.isBlank()) WeatherIcon else Icon.Builtin("Search")
            )
        }
    }

    override fun CommandActionScope.actions(target: CommandActionTarget) {
        if (target.itemId != CommandItemId.CommandRoot || state.city == null) return

        action(
            title = UiText.Plain("Clear selection"),
            icon = Icon.Builtin("Clear"),
            style = CommandListAction.Style.Destructive
        ) {
            clearSelection()
        }
    }

    override suspend fun execute(action: CommandAction) {
        ensureLoaded()
        when (action) {
            is CommandAction.OpenCommand -> {
                suggestions = emptyList()
                showSelectedCity = query.isBlank()
                refreshSelectedWeather()
                render()
                renderFullscreen()
            }
            is CommandAction.CloseCommand -> {
                suggestions = emptyList()
                showSelectedCity = false
            }
            is CommandAction.Type -> {
                showSelectedCity = false
                searchCities(action.query)
                renderFullscreen()
            }
            is CommandAction.Enter -> {
                suggestions.firstOrNull { city -> city.itemId() == action.hoveredId }?.let { city ->
                    selectCity(city)
                }
            }
            is CommandAction.Focus -> Unit
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        state = Host.storage[StorageKey, WeatherState.serializer()] ?: WeatherState()
        loaded = true
    }

    private suspend fun searchCities(rawQuery: String) {
        val cityQuery = rawQuery.trim()
        if (cityQuery.length < 2) {
            suggestions = emptyList()
            isSearching = false
            return
        }

        isSearching = true
        renderFullscreen()
        suggestions = runCatching {
            val url = "$GeocodingEndpoint?name=${cityQuery.urlEncode()}&count=8&language=en&format=json"
            val response = Host.network.request(url)
            response.body
                ?.decodeToString()
                ?.let { body -> json.decodeFromString(GeocodingResponse.serializer(), body).results }
                .orEmpty()
                .map { result -> result.toCity() }
                .distinctBy { city -> city.id }
        }.getOrElse {
            Host.notification.showToast("Could not search cities")
            emptyList()
        }
        isSearching = false
    }

    private suspend fun selectCity(city: City) {
        val weather = fetchWeather(city)
        state = state.copy(city = city, weather = weather)
        persist()
        suggestions = emptyList()
        showSelectedCity = true
        Host.notification.showToast("Weather city set to ${city.name}")
        render()
        renderFullscreen()
    }

    private suspend fun clearSelection() {
        state = WeatherState()
        persist()
        suggestions = emptyList()
        showSelectedCity = false
        Host.notification.showToast("Weather city cleared")
        render()
        renderFullscreen()
    }

    private suspend fun refreshSelectedWeather() {
        val city = state.city ?: return
        val weather = fetchWeather(city) ?: return
        state = state.copy(weather = weather)
        persist()
    }

    private suspend fun fetchWeather(city: City): WeatherSnapshot? =
        runCatching {
            val url = "$ForecastEndpoint?latitude=${city.latitude}&longitude=${city.longitude}" +
                "&current_weather=true&timezone=auto"
            val response = Host.network.request(url)
            val current = response.body
                ?.decodeToString()
                ?.let { body -> json.decodeFromString(ForecastResponse.serializer(), body).currentWeather }
            current?.let {
                WeatherSnapshot(
                    temperature = it.temperature,
                    windSpeed = it.windSpeed,
                    weatherCode = it.weatherCode
                )
            }
        }.getOrElse {
            Host.notification.showToast("Could not load weather")
            null
        }

    private suspend fun persist() {
        Host.storage[StorageKey, WeatherState.serializer()] = state
    }

    private fun RayScope.WeatherEmptyViewOffset() {
        Column(
            spacing = Spacing.Zero,
            alignment = Alignment.Center
        ) {
            Text(
                text = UiText.Plain(" "),
                fontSize = FontSize.Large,
                color = Color.Transparent
            )
            Text(
                text = UiText.Plain(" "),
                fontSize = FontSize.Medium,
                color = Color.Transparent
            )
        }
    }

    private fun emptyTitle(): String =
        if (query.isBlank() || showSelectedCity) {
            state.city?.let { "Selected: ${it.displayName()}" } ?: "Choose a city"
        } else {
            "No cities found"
        }

    private fun emptyDescription(): String =
        if (query.isBlank() || showSelectedCity) {
            state.weather?.summary() ?: "Type a city name in the search field."
        } else {
            "Try another city name."
        }

    private fun String.urlEncode(): String {
        val bytes = encodeToByteArray()
        return buildString {
            bytes.forEach { byte ->
                val value = byte.toInt() and 0xff
                if (
                    value in 'A'.code..'Z'.code ||
                    value in 'a'.code..'z'.code ||
                    value in '0'.code..'9'.code ||
                    value == '-'.code ||
                    value == '_'.code ||
                    value == '.'.code ||
                    value == '~'.code
                ) {
                    append(value.toChar())
                } else {
                    append('%')
                    append(Hex[value ushr 4])
                    append(Hex[value and 0x0f])
                }
            }
        }
    }

    private companion object {
        const val StorageKey = "weather-state"
        const val GeocodingEndpoint = "https://geocoding-api.open-meteo.com/v1/search"
        const val ForecastEndpoint = "https://api.open-meteo.com/v1/forecast"
        const val Hex = "0123456789ABCDEF"
        val WeatherIcon = Icon.Builtin("WbSunny")
    }
}

@Serializable
private data class WeatherState(
    val city: City? = null,
    val weather: WeatherSnapshot? = null
)

@Serializable
private data class City(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
) {
    fun itemId(): CommandItemId = CommandItemId("weather.city.$id")

    fun displayName(): String =
        listOfNotNull(name, admin1?.takeIf { it != name }, country)
            .joinToString(", ")

    fun subtitle(): String =
        "${latitude.formatCoordinate()}, ${longitude.formatCoordinate()}"

    fun keywords(): List<String> =
        listOfNotNull(name, admin1, country)
}

@Serializable
private data class WeatherSnapshot(
    val temperature: Double,
    val windSpeed: Double,
    val weatherCode: Int
) {
    fun summary(): String =
        "${temperature.roundToInt()} C, ${weatherCode.description()}, wind ${windSpeed.roundToInt()} km/h"

    fun icon(): Icon = when (weatherCode) {
        0, 1 -> Icon.Builtin("WbSunny")
        2, 3, 45, 48 -> Icon.Builtin("WbCloudy")
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icon.Builtin("WaterDrop")
        95, 96, 99 -> Icon.Builtin("Cloud")
        else -> Icon.Builtin("DeviceThermostat")
    }
}

@Serializable
private data class GeocodingResponse(
    val results: List<GeocodingResult> = emptyList()
)

@Serializable
private data class GeocodingResult(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
) {
    fun toCity(): City = City(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        country = country,
        admin1 = admin1
    )
}

@Serializable
private data class ForecastResponse(
    @SerialName("current_weather")
    val currentWeather: CurrentWeather? = null
)

@Serializable
private data class CurrentWeather(
    val temperature: Double,
    @SerialName("windspeed")
    val windSpeed: Double,
    @SerialName("weathercode")
    val weatherCode: Int
)

private fun Int.description(): String = when (this) {
    0 -> "clear"
    1 -> "mostly clear"
    2 -> "partly cloudy"
    3 -> "cloudy"
    45, 48 -> "fog"
    51, 53, 55 -> "drizzle"
    56, 57 -> "freezing drizzle"
    61, 63, 65 -> "rain"
    66, 67 -> "freezing rain"
    71, 73, 75 -> "snow"
    77 -> "snow grains"
    80, 81, 82 -> "showers"
    85, 86 -> "snow showers"
    95 -> "thunderstorm"
    96, 99 -> "thunderstorm with hail"
    else -> "weather code $this"
}

private fun Double.formatCoordinate(): String =
    (this * 100.0).roundToInt().let { value ->
        val sign = if (value < 0) "-" else ""
        val absolute = if (value < 0) -value else value
        "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
    }
