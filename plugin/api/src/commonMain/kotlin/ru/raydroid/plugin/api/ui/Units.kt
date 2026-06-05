package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Dp(
    val value: Float,
) {
    companion object {
        val Zero = Dp(0f)
    }
}

val Int.dp: Dp
    get() = Dp(this.toFloat())

val Float.dp: Dp
    get() = Dp(this)

@Serializable
@JvmInline
value class Sp(
    val value: Float,
)

val Int.sp: Sp
    get() = Sp(this.toFloat())

val Float.sp: Sp
    get() = Sp(this)
