package com.likhodievskii.ktorinit.model

data class KtorFeature(
    val xmlId: String,
    val name: String,
    val group: String,
    val description: String,
    val requiredFeatures: List<String>
)
