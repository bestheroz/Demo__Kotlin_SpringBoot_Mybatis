package com.github.bestheroz.demo.controllers

import com.github.bestheroz.demo.dtos.admin.AdminChangePasswordDto
import com.github.bestheroz.demo.dtos.admin.AdminCreateDto
import com.github.bestheroz.demo.dtos.admin.AdminDto
import com.github.bestheroz.demo.dtos.admin.AdminLoginDto
import com.github.bestheroz.demo.dtos.admin.AdminUpdateDto
import com.github.bestheroz.demo.services.AdminService
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
@RequestMapping("api/v1/admins")
@Tag(name = "Admin", description = "관리자 API")
class AdminController(
    private val adminService: AdminService,
) {
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    fun getAdminList(payload: AdminDto.Request): ListResult<AdminDto.Response> =
        runBlocking {
            adminService.getAdminList(payload)
        }

    @GetMapping("check-login-id")
    @Operation(summary = "로그인 아이디 중복 확인")
    fun checkLoginId(
        @Schema(description = "로그인 아이디") @RequestParam loginId: String,
        @Schema(description = "관리자 ID") @RequestParam(required = false) adminId: Long?,
    ): Boolean = runBlocking { adminService.checkLoginId(loginId, adminId) }

    @PostMapping("login")
    @Operation(summary = "관리자 로그인")
    fun loginAdmin(
        @RequestBody request: AdminLoginDto.Request,
    ): TokenDto =
        runBlocking {
            adminService.loginAdmin(request)
        }

    @GetMapping("{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    fun getAdmin(
        @PathVariable id: Long,
    ): AdminDto.Response =
        runBlocking {
            adminService.getAdmin(id)
        }

    @GetMapping("renew-token")
    @Operation(
        summary = "관리자 토큰 갱신",
        description =
            (
                """*어세스 토큰* 만료 시 *리플래시 토큰* 으로 *어세스 토큰* 을 갱신합니다.
    "(동시에 여러 사용자가 접속하고 있다면 *리플래시 토큰* 값이 달라서 갱신이 안될 수 있습니다.)"""
            ),
    )
    fun renewToken(
        @Schema(description = "리플래시 토큰") @RequestHeader(value = "AuthorizationR") refreshToken: String,
    ): TokenDto = runBlocking { adminService.renewToken(refreshToken) }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ADMIN_EDIT')")
    fun createAdmin(
        @RequestBody request: AdminCreateDto.Request,
        @CurrentUser operator: Operator,
    ): AdminDto.Response = runBlocking { adminService.createAdmin(request, operator) }

    @PutMapping("{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ADMIN_EDIT')")
    fun updateAdmin(
        @PathVariable id: Long,
        @RequestBody request: AdminUpdateDto.Request,
        @CurrentUser operator: Operator,
    ): AdminDto.Response = runBlocking { adminService.updateAdmin(id, request, operator) }

    @PatchMapping("{id}/password")
    @Operation(summary = "관리자 비밀번호 변경")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ADMIN_EDIT')")
    fun changePassword(
        @PathVariable id: Long,
        @RequestBody request: AdminChangePasswordDto.Request,
        @CurrentUser operator: Operator,
    ): AdminDto.Response = runBlocking { adminService.changePassword(id, request, operator) }

    @DeleteMapping("logout")
    @Operation(
        summary = "관리자 로그아웃",
        description = "리플래시 토큰을 삭제합니다.",
        responses = [ApiResponse(responseCode = "204")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    fun logout(
        @CurrentUser operator: Operator,
    ) = runBlocking { adminService.logout(operator.id) }

    @DeleteMapping("{id}")
    @Operation(description = "(Soft delete)", responses = [ApiResponse(responseCode = "204")])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ADMIN_EDIT')")
    fun deleteAdmin(
        @PathVariable id: Long,
        @CurrentUser operator: Operator,
    ) = runBlocking {
        adminService.deleteAdmin(id, operator)
    }
}
