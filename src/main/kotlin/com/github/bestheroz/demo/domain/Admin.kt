package com.github.bestheroz.demo.domain

import com.github.bestheroz.standard.common.domain.IdCreatedUpdated
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.getPasswordHash
import jakarta.persistence.Column
import java.time.Instant

data class Admin(
    @field:Column var loginId: String = "",
    @field:Column var password: String? = null,
    @field:Column var token: String? = null,
    @field:Column var name: String = "",
    @field:Column var useFlag: Boolean = false,
    @field:Column var managerFlag: Boolean = false,
    @field:Column var authorities: List<AuthorityEnum> = mutableListOf(),
    @field:Column var changePasswordAt: Instant? = null,
    @field:Column var latestActiveAt: Instant? = null,
    @field:Column var joinedAt: Instant? = null,
    @field:Column var removedFlag: Boolean = false,
    @field:Column var removedAt: Instant? = null,
) : IdCreatedUpdated() {
    fun getType(): UserTypeEnum = UserTypeEnum.ADMIN

    companion object {
        fun of(
            loginId: String,
            password: String,
            name: String,
            useFlag: Boolean,
            managerFlag: Boolean,
            authorities: List<AuthorityEnum>,
            operator: Operator,
        ) = Admin(
            loginId = loginId,
            name = name,
            useFlag = useFlag,
            managerFlag = managerFlag,
            authorities = authorities,
        ).apply {
            this.password = getPasswordHash(password)
            val now = Instant.now()
            this.joinedAt = now
            this.removedFlag = false
            this.setCreatedBy(operator, now)
            this.setUpdatedBy(operator, now)
        }

        fun of(operator: Operator) =
            Admin(
                loginId = operator.loginId,
                name = operator.name,
                useFlag = false,
                managerFlag = operator.managerFlag,
                authorities = emptyList(),
            ).apply { this.id = operator.id }
    }

    fun update(
        loginId: String,
        password: String?,
        name: String,
        useFlag: Boolean,
        managerFlag: Boolean,
        authorities: List<AuthorityEnum>,
        operator: Operator,
    ) {
        this.loginId = loginId
        this.name = name
        this.useFlag = useFlag
        this.managerFlag = managerFlag
        this.authorities = authorities
        val now = Instant.now()
        setUpdatedBy(operator, now)
        password?.let {
            this.password = getPasswordHash(password)
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
        setUpdatedBy(operator, now)
    }

    fun remove(operator: Operator) {
        removedFlag = true
        val now = Instant.now()
        removedAt = now
        setUpdatedBy(operator, now)
    }

    fun renewToken(token: String) {
        this.token = token
        latestActiveAt = Instant.now()
    }

    fun logout() {
        token = null
    }
}
