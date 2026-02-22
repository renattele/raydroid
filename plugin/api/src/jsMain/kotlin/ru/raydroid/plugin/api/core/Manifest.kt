package ru.raydroid.plugin.api.core

import app.cash.zipline.Zipline
import ru.raydroid.plugin.api.ZiplineServices
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.api.ui.buildRayNodes

@DslMarker
annotation class ManifestDSL

@ManifestDSL
interface ManifestScope {
    var name: String
    var title: UiText
    var description: UiText
    var author: UiText
    var version: Int
    var platforms: List<Platform>
    var categories: List<String>
    var license: String

    @ManifestDSL
    fun command(service: CommandService, content: CommandScope.() -> Unit)

    @ManifestDSL
    interface CommandScope {
        var title: UiText
        var description: UiText
        var mode: Command.Mode
        var keywords: List<UiText>

        var match: Regex?

        @ManifestDSL
        fun argument(content: ArgumentScope.() -> Unit)

        @ManifestDSL
        fun preference(content: PreferenceScope.() -> Unit)

        @ManifestDSL
        interface ArgumentScope {
            var name: String
            var placeholder: UiText
            var type: Command.Argument.Type
            var required: Boolean
        }

        @ManifestDSL
        interface PreferenceScope {
            var name: String
            var title: UiText
            var description: UiText
            var type: Command.Preference.Type
            var placeholder: UiText?
            var default: String
        }
    }
}

private val zipline = Zipline.get()

@ManifestDSL
fun manifest(content: ManifestScope.() -> Unit) {
    val scope = object : ManifestScope {
        override var name: String = ""
        override var title: UiText = UiText.Empty
        override var description: UiText = UiText.Empty
        override var author: UiText = UiText.Empty
        override var version: Int = -1
        override var platforms: List<Platform> = emptyList()
        override var categories: List<String> = emptyList()
        override var license: String = ""
        val commands: MutableList<Command> = mutableListOf()
        override fun command(service: CommandService, content: ManifestScope.CommandScope.() -> Unit) {
            val scope = object : ManifestScope.CommandScope {
                override var title: UiText = UiText.Empty
                override var description: UiText = UiText.Empty
                override var mode: Command.Mode = Command.Mode.View
                override var keywords: List<UiText> = emptyList()
                override var match: Regex? = null
                val arguments: MutableList<Command.Argument> = mutableListOf()
                val preferences: MutableList<Command.Preference> = mutableListOf()
                override fun argument(content: ManifestScope.CommandScope.ArgumentScope.() -> Unit) {
                    val scope = object : ManifestScope.CommandScope.ArgumentScope {
                        override var name: String = ""
                        override var placeholder: UiText = UiText.Empty
                        override var type: Command.Argument.Type = Command.Argument.Type.Text
                        override var required: Boolean = false
                    }
                    scope.content()
                    arguments.add(
                        Command.Argument(
                            name = scope.name,
                            placeholder = scope.placeholder,
                            type = scope.type,
                            required = scope.required
                        )
                    )
                }

                override fun preference(content: ManifestScope.CommandScope.PreferenceScope.() -> Unit) {
                    val scope = object : ManifestScope.CommandScope.PreferenceScope {
                        override var name: String = ""
                        override var title: UiText = UiText.Empty
                        override var description: UiText = UiText.Empty
                        override var type: Command.Preference.Type = Command.Preference.Type.Text
                        override var placeholder: UiText? = null
                        override var default: String = ""
                    }
                    scope.content()
                    preferences.add(
                        Command.Preference(
                            name = scope.name,
                            title = scope.title,
                            description = scope.description,
                            type = scope.type,
                            placeholder = scope.placeholder,
                            default = scope.default
                        )
                    )
                }
            }
            scope.content()
            val serviceName = requireNotNull(service::class.simpleName)
            commands.add(
                Command(
                    service = serviceName,
                    title = scope.title,
                    description = scope.description,
                    mode = scope.mode,
                    arguments = scope.arguments,
                    preferences = scope.preferences,
                    match = scope.match?.toString()
                )
            )
            zipline.bind<CommandServiceBridge>(serviceName, service.toBridge())
        }
    }
    scope.content()
    val manifest = Manifest(
        name = scope.name,
        title = scope.title,
        description = scope.description,
        author = scope.author,
        version = scope.version,
        platforms = scope.platforms,
        categories = scope.categories,
        license = scope.license,
        commands = scope.commands
    )
    val manifestService = ManifestServiceImpl(manifest)
    zipline.bind<ManifestService>(ZiplineServices.Manifest.toString(), manifestService)
}

internal class ManifestServiceImpl(val manifest: Manifest): ManifestService {
    override fun getManifest(): Manifest {
        return manifest
    }
}

internal fun CommandService.toBridge(): CommandServiceBridge = object : CommandServiceBridge {
    override fun content(): List<RayNodeData> {
        return buildRayNodes {
            content()
        }
    }

    override suspend fun execute() {
        this@toBridge.execute()
    }

    override fun initialize(request: CommandServiceBridge.RenderRequest) {
        _onRenderRequest = request::requestRender
    }

    override suspend fun update(query: String) {
        this@toBridge.update(query)
    }

}