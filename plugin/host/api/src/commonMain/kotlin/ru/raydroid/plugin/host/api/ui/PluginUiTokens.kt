package ru.raydroid.plugin.host.api.ui

enum class PluginColor {
    Primary,
    PrimaryContainer,
    OnPrimary,
    OnPrimaryContainer,

    Secondary,
    OnSecondary,
    SecondaryContainer,
    OnSecondaryContainer,

    Tertiary,
    OnTertiary,
    TertiaryContainer,
    OnTertiaryContainer,

    Error,
    ErrorContainer,
    OnError,
    OnErrorContainer,

    PrimaryFixed,
    PrimaryFixedDim,
    OnPrimaryFixed,
    OnPrimaryFixedVariant,

    SecondaryFixed,
    SecondaryFixedDim,
    OnSecondaryFixed,
    OnSecondaryFixedVariant,

    TertiaryFixed,
    TertiaryFixedDim,
    OnTertiaryFixed,
    OnTertiaryFixedVariant,

    SurfaceDim,
    Surface,
    SurfaceBright,

    SurfaceContainerLowest,
    SurfaceContainerLow,
    SurfaceContainer,
    SurfaceContainerHigh,
    SurfaceContainerHighest,

    OnSurface,
    OnSurfaceVariant,
    Outline,
    OutlineVariant,

    InverseSurface,
    InverseOnSurface,
    InversePrimary,

    Scrim,

    Transparent,
}

enum class PluginFontSize {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge,
}

enum class PluginFontWeight {
    Normal,
    Bold,
}

enum class PluginIconSize {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge,
}

enum class PluginShapeToken {
    None,
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge,
    Full,
}

enum class PluginMotionToken {
    None,
    Fast,
    Default,
    Emphasized,
}

enum class PluginSpacing {
    Zero,
    Minimal,
    Border,
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge,
}

enum class PluginAlignment {
    Start,
    Center,
    End,
}

enum class PluginBoxAlignment {
    TopStart,
    TopCenter,
    TopEnd,
    CenterStart,
    Center,
    CenterEnd,
    BottomStart,
    BottomCenter,
    BottomEnd,
}

enum class PluginOrientation {
    Vertical,
    Horizontal,
}

enum class PluginArrangement {
    Start,
    Center,
    End,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly,
}
