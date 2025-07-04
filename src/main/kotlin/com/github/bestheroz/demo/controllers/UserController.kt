package com.github.bestheroz.demo.controllers

import com.github.bestheroz.demo.dtos.user.UserChangePasswordDto
import com.github.bestheroz.demo.dtos.user.UserCreateDto
import com.github.bestheroz.demo.dtos.user.UserDto
import com.github.bestheroz.demo.dtos.user.UserLoginDto
import com.github.bestheroz.demo.dtos.user.UserUpdateDto
import com.github.bestheroz.demo.services.UserService
import com.github.bestheroz.standard.common.authenticate.CurrentUser
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.security.Operator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/users")
@Tag(name = "User", description = "유저 API")
class UserController(
    private val userService: UserService,
) {
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    fun getUserList(payload: UserDto.Request): ListResult<UserDto.Response> =
        runBlocking {
            userService.getUserList(payload)
        }

    @GetMapping("check-login-id")
    @Operation(summary = "로그인 아이디 중복 확인")
    fun checkLoginId(
        @Schema(description = "로그인 아이디") @RequestParam loginId: String,
        @Schema(description = "유저 ID") @RequestParam(required = false) userId: Long?,
    ): Boolean = runBlocking { userService.checkLoginId(loginId, userId) }

    @PostMapping("login")
    @Operation(summary = "유저 로그인")
    fun loginUser(
        @RequestBody request: UserLoginDto.Request,
    ): TokenDto =
        runBlocking {
            userService.loginUser(request)
        }

    @GetMapping("{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    fun getUser(
        @PathVariable id: Long,
    ): UserDto.Response = runBlocking { userService.getUser(id) }

    @GetMapping("renew-token")
    @Operation(
        summary = "유저 토큰 갱신",
        description =
            (
                """*어세스 토큰* 만료 시 *리플래시 토큰* 으로 *어세스 토큰* 을 갱신합니다.
    "(동시에 여러 사용자가 접속하고 있다면 *리플래시 토큰* 값이 달라서 갱신이 안될 수 있습니다.)"""
            ),
    )
    fun renewToken(
        @Schema(description = "리플래시 토큰") @RequestHeader(value = "AuthorizationR") refreshToken: String,
    ): TokenDto = runBlocking { userService.renewToken(refreshToken) }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    fun createUser(
        @RequestBody request: UserCreateDto.Request,
        @CurrentUser operator: Operator,
    ): UserDto.Response = runBlocking { userService.createUser(request, operator) }

    @PutMapping("{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody request: UserUpdateDto.Request,
        @CurrentUser operator: Operator,
    ): UserDto.Response = runBlocking { userService.updateUser(id, request, operator) }

    @PatchMapping("{id}/password")
    @Operation(summary = "유저 비밀번호 변경")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    fun changePassword(
        @PathVariable id: Long,
        @RequestBody request: UserChangePasswordDto.Request,
        @CurrentUser operator: Operator,
    ): UserDto.Response = runBlocking { userService.changePassword(id, request, operator) }

    @DeleteMapping("logout")
    @Operation(
        summary = "유저 로그아웃",
        description = "리플래시 토큰을 삭제합니다.",
        responses = [ApiResponse(responseCode = "204")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    fun logout(
        @CurrentUser operator: Operator,
    ) = runBlocking { userService.logout(operator.id) }

    @DeleteMapping("{id}")
    @Operation(description = "(Soft delete)", responses = [ApiResponse(responseCode = "204")])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    fun deleteUser(
        @PathVariable id: Long,
        @CurrentUser operator: Operator,
    ) = runBlocking {
        userService.deleteUser(id, operator)
    }
}
