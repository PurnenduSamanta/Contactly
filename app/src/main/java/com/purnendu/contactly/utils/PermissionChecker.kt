package com.purnendu.contactly.utils

/**
 * Interface for permission-related operations.
 * Abstracts Android permission checks to make ViewModels testable.
 */
interface PermissionChecker {
    fun hasContactsPermission(): Boolean
    fun hasWriteContactsPermission(): Boolean
    fun canActivateExactAlarms(): Boolean
}
