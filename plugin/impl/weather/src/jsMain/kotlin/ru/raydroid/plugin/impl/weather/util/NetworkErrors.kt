package ru.raydroid.plugin.impl.weather.util

import kotlinx.coroutines.CancellationException
import ru.raydroid.plugin.api.model.UiText

internal fun Int.isSuccess(): Boolean = this in 200..299

internal class WeatherUserException(val text: UiText) : Throwable()

internal fun openWeatherException(statusCode: Int): WeatherUserException =
    when (statusCode) {
        400 -> WeatherUserException(UiText.Resource("weather.error.rejected"))
        401 -> WeatherUserException(UiText.Resource("weather.error.apiKey.invalid"))
        429 -> WeatherUserException(UiText.Resource("weather.error.limit"))
        else -> WeatherUserException(UiText.Resource("weather.error.openWeather"))
    }

internal fun Throwable.isCancellation(): Boolean =
    this is CancellationException || message == "cancelled" || message == "canceled"

internal fun Throwable.weatherErrorText(fallback: UiText): UiText {
    val text = message.orEmpty().lowercase()
    return when {
        this is WeatherUserException -> this.text
        "timeout" in text || "timed out" in text -> UiText.Resource("weather.error.timeout")
        "unable to resolve host" in text || "failed to connect" in text -> UiText.Resource("weather.error.connection")
        else -> fallback
    }
}
