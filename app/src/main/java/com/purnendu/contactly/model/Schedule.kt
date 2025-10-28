package com.purnendu.contactly.model

import androidx.annotation.DrawableRes

data class Schedule(
    val id: String,
    val name: String,
    val originalName: String,
    @DrawableRes val avatarResId: Int? = null
)