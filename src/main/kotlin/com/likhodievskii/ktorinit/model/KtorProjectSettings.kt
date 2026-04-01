package com.likhodievskii.ktorinit.model

data class KtorProjectSettings(
    val projectName: FieldWithDefault,
    val companyWebsite: FieldWithDefault,
    val engine: OptionsField,
    val ktorVersion: OptionsField,
    val kotlinVersion: OptionsField,
    val buildSystem: OptionsField,
    val configurationIn: OptionsField
)