package com.purnendu.contactly.ui.components

/**
 * Sealed class representing different confirmation dialog scenarios.
 * This is a generic pattern that allows easy extension for new confirmation types.
 * 
 * Usage:
 * 1. Add a new data class or object extending ConfirmationDialogState for each scenario
 * 2. Handle the new type in the when expression where the dialog is displayed
 * 3. Each type can have its own custom title, message, and button texts
 */
sealed class ConfirmationDialogState {
    
    /**
     * Shown when user activates a contact with a past start time.
     * The activation will be saved immediately when confirmed, but the alarm
     * won't trigger until the next occurrence of that time.
     */
    data class PastTimeActivation(
        val onConfirm: () -> Unit
    ) : ConfirmationDialogState()
    
    // Add more confirmation scenarios below as needed:
    // 
    // Example: Confirm before deleting multiple activations
    // data class BulkDelete(
    //     val count: Int,
    //     val onConfirm: () -> Unit
    // ) : ConfirmationDialogState()
    //
    // Example: Confirm before overwriting an existing activation
    // data class OverwriteActivation(
    //     val existingActivationName: String,
    //     val onConfirm: () -> Unit
    // ) : ConfirmationDialogState()
}

/**
 * Extension function to get dialog properties for each confirmation type.
 * This keeps the dialog configuration centralized and easy to maintain.
 */
fun ConfirmationDialogState.getDialogProperties(): ConfirmationDialogProperties {
    return when (this) {
        is ConfirmationDialogState.PastTimeActivation -> ConfirmationDialogProperties(
            title = "Confirmation",
            message = "The selected time has already passed. We can activate this for next week instead. Would you like to proceed?",
            confirmButtonText = "Continue",
            dismissButtonText = "Cancel"
        )
        // Add more cases here for new confirmation types
    }
}

/**
 * Data class holding dialog display properties.
 * Separated from the state to allow easy customization of dialog appearance.
 */
data class ConfirmationDialogProperties(
    val title: String,
    val message: String,
    val confirmButtonText: String,
    val dismissButtonText: String
)
