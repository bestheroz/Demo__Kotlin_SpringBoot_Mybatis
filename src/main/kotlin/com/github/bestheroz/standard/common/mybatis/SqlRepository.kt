package com.github.bestheroz.standard.common.mybatis

import org.apache.ibatis.annotations.*

interface SqlRepository<T : Any> {
    @SelectProvider(type = SqlCommand::class, method = SqlCommand.COUNT_ALL)
    fun countAll(): Long

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.COUNT_BY_MAP)
    fun countByMap(whereConditions: Map<String, Any>): Long

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_ITEMS)
    fun getItems(): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_ITEMS_LIMIT_OFFSET)
    fun getItemsLimitOffset(
        limit: Int,
        offset: Int,
    ): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_ITEMS_ORDER_BY)
    fun getItemsOrderBy(orderByConditions: List<String>): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_ITEMS_ORDER_BY_LIMIT_OFFSET)
    fun getItemsOrderByLimitOffset(
        orderByConditions: List<String>,
        limit: Int,
        offset: Int,
    ): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_ITEMS_BY_MAP)
    fun getItemsByMap(whereConditions: Map<String, Any>): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_ITEMS_BY_MAP_LIMIT_OFFSET)
    fun getItemsByMapLimitOffset(
        whereConditions: Map<String, Any>,
        limit: Int?,
        offset: Int?,
    ): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_ITEMS_BY_MAP_ODER_BY)
    fun getItemsByMapOrderBy(
        whereConditions: Map<String, Any>,
        orderByConditions: List<String>,
    ): List<T>

    @SelectProvider(
        type = SqlCommand::class,
        method = SqlCommand.SELECT_ITEMS_BY_MAP_ODER_BY_LIMIT_OFFSET,
    )
    fun getItemsByMapOrderByLimitOffset(
        whereConditions: Map<String, Any>,
        orderByConditions: List<String>,
        limit: Int,
        offset: Int,
    ): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_DISTINCT_ITEMS)
    fun getDistinctItems(distinctColumns: Set<String>): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_TARGET_ITEMS)
    fun getTargetItems(targetColumns: Set<String>): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_TARGET_ITEMS_LIMIT_OFFSET)
    fun getTargetItemsLimitOffset(
        targetColumns: Set<String>,
        limit: Int?,
        offset: Int?,
    ): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_TARGET_ITEMS_ORDER_BY)
    fun getTargetItemsOrderBy(
        targetColumns: Set<String>,
        orderByConditions: List<String>,
    ): List<T>

    @SelectProvider(
        type = SqlCommand::class,
        method = SqlCommand.SELECT_TARGET_ITEMS_ORDER_BY_LIMIT_OFFSET,
    )
    fun getTargetItemsOrderByLimitOffset(
        targetColumns: Set<String>,
        orderByConditions: List<String>,
        limit: Int?,
        offset: Int?,
    ): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_TARGET_ITEMS_BY_MAP)
    fun getTargetItemsByMap(
        targetColumns: Set<String>,
        whereConditions: Map<String, Any>,
    ): List<T>

    @SelectProvider(
        type = SqlCommand::class,
        method = SqlCommand.SELECT_TARGET_ITEMS_BY_MAP_LIMIT_OFFSET,
    )
    fun getTargetItemsByMapLimitOffset(
        targetColumns: Set<String>,
        whereConditions: Map<String, Any>,
        limit: Int?,
        offset: Int?,
    ): List<T>

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_TARGET_ITEMS_BY_MAP_ODER_BY)
    fun getTargetItemsByMapOrderBy(
        targetColumns: Set<String>,
        whereConditions: Map<String, Any>,
        orderByConditions: List<String>,
    ): List<T>

    @SelectProvider(
        type = SqlCommand::class,
        method = SqlCommand.SELECT_TARGET_ITEMS_BY_MAP_ODER_BY_LIMIT_OFFSET,
    )
    fun getTargetItemsByMapOrderByLimitOffset(
        targetColumns: Set<String>,
        whereConditions: Map<String, Any>,
        orderByConditions: List<String>,
        limit: Int?,
        offset: Int?,
    ): List<T>

    @SelectProvider(
        type = SqlCommand::class,
        method = SqlCommand.SELECT_DISTINCT_AND_TARGET_ITEMS_BY_MAP_ODER_BY_LIMIT_OFFSET,
    )
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

    @SelectProvider(type = SqlCommand::class, method = SqlCommand.SELECT_ITEM_BY_ID)
    fun getItemById(id: Long): T?

    @InsertProvider(type = SqlCommand::class, method = SqlCommand.INSERT)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(entity: T)

    @InsertProvider(type = SqlCommand::class, method = SqlCommand.INSERT_BATCH)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insertBatch(entities: List<T>)

    @UpdateProvider(type = SqlCommand::class, method = SqlCommand.UPDATE_BY_ID)
    fun updateById(
        entity: T,
        id: Long,
    )

    @UpdateProvider(type = SqlCommand::class, method = SqlCommand.UPDATE_BY_MAP)
    fun updateByMap(
        entity: T,
        whereConditions: Map<String, Any>,
    )

    @UpdateProvider(type = SqlCommand::class, method = SqlCommand.UPDATE_MAP_BY_ID)
    fun updateMapById(
        updateMap: Map<String, Any>,
        id: Long,
    )

    @UpdateProvider(type = SqlCommand::class, method = SqlCommand.UPDATE_MAP_BY_MAP)
    fun updateMapByMap(
        updateMap: Map<String, Any>,
        whereConditions: Map<String, Any>,
    )

    @DeleteProvider(type = SqlCommand::class, method = SqlCommand.DELETE_BY_MAP)
    fun deleteByMap(whereConditions: Map<String, Any>)

    @DeleteProvider(type = SqlCommand::class, method = SqlCommand.DELETE_BY_ID)
    fun deleteById(id: Long)
}
