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
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.LogUtils
import com.github.bestheroz.standard.common.util.PasswordUtil.isPasswordValid
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminService(
    private val adminRepository: AdminRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val operatorHelper: OperatorHelper,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    suspend fun getAdminList(request: AdminDto.Request): ListResult<AdminDto.Response> =
        coroutineScope {
            val filterMap =
                buildMap {
                    put("removedFlag", false)
                    request.id?.let { put("id", it) }
                    request.loginId?.let { put("loginId:contains", it) }
                    request.name?.let { put("name:contains", it) }
                    request.useFlag?.let { put("useFlag", it) }
                    request.managerFlag?.let { put("managerFlag", it) }
                }

            val countDeferred = async(Dispatchers.IO) { adminRepository.countByMap(filterMap) }
            val itemsDeferred =
                async(Dispatchers.IO) {
                    adminRepository.getItemsByMapOrderByLimitOffset(
                        filterMap,
                        listOf("-id"),
                        request.pageSize,
                        (request.page - 1) * request.pageSize,
                    )
                }

            val count = countDeferred.await()
            val items =
                if (count == 0L) {
                    itemsDeferred.cancel()
                    emptyList()
                } else {
                    operatorHelper.fulfilOperator(itemsDeferred.await()).map(AdminDto.Response::of)
                }

            ListResult(page = request.page, pageSize = request.pageSize, total = count, items = items)
        }

    suspend fun getAdmin(id: Long): AdminDto.Response =
        withContext(Dispatchers.IO) { adminRepository.getItemById(id) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .let { operatorHelper.fulfilOperator(it) }
            .let(AdminDto.Response::of)

    @Transactional
    suspend fun createAdmin(
        request: AdminCreateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        withContext(Dispatchers.IO) {
            adminRepository.getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))
        }.ifPresent { throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT) }
        return request
            .toEntity(operator)
            .let {
                withContext(Dispatchers.IO) { adminRepository.insert(it) }
                operatorHelper.fulfilOperator(it)
            }.let(AdminDto.Response::of)
    }

    @Transactional
    suspend fun updateAdmin(
        id: Long,
        request: AdminUpdateDto.Request,
        operator: Operator,
    ): AdminDto.Response =
        coroutineScope {
            val adminLoginIdDeferred =
                async(Dispatchers.IO) {
                    adminRepository.getItemByMap(
                        mapOf("loginId" to request.loginId, "removedFlag" to false, "id:not" to id),
                    )
                }
            val adminDeferred = async(Dispatchers.IO) { adminRepository.getItemById(id) }

            adminLoginIdDeferred.await().ifPresent {
                adminDeferred.cancel()
                throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
            }

            adminDeferred
                .await()
                .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
                .also {
                    if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                    if (!request.managerFlag && it.id == operator.id) {
                        throw BadRequest400Exception(ExceptionCode.CANNOT_UPDATE_YOURSELF)
                    }
                }.let {
                    it.update(
                        request.loginId,
                        request.password,
                        request.name,
                        request.useFlag,
                        request.managerFlag,
                        request.authorities,
                        operator,
                    )
                    withContext(Dispatchers.IO) { adminRepository.updateById(it, id) }
                    operatorHelper.fulfilOperator(it)
                }.let(AdminDto.Response::of)
        }

    @Transactional
    suspend fun deleteAdmin(
        id: Long,
        operator: Operator,
    ) {
        withContext(Dispatchers.IO) { adminRepository.getItemById(id) }
            .orElseThrow { (BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)) }
            .also {
                if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                if (operator.type == UserTypeEnum.ADMIN && it.id == operator.id) {
                    throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF)
                }
            }.let {
                it.remove(operator)
                withContext(Dispatchers.IO) { adminRepository.updateById(it, id) }
            }
    }

    @Transactional
    suspend fun changePassword(
        id: Long,
        request: AdminChangePasswordDto.Request,
        operator: Operator,
    ): AdminDto.Response =
        withContext(Dispatchers.IO) { adminRepository.getItemById(id) }
            .orElseThrow { (BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)) }
            .also {
                if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                it.password
                    ?.takeUnless { password -> isPasswordValid(request.oldPassword, password) }
                    ?.let {
                        logger.warn { "password not match" }
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                it.password
                    ?.takeIf { password -> isPasswordValid(request.newPassword, password) }
                    ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
            }.let {
                it.changePassword(request.newPassword, operator)
                withContext(Dispatchers.IO) { adminRepository.updateById(it, id) }
                operatorHelper.fulfilOperator(it)
            }.let(AdminDto.Response::of)

    @Transactional
    suspend fun loginAdmin(request: AdminLoginDto.Request): TokenDto =
        withContext(Dispatchers.IO) {
            adminRepository.getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))
        }.orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
            .also {
                if (!it.useFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                it.password
                    ?.takeUnless { password -> isPasswordValid(request.password, password) }
                    ?.let {
                        logger.warn { "password not match" }
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
            }.let {
                it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                withContext(Dispatchers.IO) { adminRepository.updateById(it, it.id!!) }
                operatorHelper.fulfilOperator(it)
            }.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }

    @Transactional
    suspend fun renewToken(refreshToken: String): TokenDto =
        withContext(Dispatchers.IO) {
            adminRepository.getItemById(jwtTokenProvider.getId(refreshToken))
        }.orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .also {
                if (it.removedFlag || it.token == null || !jwtTokenProvider.validateToken(refreshToken)) {
                    throw Unauthorized401Exception()
                }
            }.let {
                it.token?.let { token ->
                    if (token == refreshToken) {
                        it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                        withContext(Dispatchers.IO) { adminRepository.updateById(it, it.id!!) }
                    }
                }
                it
            }.let {
                it.token?.let { token ->
                    if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(token) || token == refreshToken) {
                        return TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), token)
                    }
                }
                throw Unauthorized401Exception()
            }

    @Transactional
    suspend fun logout(id: Long) {
        try {
            val optionalAdmin = withContext(Dispatchers.IO) { adminRepository.getItemById(id) }

            if (optionalAdmin.isPresent) {
                val admin = optionalAdmin.get()
                admin.logout()
                withContext(Dispatchers.IO) { adminRepository.updateById(admin, id) }
            }
        } catch (e: Exception) {
            logger.warn { LogUtils.getStackTrace(e) }
        }
    }

    suspend fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            adminRepository.countByMap(
                buildMap {
                    put("loginId", loginId)
                    put("removedFlag", false)
                    id?.let { put("id:not", it) }
                },
            )
        } == 0L
}
