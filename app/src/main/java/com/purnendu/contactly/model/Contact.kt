package com.purnendu.contactly.model

data class Contact(
    val name: String?,  // Nullable: null if contact has no actual name (only phone/email)
    val phone: String,
    val image: Any?,
    val id: Long? = null,
    val lookupKey: String? = null
)
