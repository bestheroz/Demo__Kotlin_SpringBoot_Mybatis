package com.github.bestheroz.standard.common.dto

data class ListResult<T>(
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val items: List<T>,
)
