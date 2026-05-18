package ru.raydroid.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okio.Path
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val PREFERENCES_FILE_NAME = "raydroid.preferences_pb"

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

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath {
            get<Path>(named("localPath")) / PREFERENCES_FILE_NAME
        }
    }
}
