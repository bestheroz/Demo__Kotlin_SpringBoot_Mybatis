package com.github.bestheroz.demo.dtos.user

import io.swagger.v3.oas.annotations.media.Schema

class UserLoginDto {
    data class Request(
        @Schema(description = "로그인 아이디", requiredMode = Schema.RequiredMode.REQUIRED)
        val loginId: String,
        @Schema(
            description = "비밀번호",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example =
                "4dff4ea340f0a823f15d3f4f01ab62eae0e5da579ccb851f8db9dfe84c58b2b37b89903a740e1ee172da793a6e79d560e5f7f9bd058a12a280433ed6fa46510a",
        )
        val password: String,
    )
}