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
        return with(request) {
            ListResult(
                page = page,
                pageSize = pageSize,
                total = count,
                items =
                    if (count == 0L) {
                        emptyList()
                    } else {
                        userRepository
                            .getItemsByMapOrderByLimitOffset(
                                mapOf("removedFlag" to false),
                                listOf("-id"),
                                pageSize,
                                (page - 1) * pageSize,
                            ).apply(operatorHelper::fulfilOperator)
                            .map(UserDto.Response::of)
                    },
            )
        }
    }

    fun getUser(id: Long): UserDto.Response =
        userRepository
            .getItemById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .apply(operatorHelper::fulfilOperator)
            .let(UserDto.Response::of)

    @Transactional
    fun createUser(
        request: UserCreateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        userRepository
            .getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))
            .ifPresent { throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT) }
        return request
            .toEntity(operator)
            .apply {
                userRepository.insert(this)
                operatorHelper.fulfilOperator(this)
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
        val userDeferred = coroutineScope.async(Dispatchers.IO) { userRepository.getItemById(id) }

        userLoginIdDeferred.await().ifPresent {
            BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }

        return userDeferred
            .await()
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .also { if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .apply {
                update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.authorities,
                    operator,
                )
                userRepository.updateById(this, id)
                operatorHelper.fulfilOperator(this)
            }.let(UserDto.Response::of)
    }

    @Transactional
    fun deleteUser(
        id: Long,
        operator: Operator,
    ) {
        userRepository
            .getItemById(id)
            .orElseThrow { (BadRequest400Exception(ExceptionCode.UNKNOWN_USER)) }
            .also {
                if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                if (operator.type == UserTypeEnum.USER && it.id == operator.id) {
                    throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF)
                }
            }.apply {
                remove(operator)
                userRepository.updateById(this, id)
            }
    }

    @Transactional
    fun changePassword(
        id: Long,
        request: UserChangePasswordDto.Request,
        operator: Operator,
    ): UserDto.Response =
        userRepository
            .getItemById(id)
            .orElseThrow { (BadRequest400Exception(ExceptionCode.UNKNOWN_USER)) }
            .also {
                if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                it.password
                    ?.takeUnless { password -> verifyPassword(request.oldPassword, password) }
                    ?.also {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                it.password
                    ?.takeIf { password -> password == request.newPassword }
                    ?.also { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
            }.apply {
                changePassword(request.newPassword, operator)
                userRepository.updateById(this, id)
                operatorHelper.fulfilOperator(this)
            }.let(UserDto.Response::of)

    @Transactional
    fun loginUser(request: UserLoginDto.Request): TokenDto =
        userRepository
            .getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
            .also {
                if (!it.useFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                it.password
                    ?.takeUnless { password -> verifyPassword(request.password, password) }
                    ?.also {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
            }.apply {
                renewToken(jwtTokenProvider.createRefreshToken(Operator(this)))
                userRepository.updateById(this, this.id!!)
                operatorHelper.fulfilOperator(this)
            }.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }

    @Transactional
    fun renewToken(refreshToken: String): TokenDto =
        userRepository
            .getItemById(jwtTokenProvider.getId(refreshToken))
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .also {
                if (it.removedFlag || it.token == null || !jwtTokenProvider.validateToken(refreshToken)) {
                    throw Unauthorized401Exception()
                }
            }.apply {
                token?.let {
                    if (it == refreshToken) {
                        renewToken(jwtTokenProvider.createRefreshToken(Operator(this)))
                        userRepository.updateById(this, this.id!!)
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
            userRepository.getItemById(id).ifPresent { user ->
                user.logout()
                userRepository.updateById(user, id)
            }
        } catch (e: Exception) {
            log.warn(LogUtils.getStackTrace(e))
        }

    fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean =
        userRepository.countByMap(
            buildMap {
                put("loginId", loginId)
                put("removedFlag", false)
                id?.let { put("id:not", it) }
            },
        ) == 0L
}
