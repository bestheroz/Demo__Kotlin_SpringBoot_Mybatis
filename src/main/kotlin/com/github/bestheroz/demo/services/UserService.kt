package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.domain.service.OperatorHelper
import com.github.bestheroz.demo.dtos.user.*
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
import com.github.bestheroz.standard.common.util.PasswordUtil.isPasswordValid
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val operatorHelper: OperatorHelper,
) {
    companion object {
        private val log = logger()
    }

    suspend fun getUserList(request: UserDto.Request): ListResult<UserDto.Response> =
        coroutineScope {
            val filterMap =
                buildMap {
                    put("removedFlag", false)
                    request.id?.let { put("id", it) }
                    request.loginId?.let { put("loginId:contains", it) }
                    request.name?.let { put("name:contains", it) }
                    request.useFlag?.let { put("useFlag", it) }
                }

            val countDeferred = async(Dispatchers.IO) { userRepository.countByMap(filterMap) }
            val itemsDeferred =
                async(Dispatchers.IO) {
                    userRepository.getItemsByMapOrderByLimitOffset(
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
                    operatorHelper.fulfilOperator(itemsDeferred.await()).map(UserDto.Response::of)
                }

            ListResult(page = request.page, pageSize = request.pageSize, total = count, items = items)
        }

    suspend fun getUser(id: Long): UserDto.Response =
        withContext(Dispatchers.IO) { userRepository.getItemById(id) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .let { operatorHelper.fulfilOperator(it) }
            .let(UserDto.Response::of)

    @Transactional
    suspend fun createUser(
        request: UserCreateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        withContext(Dispatchers.IO) {
            userRepository.getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))
        }.ifPresent { throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT) }
        return request
            .toEntity(operator)
            .let {
                withContext(Dispatchers.IO) { userRepository.insert(it) }
                operatorHelper.fulfilOperator(it)
            }.let(UserDto.Response::of)
    }

    @Transactional
    suspend fun updateUser(
        id: Long,
        request: UserUpdateDto.Request,
        operator: Operator,
    ): UserDto.Response =
        coroutineScope {
            val userLoginIdDeferred =
                async(Dispatchers.IO) {
                    userRepository.getItemByMap(
                        mapOf("loginId" to request.loginId, "removedFlag" to false, "id:not" to id),
                    )
                }
            val userDeferred = async(Dispatchers.IO) { userRepository.getItemById(id) }

            userLoginIdDeferred.await().ifPresent {
                userDeferred.cancel()
                throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
            }

            userDeferred
                .await()
                .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
                .also { if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
                .let {
                    it.update(
                        request.loginId,
                        request.password,
                        request.name,
                        request.useFlag,
                        request.authorities,
                        operator,
                    )
                    withContext(Dispatchers.IO) { userRepository.updateById(it, id) }
                    operatorHelper.fulfilOperator(it)
                }.let(UserDto.Response::of)
        }

    @Transactional
    suspend fun deleteUser(
        id: Long,
        operator: Operator,
    ) {
        withContext(Dispatchers.IO) { userRepository.getItemById(id) }
            .orElseThrow { (BadRequest400Exception(ExceptionCode.UNKNOWN_USER)) }
            .also {
                if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                if (operator.type == UserTypeEnum.USER && it.id == operator.id) {
                    throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF)
                }
            }.let {
                it.remove(operator)
                withContext(Dispatchers.IO) { userRepository.updateById(it, id) }
            }
    }

    @Transactional
    suspend fun changePassword(
        id: Long,
        request: UserChangePasswordDto.Request,
        operator: Operator,
    ): UserDto.Response =
        withContext(Dispatchers.IO) { userRepository.getItemById(id) }
            .orElseThrow { (BadRequest400Exception(ExceptionCode.UNKNOWN_USER)) }
            .also {
                if (it.removedFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                it.password
                    ?.takeUnless { password -> isPasswordValid(request.oldPassword, password) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                it.password
                    ?.takeIf { password -> password == request.newPassword }
                    ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
            }.let {
                it.changePassword(request.newPassword, operator)
                withContext(Dispatchers.IO) { userRepository.updateById(it, id) }
                operatorHelper.fulfilOperator(it)
            }.let(UserDto.Response::of)

    @Transactional
    suspend fun loginUser(request: UserLoginDto.Request): TokenDto =
        withContext(Dispatchers.IO) {
            userRepository.getItemByMap(mapOf("loginId" to request.loginId, "removedFlag" to false))
        }.orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
            .also {
                if (!it.useFlag) throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                it.password
                    ?.takeUnless { password -> isPasswordValid(request.password, password) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
            }.let {
                it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                withContext(Dispatchers.IO) { userRepository.updateById(it, it.id!!) }
                operatorHelper.fulfilOperator(it)
            }.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }

    @Transactional
    suspend fun renewToken(refreshToken: String): TokenDto =
        withContext(Dispatchers.IO) { userRepository.getItemById(jwtTokenProvider.getId(refreshToken)) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .also {
                if (it.removedFlag || it.token == null || !jwtTokenProvider.validateToken(refreshToken)) {
                    throw Unauthorized401Exception()
                }
            }.let {
                it.token?.let { token ->
                    if (token == refreshToken) {
                        it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                        withContext(Dispatchers.IO) { userRepository.updateById(it, it.id!!) }
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
            val optionalUser = withContext(Dispatchers.IO) { userRepository.getItemById(id) }

            if (optionalUser.isPresent) {
                val user = optionalUser.get()
                user.logout()
                withContext(Dispatchers.IO) { userRepository.updateById(user, id) }
            }
        } catch (e: Exception) {
            log.warn(LogUtils.getStackTrace(e))
        }
    }

    suspend fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            userRepository.countByMap(
                buildMap {
                    put("loginId", loginId)
                    put("removedFlag", false)
                    id?.let { put("id:not", it) }
                },
            )
        } == 0L
}
