package com.purnendu.contactly.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Small command object for changing NavigationState.
 *
 * UI code can call this instead of directly editing back stacks, which keeps
 * navigation decisions in one predictable place.
 */
class Navigator(
    private val state: NavigationState,
) {
    /**
     * Opens a route.
     *
     * Top-level routes switch tabs; child routes are pushed onto the currently
     * selected tab's back stack.
     */
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    /**
     * Moves backward inside the current tab.
     *
     * If the current tab is already at its root screen, back moves toward the
     * app's start route instead of removing the start screen.
     */
    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
