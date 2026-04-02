package ru.raydroid.plugin.api.manifest

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.raydroid.plugin.api.model.UiText

object ManifestUiTextSerializer : KSerializer<UiText> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ManifestUiText", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UiText =
        UiText.Resource(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: UiText) {
        encoder.encodeString(value.text)
    }
}
