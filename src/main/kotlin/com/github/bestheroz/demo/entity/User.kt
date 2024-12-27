package com.github.bestheroz.demo.entity

import com.github.bestheroz.standard.common.entity.IdCreatedUpdated
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.getPasswordHash
import jakarta.persistence.Table
import java.time.Instant

@Table(name = "users")
data class User(
    var loginId: String = "",
    var password: String? = null,
    var token: String? = null,
    var name: String = "",
    var useFlag: Boolean = false,
    var authorities: List<AuthorityEnum> = mutableListOf(),
    var changePasswordAt: Instant? = null,
    var latestActiveAt: Instant? = null,
    var joinedAt: Instant? = null,
    var additionalInfo: Map<String, Any> = mutableMapOf(),
    var removedFlag: Boolean = false,
    var removedAt: Instant? = null,
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
