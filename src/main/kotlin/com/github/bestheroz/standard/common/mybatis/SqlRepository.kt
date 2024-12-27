package com.github.bestheroz.standard.common.mybatis

import org.apache.ibatis.annotations.*

interface SqlRepository<T : Any> {
    fun getItems(): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = emptySet(),
            whereConditions = emptyMap(),
            orderByConditions = emptyList(),
            limit = null,
            offset = null,
        )

    fun getItemsLimitOffset(
        limit: Int,
        offset: Int,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = emptySet(),
            whereConditions = emptyMap(),
            orderByConditions = emptyList(),
            limit = limit,
            offset = offset,
        )

    fun getItemsOrderBy(orderByConditions: List<String>): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = emptySet(),
            whereConditions = emptyMap(),
            orderByConditions = orderByConditions,
            limit = null,
            offset = null,
        )

    fun getItemsOrderByLimitOffset(
        orderByConditions: List<String>,
        limit: Int,
        offset: Int,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = emptySet(),
            whereConditions = emptyMap(),
            orderByConditions = orderByConditions,
            limit = limit,
            offset = offset,
        )

    fun getItemsByMap(whereConditions: Map<String, Any>): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = emptySet(),
            whereConditions = whereConditions,
            orderByConditions = emptyList(),
            limit = null,
            offset = null,
        )

    fun getItemsByMapLimitOffset(
        whereConditions: Map<String, Any>,
        limit: Int?,
        offset: Int?,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = emptySet(),
            whereConditions = whereConditions,
            orderByConditions = emptyList(),
            limit = limit,
            offset = offset,
        )

    fun getItemsByMapOrderBy(
        whereConditions: Map<String, Any>,
        orderByConditions: List<String>,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = emptySet(),
            whereConditions = whereConditions,
            orderByConditions = orderByConditions,
            limit = null,
            offset = null,
        )

    fun getItemsByMapOrderByLimitOffset(
        whereConditions: Map<String, Any>,
        orderByConditions: List<String>,
        limit: Int,
        offset: Int,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = emptySet(),
            whereConditions = whereConditions,
            orderByConditions = orderByConditions,
            limit = limit,
            offset = offset,
        )

    fun getDistinctItems(distinctColumns: Set<String>): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = distinctColumns,
            targetColumns = emptySet(),
            whereConditions = emptyMap(),
            orderByConditions = emptyList(),
            limit = null,
            offset = null,
        )

    fun getTargetItems(targetColumns: Set<String>): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = targetColumns,
            whereConditions = emptyMap(),
            orderByConditions = emptyList(),
            limit = null,
            offset = null,
        )

    fun getTargetItemsLimitOffset(
        targetColumns: Set<String>,
        limit: Int?,
        offset: Int?,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = targetColumns,
            whereConditions = emptyMap(),
            orderByConditions = emptyList(),
            limit = limit,
            offset = offset,
        )

    fun getTargetItemsOrderBy(
        targetColumns: Set<String>,
        orderByConditions: List<String>,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = targetColumns,
            whereConditions = emptyMap(),
            orderByConditions = orderByConditions,
            limit = null,
            offset = null,
        )

    fun getTargetItemsOrderByLimitOffset(
        targetColumns: Set<String>,
        orderByConditions: List<String>,
        limit: Int?,
        offset: Int?,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = targetColumns,
            whereConditions = emptyMap(),
            orderByConditions = orderByConditions,
            limit = limit,
            offset = offset,
        )

    fun getTargetItemsByMap(
        targetColumns: Set<String>,
        whereConditions: Map<String, Any>,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = targetColumns,
            whereConditions = whereConditions,
            orderByConditions = emptyList(),
            limit = null,
            offset = null,
        )

    fun getTargetItemsByMapLimitOffset(
        targetColumns: Set<String>,
        whereConditions: Map<String, Any>,
        limit: Int?,
        offset: Int?,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = targetColumns,
            whereConditions = whereConditions,
            orderByConditions = emptyList(),
            limit = limit,
            offset = offset,
        )

    fun getTargetItemsByMapOrderBy(
        targetColumns: Set<String>,
        whereConditions: Map<String, Any>,
        orderByConditions: List<String>,
    ): List<T> =
        getTargetItemsByMapOrderByLimitOffset(
            targetColumns = targetColumns,
            whereConditions = whereConditions,
            orderByConditions = orderByConditions,
            limit = null,
            offset = null,
        )

    fun getTargetItemsByMapOrderByLimitOffset(
        targetColumns: Set<String>,
        whereConditions: Map<String, Any>,
        orderByConditions: List<String>,
        limit: Int?,
        offset: Int?,
    ): List<T> =
        getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = targetColumns,
            whereConditions = whereConditions,
            orderByConditions = orderByConditions,
            limit = limit,
            offset = offset,
        )

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_ITEMS)
    fun getDistinctAndTargetItemsByMapOrderByLimitOffset(
        distinctColumns: Set<String>,
        targetColumns: Set<String>,
        whereConditions: Map<String, Any>,
        orderByConditions: List<String>,
        limit: Int?,
        offset: Int?,
    ): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_ITEM_BY_MAP)
    fun getItemByMap(whereConditions: Map<String, Any>): T?

    fun getItemById(id: Long): T? = getItemByMap(mapOf("id" to id))

    fun countAll(): Long = countByMap(emptyMap())

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.COUNT_BY_MAP)
    fun countByMap(whereConditions: Map<String, Any>): Long

    @InsertProvider(type = SqlCommand::class, method = SqlCommand.INSERT)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(entity: T)

    @InsertProvider(type = SqlCommand::class, method = SqlCommand.INSERT_BATCH)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insertBatch(entities: List<T>)

    fun updateById(
        entity: T,
        id: Long,
    ) {
        updateMapByMap(SqlCommand.toMap(entity), mapOf("id" to id))
    }

    fun updateByMap(
        entity: T,
        whereConditions: Map<String, Any>,
    ) {
        updateMapByMap(SqlCommand.toMap(entity), whereConditions)
    }

    @UpdateProvider(type = SqlCommand::class, method = SqlCommand.UPDATE_MAP_BY_MAP)
    fun updateMapByMap(
        updateMap: Map<String, Any>,
        whereConditions: Map<String, Any>,
    )

    fun updateMapById(
        updateMap: Map<String, Any>,
        id: Long,
    ) {
        updateMapByMap(updateMap, mapOf("id" to id))
    }

    @DeleteProvider(type = SqlCommand::class, method = SqlCommand.DELETE_BY_MAP)
    fun deleteByMap(whereConditions: Map<String, Any>)

    fun deleteById(id: Long) {
        deleteByMap(mapOf("id" to id))
    }
}
