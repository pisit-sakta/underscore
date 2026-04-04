package com.underscore.app.ui

/**
 * Navigation state for the app.
 * Main -> OptionsMenu -> sub-screens (Settings/Character/Mood/Drama).
 */
sealed class AppScreen {
    data object Main : AppScreen()
    data object OptionsMenu : AppScreen()
    data object Settings : AppScreen()
    data object Character : AppScreen()
    data object Mood : AppScreen()
    data object Drama : AppScreen()
    data object Franchise : AppScreen()
}
