package com.github.bestheroz.standard.common.security

import com.github.bestheroz.demo.domain.Admin
import com.github.bestheroz.demo.domain.User
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class Operator(
    val id: Long,
    val loginId: String,
    val name: String,
    val type: UserTypeEnum,
    val managerFlag: Boolean,
    val authorities: List<AuthorityEnum>,
) : UserDetails {
    constructor(
        admin: Admin,
    ) : this(
        id = admin.id!!,
        loginId = admin.loginId,
        name = admin.name,
        type = admin.getType(),
        managerFlag = admin.managerFlag,
        authorities = if (admin.managerFlag) AuthorityEnum.entries else admin.authorities,
    )

    constructor(
        user: User,
    ) : this(
        id = user.id!!,
        loginId = user.loginId,
        name = user.name,
        type = user.getType(),
        managerFlag = false,
        authorities = user.authorities,
    )

    override fun getAuthorities(): Collection<GrantedAuthority> =
        if (managerFlag) {
            AuthorityEnum.entries.map { SimpleGrantedAuthority(it.name) }
        } else {
            authorities.map { SimpleGrantedAuthority(it.name) }
        }

    override fun getPassword(): String? = null

    override fun getUsername(): String = loginId

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
