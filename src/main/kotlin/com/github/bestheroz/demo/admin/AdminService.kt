package com.github.bestheroz.demo.admin

import com.github.bestheroz.demo.entity.service.OperatorHelper
import com.github.bestheroz.demo.repository.AdminRepository
import com.github.bestheroz.standard.common.authenticate.JwtTokenProvider
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
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
import kotlinx.coroutines.withContext
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
        return ListResult(
            page = request.page,
            pageSize = request.pageSize,
            total = count,
            items =
                if (count == 0L) {
                    emptyList()
                } else {
                    adminRepository
                        .getItemsByMapOrderByLimitOffset(
                            mapOf("removedFlag" to false),
                            listOf("-id"),
                            request.page,
                            request.pageSize,
                        ).let(operatorHelper::fulfilOperator)
                        .map(AdminDto.Response::of)
                },
        )
    }

    fun getAdmin(id: Long): AdminDto.Response =
        adminRepository.getItemById(id)?.let(operatorHelper::fulfilOperator)?.let(AdminDto.Response::of)
            ?: throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)

    @Transactional
    fun createAdmin(
        request: AdminCreateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        adminRepository.getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))?.let {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return request
            .toEntity(operator)
            .let {
                adminRepository.insert(it)
                it
            }.let {
                operatorHelper.fulfilOperator(it)
                it
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

        val admin =
            withContext(Dispatchers.IO) { adminRepository.getItemById(id) }
                ?: throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
        adminLoginIdDeferred.await()?.let {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        admin
            .takeIf { it.removedFlag }
            ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
        admin
            .takeIf { !request.managerFlag && it.id == operator.id }
            ?.let { throw BadRequest400Exception(ExceptionCode.CANNOT_UPDATE_YOURSELF) }

        return admin
            .let { it ->
                it.update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.managerFlag,
                    request.authorities,
                    operator,
                )
                it
            }.let {
                adminRepository.insert(it)
                it
            }.let {
                operatorHelper.fulfilOperator(it)
                it
            }.let(AdminDto.Response::of)
    }

    @Transactional
    fun deleteAdmin(
        id: Long,
        operator: Operator,
    ) = adminRepository.getItemById(id)?.let { admin ->
        admin
            .takeIf { it.removedFlag }
            ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
        admin
            .takeIf { it.id == operator.id }
            ?.let { throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF) }
        admin.remove(operator)
    } ?: throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)

    @Transactional
    fun changePassword(
        id: Long,
        request: AdminChangePasswordDto.Request,
        operator: Operator,
    ): AdminDto.Response =
        adminRepository
            .getItemById(id)
            ?.let { admin ->
                admin
                    .takeIf { it.removedFlag }
                    ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
                admin.password
                    ?.takeUnless { verifyPassword(request.oldPassword, it) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                admin.password
                    ?.takeIf { it == request.newPassword }
                    ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
                admin
            }?.let {
                it.changePassword(request.newPassword, operator)
                it
            }?.let(AdminDto.Response::of) ?: throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)

    @Transactional
    fun loginAdmin(request: AdminLoginDto.Request): TokenDto =
        adminRepository
            .getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))
            ?.let { admin ->
                admin
                    .takeUnless { admin.useFlag }
                    ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
                admin.password
                    ?.takeUnless { verifyPassword(request.password, it) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                admin
            }?.let {
                it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                it
            }?.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }
            ?: throw BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT)

    @Transactional
    fun renewToken(refreshToken: String): TokenDto =
        adminRepository.getItemById(jwtTokenProvider.getId(refreshToken))?.let { admin ->
            admin
                .takeIf {
                    admin.removedFlag || admin.token == null || !jwtTokenProvider.validateToken(refreshToken)
                }?.let { throw Unauthorized401Exception() }

            admin.token?.let { it ->
                if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it)) {
                    return TokenDto(jwtTokenProvider.createAccessToken(Operator(admin)), it)
                } else if (it == refreshToken) {
                    admin.renewToken(jwtTokenProvider.createRefreshToken(Operator(admin)))
                    return TokenDto(jwtTokenProvider.createAccessToken(Operator(admin)), it)
                }
            }
            throw Unauthorized401Exception()
        } ?: throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)

    @Transactional
    fun logout(id: Long) =
        try {
            adminRepository.getItemById(id)?.logout()
        } catch (e: Exception) {
            log.warn(LogUtils.getStackTrace(e))
        }

    fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean =
        adminRepository.getItemByMap(
            buildMap {
                put("loginId", loginId)
                put("removedFlag", false)
                id?.let { put("id:not", it) }
            },
        ) == null
}
