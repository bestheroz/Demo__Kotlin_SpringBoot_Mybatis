package com.github.bestheroz.demo.domain

import com.github.bestheroz.standard.common.domain.IdCreatedUpdated
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.getPasswordHash
import jakarta.persistence.Column
import java.time.Instant

data class User(
    @field:Column var loginId: String = "",
    @field:Column var password: String? = null,
    @field:Column var token: String? = null,
    @field:Column var name: String = "",
    @field:Column var useFlag: Boolean = false,
    @field:Column var authorities: List<AuthorityEnum> = mutableListOf(),
    @field:Column var changePasswordAt: Instant? = null,
    @field:Column var latestActiveAt: Instant? = null,
    @field:Column var joinedAt: Instant? = null,
    @field:Column var additionalInfo: Map<String, Any> = mutableMapOf(),
    @field:Column var removedFlag: Boolean = false,
    @field:Column var removedAt: Instant? = null,
) : IdCreatedUpdated() {
    fun getType(): UserTypeEnum = UserTypeEnum.USER

    companion object {
        fun of(
            loginId: String,
            password: String,
            name: String,
            useFlag: Boolean,
            authorities: List<AuthorityEnum>,
            operator: Operator,
        ) = User(
            loginId = loginId,
            name = name,
            useFlag = useFlag,
            authorities = authorities,
            additionalInfo = mapOf(),
        ).apply {
            val now = Instant.now()
            this.password = getPasswordHash(password)
            this.joinedAt = now
            this.removedFlag = false
            this.setCreatedBy(operator, now)
            this.setUpdatedBy(operator, now)
        }

        fun of(operator: Operator) =
            User(
                loginId = operator.loginId,
                name = operator.name,
                useFlag = false,
                authorities = listOf(),
                additionalInfo = mapOf(),
            ).apply { this.id = operator.id }
    }

    fun update(
        loginId: String,
        password: String?,
        name: String,
        useFlag: Boolean,
        authorities: List<AuthorityEnum>,
        operator: Operator,
    ) {
        this.loginId = loginId
        this.name = name
        this.useFlag = useFlag
        this.authorities = authorities
        val now = Instant.now()
        this.setUpdatedBy(operator, now)
        password?.let {
            this.password = getPasswordHash(it)
            this.changePasswordAt = now
        }
    }

    fun changePassword(
        password: String,
        operator: Operator,
    ) {
        this.password = getPasswordHash(password)
        val now = Instant.now()
        this.changePasswordAt = now
        this.setUpdatedBy(operator, now)
    }

    fun remove(operator: Operator) {
        this.removedFlag = true
        val now = Instant.now()
        this.removedAt = now
        this.setUpdatedBy(operator, now)
    }

    fun renewToken(token: String?) {
        this.token = token
        this.latestActiveAt = Instant.now()
    }

    fun logout() {
        this.token = null
    }
}
