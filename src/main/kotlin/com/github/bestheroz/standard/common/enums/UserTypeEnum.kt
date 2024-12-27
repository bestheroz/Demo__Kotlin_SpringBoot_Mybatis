package com.github.bestheroz.standard.common.enums

enum class UserTypeEnum(
    private val value: String,
) : ValueEnum {
    ADMIN("ADMIN"),
    USER("USER"),
    ;

    override fun getValue(): String = value
}
