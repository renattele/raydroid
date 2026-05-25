package ru.raydroid.plugin.host.impl.services

import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

internal object PresentationRetainer {
    private val delegates = mutableSetOf<Any>()

    fun retain(value: Any) {
        delegates += value
    }

    fun release(value: Any) {
        delegates -= value
    }
}

internal fun topViewController(): UIViewController? {
    val rootController =
        UIApplication.sharedApplication.windows
            .firstOrNull()
            ?.let { it as? UIWindow }
            ?.rootViewController ?: return null
    return rootController.topPresentedController()
}

private fun UIViewController.topPresentedController(): UIViewController =
    when {
        presentedViewController != null -> presentedViewController!!.topPresentedController()
        this is UINavigationController -> visibleViewController?.topPresentedController() ?: this
        this is UITabBarController -> selectedViewController?.topPresentedController() ?: this
        else -> this
    }
