package ru.raydroid.core.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val coreDataModule = module {
    single<Json> {
        Json {
            ignoreUnknownKeys = true
        }
    }

    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(get(), contentType = ContentType.Any)
            }
        }
    }

    single<CoroutineScope> {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}