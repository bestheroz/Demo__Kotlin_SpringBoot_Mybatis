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
        return ListResult(
            page = request.page,
            pageSize = request.pageSize,
            total = count,
            items =
                if (count == 0L) {
                    emptyList()
                } else {
                    noticeRepository
                        .getItemsByMapOrderByLimitOffset(
                            mapOf("removedFlag" to false),
                            listOf("-id"),
                            request.pageSize,
                            (request.page - 1) * request.pageSize,
                        ).let(operatorHelper::fulfilOperator)
                        .map(NoticeDto.Response::of)
                },
        )
    }

    fun getNotice(id: Long): NoticeDto.Response =
        noticeRepository
            .getItemById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
            .let {
                operatorHelper.fulfilOperator(it)
                it
            }.let(NoticeDto.Response::of)

    @Transactional
    fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        request
            .toEntity(operator)
            .let {
                noticeRepository.insert(it)
                it
            }.let {
                operatorHelper.fulfilOperator(it)
                it
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
            .let {
                it.update(request.title, request.content, request.useFlag, operator)
                noticeRepository.updateById(it, id)
                it
            }.let {
                operatorHelper.fulfilOperator(it)
                it
            }.let(NoticeDto.Response::of)

    @Transactional
    fun deleteNotice(
        id: Long,
        operator: Operator,
    ) = noticeRepository
        .getItemById(id)
        .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
        .let {
            it.remove(operator)
            noticeRepository.updateById(it, id)
        }
}
