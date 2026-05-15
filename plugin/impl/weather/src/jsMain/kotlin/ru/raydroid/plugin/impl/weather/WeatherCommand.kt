package ru.raydroid.plugin.impl.weather

import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.ListSerializer
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
import ru.raydroid.plugin.api.ui.Form
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.LazyList
import ru.raydroid.plugin.api.ui.Modifier
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.Text
import ru.raydroid.plugin.api.ui.actions
import ru.raydroid.plugin.impl.weather.model.City
import ru.raydroid.plugin.impl.weather.model.GeocodingResult
import ru.raydroid.plugin.impl.weather.model.OpenWeatherResponse
import ru.raydroid.plugin.impl.weather.model.WeatherSnapshot
import ru.raydroid.plugin.impl.weather.model.WeatherState
import ru.raydroid.plugin.impl.weather.util.isCancellation
import ru.raydroid.plugin.impl.weather.util.isSuccess
import ru.raydroid.plugin.impl.weather.util.openWeatherException
import ru.raydroid.plugin.impl.weather.util.text
import ru.raydroid.plugin.impl.weather.util.urlEncode
import ru.raydroid.plugin.impl.weather.util.weatherErrorText

class WeatherCommand : CommandService() {
    private val json = Json { ignoreUnknownKeys = true }
    private var state = WeatherState()
    private var loaded = false
    private var suggestions = emptyList<City>()
    private var isSearching = false
    private var showSelectedCity = false
    private var searchError: UiText? = null

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
            title = state.city?.displayName()?.let(UiText::Plain) ?: UiText.Resource("command.weather.title"),
            description = weather?.summary()?.let(UiText::Plain) ?: UiText.Resource("weather.chooseCity"),
            icon = weather?.icon() ?: WeatherIcon
        )
    }

    override fun RayScope.fullscreen() {
        if (activeApiKey().isBlank()) {
            ApiKeyForm()
            return
        }

        if (state.city != null && (query.isBlank() || showSelectedCity)) {
            WeatherEmptyViewOffset()
        }
        LazyList(
            isLoading = isSearching,
            searchBarPlaceholder = UiText.Resource("weather.search.placeholder")
        ) {
            if (query.isNotBlank() && !showSelectedCity) suggestions.forEach { city ->
                item(
                    id = city.itemId(),
                    title = UiText.Plain(city.displayName()),
                    subtitle = UiText.Plain(city.subtitle()),
                    icon = Icon.Builtin("LocationCity"),
                    iconColor = WeatherIconColor,
                    keywords = city.keywords(),
                    modifier = Modifier.actions {
                        action(
                            title = UiText.Resource("weather.action.choose"),
                            icon = Icon.Builtin("Check"),
                            primary = true
                        ) {
                            selectCity(city)
                        }
                    }
                )
            }
            emptyView(
                title = emptyTitle(),
                description = emptyDescription(),
                icon = if (query.isBlank()) WeatherIcon else Icon.Builtin("Search"),
                iconColor = WeatherIconColor
            )
        }
    }

    override fun CommandActionScope.actions(target: CommandActionTarget) {
        if (target.itemId != CommandItemId.CommandRoot || state.city == null) return

        action(
            title = UiText.Resource("weather.action.clear"),
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
                searchError = null
                showSelectedCity = query.isBlank()
                if (activeApiKey().isNotBlank()) {
                    refreshSelectedWeather()
                }
                render()
                renderFullscreen()
            }
            is CommandAction.CloseCommand -> {
                suggestions = emptyList()
                searchError = null
                showSelectedCity = false
            }
            is CommandAction.Type -> {
                showSelectedCity = false
                if (activeApiKey().isBlank()) {
                    suggestions = emptyList()
                    isSearching = false
                } else {
                    searchCities(action.query)
                }
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
        state = runCatching {
            Host.storage[STORAGE_KEY, WeatherState.serializer()] ?: WeatherState()
        }.getOrElse {
            WeatherState()
        }
        loaded = true
    }

    private suspend fun searchCities(rawQuery: String) {
        val cityQuery = rawQuery.trim()
        if (cityQuery.length < 2) {
            suggestions = emptyList()
            searchError = null
            isSearching = false
            return
        }

        isSearching = true
        searchError = null
        renderFullscreen()
        suggestions = runCatching {
            val url = "$GEOCODING_ENDPOINT?q=${cityQuery.urlEncode()}&limit=5&appid=${activeApiKey().urlEncode()}"
            val response = Host.network.request(url)
            if (!response.statusCode.isSuccess()) {
                throw openWeatherException(response.statusCode)
            }
            response.body
                ?.decodeToString()
                ?.let { body -> json.decodeFromString(ListSerializer(GeocodingResult.serializer()), body) }
                .orEmpty()
                .map { result -> result.toCity() }
                .distinctBy { city -> city.itemId().value }
        }.getOrElse {
            if (!it.isCancellation()) {
                searchError = it.weatherErrorText(UiText.Resource("weather.error.search"))
            }
            emptyList()
        }
        isSearching = false
    }

    private suspend fun selectCity(city: City) {
        val weather = fetchWeather(city)
        state = state.copy(city = city, weather = weather)
        persist()
        suggestions = emptyList()
        searchError = null
        showSelectedCity = true
        Host.notification.showToast(UiText.Resource("weather.toast.city.saved"))
        render()
        renderFullscreen()
    }

    private suspend fun clearSelection() {
        state = WeatherState(apiKey = state.apiKey)
        persist()
        suggestions = emptyList()
        searchError = null
        showSelectedCity = false
        Host.notification.showToast(UiText.Resource("weather.toast.city.cleared"))
        render()
        renderFullscreen()
    }

    private suspend fun refreshSelectedWeather() {
        if (activeApiKey().isBlank()) return
        val city = state.city ?: return
        val weather = fetchWeather(city) ?: return
        state = state.copy(weather = weather)
        persist()
    }

    private suspend fun fetchWeather(city: City): WeatherSnapshot? =
        runCatching {
            val apiKey = activeApiKey()
            if (apiKey.isBlank()) error("OpenWeather API key is missing")
            val url = "$WEATHER_ENDPOINT?lat=${city.latitude}&lon=${city.longitude}" +
                "&units=metric&lang=en&appid=${apiKey.urlEncode()}"
            val response = Host.network.request(url)
            if (!response.statusCode.isSuccess()) {
                throw openWeatherException(response.statusCode)
            }
            response.body
                ?.decodeToString()
                ?.let { body -> json.decodeFromString(OpenWeatherResponse.serializer(), body) }
                ?.let {
                    WeatherSnapshot(
                        temperature = it.main.temperature,
                        windSpeed = it.wind.windSpeed * METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR,
                        weatherCode = it.weather.firstOrNull()?.id,
                        description = it.weather.firstOrNull()?.description
                    )
                }
        }.getOrElse {
            if (!it.isCancellation()) {
                Host.notification.showToast(it.weatherErrorText(UiText.Resource("weather.error.load")))
            }
            null
        }

    private suspend fun persist() {
        Host.storage[STORAGE_KEY, WeatherState.serializer()] = state
    }

    private fun activeApiKey(): String =
        DefaultOpenWeatherApiKey.ifBlank { state.apiKey }

    private fun RayScope.ApiKeyForm() {
        Form(
            suppressHostActions = true
        ) {
            description(
                text = UiText.Resource("weather.apiKey.description")
            )
            passwordField(
                id = API_KEY_FIELD_ID,
                title = UiText.Resource("weather.apiKey.title"),
                placeholder = UiText.Resource("weather.apiKey.placeholder"),
                required = true
            )
            submit(
                title = UiText.Resource("weather.action.save"),
                icon = Icon.Builtin("Save")
            ) { values ->
                saveApiKey(values.text(API_KEY_FIELD_ID))
            }
        }
    }

    private suspend fun saveApiKey(apiKey: String) {
        val cleanedKey = apiKey.trim()
        if (cleanedKey.isBlank()) {
            Host.notification.showToast(UiText.Resource("weather.toast.apiKey.empty"))
            return
        }
        state = state.copy(apiKey = cleanedKey)
        persist()
        Host.notification.showToast(UiText.Resource("weather.toast.apiKey.saved"))
        render()
        renderFullscreen()
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

    private fun emptyTitle(): UiText =
        if (query.isBlank() || showSelectedCity) {
            state.city?.let { UiText.Plain("Selected: ${it.displayName()}") } ?: UiText.Resource("weather.chooseCity")
        } else {
            UiText.Resource("weather.empty.noCities")
        }

    private fun emptyDescription(): UiText =
        if (query.isBlank() || showSelectedCity) {
            state.weather?.summary()?.let(UiText::Plain) ?: UiText.Resource("weather.empty.typeCity")
        } else if (searchError != null) {
            searchError ?: UiText.Resource("weather.error.search")
        } else {
            UiText.Resource("weather.empty.tryAnother")
        }

    private companion object {
        const val STORAGE_KEY = "weather-state"
        const val API_KEY_FIELD_ID = "apiKey"
        const val GEOCODING_ENDPOINT = "https://api.openweathermap.org/geo/1.0/direct"
        const val WEATHER_ENDPOINT = "https://api.openweathermap.org/data/2.5/weather"
        const val METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR = 3.6
        val WeatherIcon = Icon.Builtin("WbSunny")
        val WeatherIconColor = Color.OnSurfaceVariant
    }
}
