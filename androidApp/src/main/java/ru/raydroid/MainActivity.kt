package ru.raydroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import app.cash.zipline.loader.ManifestVerifier
import app.cash.zipline.loader.ZiplineLoader
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import ru.raydroid.plugin.api.ui.RayNodeData
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val executorService = Executors.newSingleThreadExecutor { Thread(it, "Zipline") }
        val dispatcher = executorService.asCoroutineDispatcher()
        val queryFlow = MutableStateFlow("")
        val nodeFlow = MutableStateFlow(listOf<RayNodeData>())
        val loader = ZiplineLoader(
            dispatcher = dispatcher,
            manifestVerifier = ManifestVerifier.NO_SIGNATURE_CHECKS,
            httpClient = OkHttpClient()
        )
        startPlugin(
            scope = MainScope(),
            dispatcher = dispatcher,
            loader = loader,
            manifestUrl = "http://192.168.0.149:8080/manifest.zipline.json",
            query = queryFlow,
            nodes = nodeFlow
        )
        setContent {
            val query by queryFlow.collectAsState()
            val nodes = nodeFlow.collectAsState()
            App(query, nodes, onFieldUpdate = { queryFlow.value = it })
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
}