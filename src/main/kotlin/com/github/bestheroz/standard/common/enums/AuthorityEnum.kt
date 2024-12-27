package com.github.bestheroz.standard.common.enums

enum class AuthorityEnum(
    private val value: String,
) : ValueEnum {
    ADMIN_VIEW("ADMIN_VIEW"),
    ADMIN_EDIT("ADMIN_EDIT"),
    USER_VIEW("USER_VIEW"),
    USER_EDIT("USER_EDIT"),
    NOTICE_VIEW("NOTICE_VIEW"),
    NOTICE_EDIT("NOTICE_EDIT"),
    ;

    override fun getValue(): String = value
}
