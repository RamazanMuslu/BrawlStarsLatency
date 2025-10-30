package com.yorastudio.overlaylatency

data class Servers(
    val id: Int,
    val name: String,
    val url: String,
    var isSelected: Boolean = false
)