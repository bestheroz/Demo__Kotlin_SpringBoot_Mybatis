package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.domain.service.OperatorHelper
import com.github.bestheroz.demo.dtos.notice.NoticeCreateDto
import com.github.bestheroz.demo.dtos.notice.NoticeDto
import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.security.Operator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
    private val operatorHelper: OperatorHelper,
) {
    fun getNoticeList(request: NoticeDto.Request): ListResult<NoticeDto.Response> {
        val count = noticeRepository.countByMap(mapOf("removedFlag" to false))
        return with(request) {
            ListResult(
                page = page,
                pageSize = pageSize,
                total = count,
                items =
                    if (count == 0L) {
                        emptyList()
                    } else {
                        noticeRepository
                            .getItemsByMapOrderByLimitOffset(
                                mapOf("removedFlag" to false),
                                listOf("-id"),
                                pageSize,
                                (page - 1) * pageSize,
                            ).apply(operatorHelper::fulfilOperator)
                            .map(NoticeDto.Response::of)
                    },
            )
        }
    }

    fun getNotice(id: Long): NoticeDto.Response =
        noticeRepository
            .getItemById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
            .apply(operatorHelper::fulfilOperator)
            .let(NoticeDto.Response::of)

    @Transactional
    fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        request
            .toEntity(operator)
            .apply {
                noticeRepository.insert(this)
                operatorHelper.fulfilOperator(this)
            }.let(NoticeDto.Response::of)

    @Transactional
    fun updateNotice(
        id: Long,
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        noticeRepository
            .getItemById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
            .apply {
                update(request.title, request.content, request.useFlag, operator)
                noticeRepository.updateById(this, id)
                operatorHelper.fulfilOperator(this)
            }.let(NoticeDto.Response::of)

    @Transactional
    fun deleteNotice(
        id: Long,
        operator: Operator,
    ) {
        noticeRepository
            .getItemById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
            .apply {
                remove(operator)
                noticeRepository.updateById(this, id)
            }
    }
}
