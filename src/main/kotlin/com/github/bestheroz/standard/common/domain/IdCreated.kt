package com.github.bestheroz.standard.common.domain

import com.github.bestheroz.demo.domain.Admin
import com.github.bestheroz.demo.domain.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.Column
import java.time.Instant

abstract class IdCreated {
    @field:Column var id: Long? = null

    @field:Column lateinit var createdAt: Instant

    @field:Column lateinit var createdObjectType: UserTypeEnum

    @field:Column var createdObjectId: Long = 0L

    var createdByAdmin: Admin? = null

    var createdByUser: User? = null

    var creator: Operator? = null

    fun setCreatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        createdAt = instant
        createdObjectId = operator.id
        createdObjectType = operator.type
        creator = operator
        when (operator.type) {
            UserTypeEnum.ADMIN -> {
                createdByAdmin = Admin.of(operator)
                createdByUser = null
            }

            UserTypeEnum.USER -> {
                createdByAdmin = null
                createdByUser = User.of(operator)
            }
        }
    }

    val createdBy: UserSimpleDto
        get() =
            when (createdObjectType) {
                UserTypeEnum.ADMIN -> {
                    creator?.let(UserSimpleDto::of)
                        ?: createdByAdmin?.let(UserSimpleDto::of)
                        ?: throw IllegalStateException("Neither createdByAdmin nor creator exists")
                }

                UserTypeEnum.USER -> {
                    creator?.let(UserSimpleDto::of)
                        ?: createdByUser?.let(UserSimpleDto::of)
                        ?: throw IllegalStateException("Neither createdByUser nor creator exists")
                }
            }
}
