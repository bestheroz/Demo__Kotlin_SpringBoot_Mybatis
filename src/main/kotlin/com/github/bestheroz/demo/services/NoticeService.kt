package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.domain.service.OperatorHelper
import com.github.bestheroz.demo.dtos.notice.NoticeCreateDto
import com.github.bestheroz.demo.dtos.notice.NoticeDto
import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.security.Operator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
    private val operatorHelper: OperatorHelper,
) {
    suspend fun getNoticeList(request: NoticeDto.Request): ListResult<NoticeDto.Response> =
        coroutineScope {
            val countDeferred =
                async(Dispatchers.IO) { noticeRepository.countByMap(mapOf("removedFlag" to false)) }
            val itemsDeferred =
                async(Dispatchers.IO) {
                    noticeRepository.getItemsByMapOrderByLimitOffset(
                        mapOf("removedFlag" to false),
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
                    operatorHelper.fulfilOperator(itemsDeferred.await()).map(NoticeDto.Response::of)
                }

            ListResult(page = request.page, pageSize = request.pageSize, total = count, items = items)
        }

    suspend fun getNotice(id: Long): NoticeDto.Response =
        withContext(Dispatchers.IO) { noticeRepository.getItemById(id) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
            .let { operatorHelper.fulfilOperator(it) }
            .let(NoticeDto.Response::of)

    @Transactional
    suspend fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        request
            .toEntity(operator)
            .let {
                withContext(Dispatchers.IO) { noticeRepository.insert(it) }
                operatorHelper.fulfilOperator(it)
            }.let(NoticeDto.Response::of)

    @Transactional
    suspend fun updateNotice(
        id: Long,
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        withContext(Dispatchers.IO) { noticeRepository.getItemById(id) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
            .let {
                it.update(request.title, request.content, request.useFlag, operator)
                withContext(Dispatchers.IO) { noticeRepository.updateById(it, id) }
                operatorHelper.fulfilOperator(it)
            }.let(NoticeDto.Response::of)

    @Transactional
    suspend fun deleteNotice(
        id: Long,
        operator: Operator,
    ) {
        withContext(Dispatchers.IO) { noticeRepository.getItemById(id) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
            .let {
                it.remove(operator)
                withContext(Dispatchers.IO) { noticeRepository.updateById(it, id) }
            }
    }
}
