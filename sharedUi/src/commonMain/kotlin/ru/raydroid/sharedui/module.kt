package ru.raydroid.sharedui

import org.koin.dsl.KoinAppDeclaration
import ru.raydroid.feature.search.raydroidSearchModule
import ru.raydroid.feature.search.initKoin as initFeatureKoin

val appModule = raydroidSearchModule

fun initKoin(configuration: KoinAppDeclaration? = null) {
    initFeatureKoin(configuration)
}
