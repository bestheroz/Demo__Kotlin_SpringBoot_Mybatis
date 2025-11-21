package com.github.bestheroz.standard.common.domain

import com.github.bestheroz.demo.domain.Admin
import com.github.bestheroz.demo.domain.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.Column
import java.time.Instant

abstract class IdCreatedUpdated : IdCreated() {
    @field:Column lateinit var updatedObjectType: UserTypeEnum

    @field:Column lateinit var updatedAt: Instant

    @field:Column var updatedObjectId: Long = 0L

    var updatedByAdmin: Admin? = null

    var updatedByUser: User? = null

    var updater: Operator? = null

    fun setUpdatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        updatedAt = instant
        updatedObjectId = operator.id
        updatedObjectType = operator.type
        updater = operator
    }

    val updatedBy: UserSimpleDto
        get() =
            when (updatedObjectType) {
                UserTypeEnum.ADMIN -> {
                    updater?.let(UserSimpleDto::of)
                        ?: updatedByAdmin?.let(UserSimpleDto::of)
                        ?: throw IllegalStateException("Neither updatedByAdmin nor updater exists")
                }

                UserTypeEnum.USER -> {
                    updater?.let(UserSimpleDto::of)
                        ?: updatedByUser?.let(UserSimpleDto::of)
                        ?: throw IllegalStateException("Neither updatedByUser nor updater exists")
                }
            }
}
