package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.domain.service.OperatorHelper
import com.github.bestheroz.demo.dtos.user.UserChangePasswordDto
import com.github.bestheroz.demo.dtos.user.UserCreateDto
import com.github.bestheroz.demo.dtos.user.UserDto
import com.github.bestheroz.demo.dtos.user.UserLoginDto
import com.github.bestheroz.demo.dtos.user.UserUpdateDto
import com.github.bestheroz.demo.repository.UserRepository
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
class UserService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val coroutineScope: CoroutineScope,
    private val operatorHelper: OperatorHelper,
) {
    companion object {
        private val log = logger()
    }

    fun getUserList(request: UserDto.Request): ListResult<UserDto.Response> {
        val count = userRepository.countByMap(mapOf("removedFlag" to false))
        return ListResult(
            page = request.page,
            pageSize = request.pageSize,
            total = count,
            items =
                if (count == 0L) {
                    emptyList()
                } else {
                    userRepository
                        .getItemsByMapOrderByLimitOffset(
                            mapOf("removedFlag" to false),
                            listOf("-id"),
                            request.page,
                            request.pageSize,
                        ).let(operatorHelper::fulfilOperator)
                        .map(UserDto.Response::of)
                },
        )
    }

    fun getUser(id: Long): UserDto.Response =
        userRepository.getItemById(id)?.let(operatorHelper::fulfilOperator)?.let(UserDto.Response::of)
            ?: throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)

    @Transactional
    fun createUser(
        request: UserCreateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        userRepository.getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))?.let {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return request
            .toEntity(operator)
            .let {
                userRepository.insert(it)
                it
            }.let {
                operatorHelper.fulfilOperator(it)
                it
            }.let(UserDto.Response::of)
    }

    @Transactional
    suspend fun updateUser(
        id: Long,
        request: UserUpdateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        val userLoginIdDeferred =
            coroutineScope.async(Dispatchers.IO) {
                userRepository.getItemByMap(
                    mapOf("loginId" to request.loginId, "removedFlag" to false, "id:not" to id),
                )
            }
        val user =
            withContext(Dispatchers.IO) { userRepository.getItemById(id) }
                ?: throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
        userLoginIdDeferred.await()?.let {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        user.takeIf { it.removedFlag }?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }

        return user
            .let { it ->
                it.update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.authorities,
                    operator,
                )
                it
            }.let {
                userRepository.insert(it)
                it
            }.let {
                operatorHelper.fulfilOperator(it)
                it
            }.let(UserDto.Response::of)
    }

    @Transactional
    fun deleteUser(
        id: Long,
        operator: Operator,
    ) = userRepository.getItemById(id)?.let { user ->
        user
            .takeIf { it.removedFlag }
            ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
        user
            .takeIf { it.id == operator.id }
            ?.let { throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF) }
        user.remove(operator)
    } ?: throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)

    @Transactional
    fun changePassword(
        id: Long,
        request: UserChangePasswordDto.Request,
        operator: Operator,
    ): UserDto.Response =
        userRepository
            .getItemById(id)
            ?.let { user ->
                user
                    .takeIf { it.removedFlag }
                    ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
                user.password
                    ?.takeUnless { verifyPassword(request.oldPassword, it) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                user.password
                    ?.takeIf { it == request.newPassword }
                    ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
                user
            }?.let {
                it.changePassword(request.newPassword, operator)
                it
            }?.let(UserDto.Response::of) ?: throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)

    @Transactional
    fun loginUser(request: UserLoginDto.Request): TokenDto =
        userRepository
            .getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))
            ?.let { user ->
                user
                    .takeIf { it.removedFlag || !user.useFlag }
                    ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
                user.password
                    ?.takeUnless { verifyPassword(request.password, it) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                user
            }?.let {
                it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                it
            }?.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token!!) }
            ?: throw BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT)

    @Transactional
    fun renewToken(refreshToken: String): TokenDto =
        userRepository.getItemById(jwtTokenProvider.getId(refreshToken))?.let { user ->
            user
                .takeIf {
                    user.removedFlag || user.token == null || !jwtTokenProvider.validateToken(refreshToken)
                }?.let { throw Unauthorized401Exception() }
            user.token?.let {
                if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it)) {
                    return TokenDto(jwtTokenProvider.createAccessToken(Operator(user)), it)
                } else if (it == refreshToken) {
                    user.renewToken(jwtTokenProvider.createRefreshToken(Operator(user)))
                    return TokenDto(jwtTokenProvider.createAccessToken(Operator(user)), it)
                }
            }
            throw Unauthorized401Exception()
        } ?: throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)

    @Transactional
    fun logout(id: Long) =
        try {
            userRepository.getItemById(id)?.logout()
        } catch (e: Exception) {
            log.warn(LogUtils.getStackTrace(e))
        }

    fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean =
        userRepository.getItemByMap(
            buildMap {
                put("loginId", loginId)
                put("removedFlag", false)
                id?.let { put("id:not", it) }
            },
        ) == null
}
