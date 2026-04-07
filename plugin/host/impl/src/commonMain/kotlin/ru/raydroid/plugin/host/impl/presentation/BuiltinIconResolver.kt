package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.ui.graphics.vector.ImageVector
import ru.raydroid.plugin.host.impl.presentation.generated.GeneratedOutlinedMaterialIconRegistry

internal interface BuiltinIconResolver {
    fun resolve(name: String): ImageVector
}

internal class OutlinedMaterialBuiltinIconResolver : BuiltinIconResolver {
    override fun resolve(name: String): ImageVector {
        return GeneratedOutlinedMaterialIconRegistry.resolve(name) ?: Icons.AutoMirrored.Outlined.HelpOutline
    }
}
