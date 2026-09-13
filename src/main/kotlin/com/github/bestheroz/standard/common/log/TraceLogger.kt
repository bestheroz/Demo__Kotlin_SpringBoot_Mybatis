package com.github.bestheroz.standard.common.log

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

@Aspect
@Component
class TraceLogger(
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @Around(
        """
        execution(!private * com.github.bestheroz..*Controller.*(..)) ||
        execution(!private * com.github.bestheroz..*Service.*(..)) ||
        execution(!private * com.github.bestheroz..*Repository.*(..))
        """,
    )
    @Throws(Throwable::class)
    fun writeLog(pjp: ProceedingJoinPoint): Any? {
        val signature =
            pjp.staticPart.signature
                .toString()
                .removePrefix(pjp.staticPart.signature.declaringType.`package`.name + ".")

        if (signature.containsAny("HealthController", "HealthRepository")) {
            return pjp.proceed()
        }

        val stopWatch = StopWatch(signature)
        stopWatch.start()

        return try {
            logger.info { "$signature START ......." }

            val retVal = pjp.proceed()
            stopWatch.stop()

            when {
                // mybatis-repository 0.10.0 부터 기본 메서드가 MybatisRepositoryBase 에 선언되어 signature 가 "RepositoryBase." 로 찍힌다.
                // 이를 놓치면 작성자 정보가 채워지기 전 엔티티를 직렬화하다 예외가 나므로 함께 건너뛴다.
                signature.containsAny("Repository.", "RepositoryBase.", "RepositoryCustom.", ".domain.") -> {
                    logger.info { "$signature E N D [${stopWatch.totalTimeMillis}ms]" }
                }

                else -> {
                    logger.info {
                        val returnValue =
                            retVal?.run {
                                val str = objectMapper.writeValueAsString(retVal)
                                str.abbreviate(1000, "--skip massive text-- total length : ${str.length}")
                            } ?: "null"
                        "$signature E N D [${stopWatch.totalTimeMillis}ms] - return: $returnValue"
                    }
                }
            }

            retVal
        } catch (e: Throwable) {
            if (stopWatch.isRunning) {
                stopWatch.stop()
            }
            logger.info { "$signature THROW [${stopWatch.totalTimeMillis}ms]" }
            throw e
        }
    }

    private fun String.containsAny(vararg substrings: String): Boolean = substrings.any { this.contains(it) }

    private fun String?.abbreviate(
        maxWidth: Int,
        abbrevMarker: String,
    ): String {
        val str = this ?: "null"
        return if (str.length <= maxWidth) str else str.take(maxWidth) + abbrevMarker
    }
}
