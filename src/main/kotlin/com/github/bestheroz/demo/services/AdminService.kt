package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.domain.service.OperatorHelper
import com.github.bestheroz.demo.dtos.admin.*
import com.github.bestheroz.demo.repository.AdminRepository
import com.github.bestheroz.standard.common.authenticate.JwtTokenProvider
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.Unauthorized401Exception
import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.LogUtils
import com.github.bestheroz.standard.common.util.PasswordUtil.verifyPassword
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AdminService(
    private val adminRepository: AdminRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val coroutineScope: CoroutineScope,
    private val operatorHelper: OperatorHelper,
) {
    companion object {
        private val log = logger()
    }

    fun getAdminList(request: AdminDto.Request): ListResult<AdminDto.Response> {
        val count = adminRepository.countByMap(mapOf("removedFlag" to false))
        with(request) {
            return ListResult(
                page = page,
                pageSize = pageSize,
                total = count,
                items =
                    if (count == 0L) {
                        emptyList()
                    } else {
                        adminRepository
                            .getItemsByMapOrderByLimitOffset(
                                mapOf("removedFlag" to false),
                                listOf("-id"),
                                pageSize,
                                (page - 1) * pageSize,
                            ).apply(operatorHelper::fulfilOperator)
                            .map(AdminDto.Response::of)
                    },
            )
        }
    }

    fun getAdmin(id: Long): AdminDto.Response =
        adminRepository
            .getItemById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .apply(operatorHelper::fulfilOperator)
            .let(AdminDto.Response::of)

    @Transactional
    fun createAdmin(
        request: AdminCreateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        adminRepository
            .getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))
            .ifPresent { throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT) }
        return request
            .toEntity(operator)
            .apply {
                adminRepository.insert(this)
                operatorHelper.fulfilOperator(this)
            }.let(AdminDto.Response::of)
    }

    @Transactional
    suspend fun updateAdmin(
        id: Long,
        request: AdminUpdateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        val adminLoginIdDeferred =
            coroutineScope.async(Dispatchers.IO) {
                adminRepository.getItemByMap(
                    mapOf("loginId" to request.loginId, "removedFlag" to false, "id:not" to id),
                )
            }
        val adminDeferred = coroutineScope.async(Dispatchers.IO) { adminRepository.getItemById(id) }

        adminLoginIdDeferred.await().ifPresent {
            BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }

        return adminDeferred
            .await()
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .also {
                if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                if (!request.managerFlag && it.id == operator.id) {
                    throw BadRequest400Exception(ExceptionCode.CANNOT_UPDATE_YOURSELF)
                }
            }.apply {
                update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.managerFlag,
                    request.authorities,
                    operator,
                )
                adminRepository.updateById(this, id)
                operatorHelper.fulfilOperator(this)
            }.let(AdminDto.Response::of)
    }

    @Transactional
    fun deleteAdmin(
        id: Long,
        operator: Operator,
    ) {
        adminRepository
            .getItemById(id)
            .orElseThrow { (BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)) }
            .also {
                if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                if (operator.type == UserTypeEnum.ADMIN && it.id == operator.id) {
                    throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF)
                }
            }.apply {
                remove(operator)
                adminRepository.updateById(this, id)
            }
    }

    @Transactional
    fun changePassword(
        id: Long,
        request: AdminChangePasswordDto.Request,
        operator: Operator,
    ): AdminDto.Response =
        adminRepository
            .getItemById(id)
            .orElseThrow { (BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)) }
            .also {
                if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                it.password
                    ?.takeUnless { password -> verifyPassword(request.oldPassword, password) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                it.password
                    ?.takeIf { password -> password == request.newPassword }
                    ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
            }.apply {
                changePassword(request.newPassword, operator)
                adminRepository.updateById(this, id)
                operatorHelper.fulfilOperator(this)
            }.let(AdminDto.Response::of)

    @Transactional
    fun loginAdmin(request: AdminLoginDto.Request): TokenDto =
        adminRepository
            .getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
            .also {
                if (!it.useFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                it.password
                    ?.takeUnless { password -> verifyPassword(request.password, password) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
            }.apply {
                renewToken(jwtTokenProvider.createRefreshToken(Operator(this)))
                adminRepository.updateById(this, this.id!!)
                operatorHelper.fulfilOperator(this)
            }.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }

    @Transactional
    fun renewToken(refreshToken: String): TokenDto =
        adminRepository
            .getItemById(jwtTokenProvider.getId(refreshToken))
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .also {
                if (it.removedFlag || it.token == null || !jwtTokenProvider.validateToken(refreshToken)) {
                    throw Unauthorized401Exception()
                }
            }.apply {
                token?.let {
                    if (it == refreshToken) {
                        renewToken(jwtTokenProvider.createRefreshToken(Operator(this)))
                        adminRepository.updateById(this, this.id!!)
                    }
                }
            }.run {
                token?.let { it ->
                    if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it) || it == refreshToken) {
                        return TokenDto(jwtTokenProvider.createAccessToken(Operator(this)), it)
                    }
                }
                throw Unauthorized401Exception()
            }

    @Transactional
    fun logout(id: Long) =
        try {
            adminRepository.getItemById(id).ifPresent { admin ->
                admin.logout()
                adminRepository.updateById(admin, id)
            }
        } catch (e: Exception) {
            log.warn(LogUtils.getStackTrace(e))
        }

    fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean =
        adminRepository.countByMap(
            buildMap {
                put("loginId", loginId)
                put("removedFlag", false)
                id?.let { put("id:not", it) }
            },
        ) == 0L
}
