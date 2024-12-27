package com.github.bestheroz.demo.entity

import com.github.bestheroz.standard.common.entity.IdCreatedUpdated
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.getPasswordHash
import jakarta.persistence.Table
import java.time.Instant

@Table(name = "admins")
data class Admin(
    var loginId: String = "",
    var password: String? = null,
    var token: String? = null,
    var name: String = "",
    var useFlag: Boolean = false,
    var managerFlag: Boolean = false,
    var authorities: List<AuthorityEnum> = mutableListOf(),
    var changePasswordAt: Instant? = null,
    var latestActiveAt: Instant? = null,
    var joinedAt: Instant? = null,
    var removedFlag: Boolean = false,
    var removedAt: Instant? = null,
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
