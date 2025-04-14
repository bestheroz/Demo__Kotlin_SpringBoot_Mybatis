package com.github.bestheroz.standard.common.authenticate

import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.config.SecurityConfig
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.StopWatch
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.UrlPathHelper
import java.io.IOException
import java.util.*

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {
    companion object {
        private const val REQUEST_COMPLETE_EXECUTE_TIME =
            "{} ....... Request Complete Execute Time ....... : {}"
        private const val REQUEST_PARAMETERS = "<{}>{}?{}"
        private val log = logger()
    }

    private val publicGetPaths: List<AntPathRequestMatcher> =
        Arrays
            .stream(SecurityConfig.GET_PUBLIC)
            .map { pattern: String? -> AntPathRequestMatcher(pattern) }
            .toList()
    private val publicPostPaths: List<AntPathRequestMatcher> =
        Arrays
            .stream(SecurityConfig.POST_PUBLIC)
            .map { pattern: String? -> AntPathRequestMatcher(pattern) }
            .toList()
    private val publicDeletePaths: List<AntPathRequestMatcher> =
        Arrays
            .stream(SecurityConfig.DELETE_PUBLIC)
            .map { pattern: String? -> AntPathRequestMatcher(pattern) }
            .toList()

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestURI: String = UrlPathHelper().getPathWithinApplication(request)

        if (requestURI == "/") {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }

        if (!requestURI.startsWith("/api/v1/health/")) {
            log.info(
                REQUEST_PARAMETERS,
                request.method,
                requestURI,
                StringUtils.defaultString(request.queryString),
            )
        }

        val stopWatch = StopWatch()
        stopWatch.start()

        try {
            if (isPublicPath(request)) {
                filterChain.doFilter(request, response)
                return
            }

            val token = jwtTokenProvider.resolveAccessToken(request)
            if (token == null) {
                log.info("No access token found")
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                return
            }

            if (!jwtTokenProvider.validateToken(token)) {
                log.info("Invalid access token - refresh token required")
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                return
            }

            val userDetails: UserDetails = jwtTokenProvider.getOperator(token)
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)

            filterChain.doFilter(request, response)
        } finally {
            stopWatch.stop()
            if (!requestURI.startsWith("/api/v1/health/")) {
                log.info(REQUEST_COMPLETE_EXECUTE_TIME, requestURI, stopWatch)
            }
        }
    }

    private fun isPublicPath(request: HttpServletRequest): Boolean =
        when (request.method) {
            HttpMethod.GET.toString() -> {
                publicGetPaths.stream().anyMatch { matcher: AntPathRequestMatcher ->
                    matcher.matches(request)
                }
            }
            HttpMethod.POST.toString() -> {
                publicPostPaths.stream().anyMatch { matcher: AntPathRequestMatcher ->
                    matcher.matches(request)
                }
            }
            HttpMethod.DELETE.toString() -> {
                publicDeletePaths.stream().anyMatch { matcher: AntPathRequestMatcher ->
                    matcher.matches(request)
                }
            }
            else -> {
                false
            }
        }
}
