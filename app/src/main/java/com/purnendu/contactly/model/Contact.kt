package com.purnendu.contactly.model

data class Contact(
    val name: String,
    val phone: String,
    val image: Any?,
    val id: Long? = null,
    val lookupKey: String? = null
)
