package ru.raydroid.sharedui

import org.koin.dsl.KoinAppDeclaration
import ru.raydroid.feature.search.initKoin as initFeatureKoin
import ru.raydroid.feature.search.raydroidSearchModule

val appModule = raydroidSearchModule

fun initKoin(configuration: KoinAppDeclaration? = null) {
    initFeatureKoin(configuration)
}
