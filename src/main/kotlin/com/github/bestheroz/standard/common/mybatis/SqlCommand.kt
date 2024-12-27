package com.github.bestheroz.standard.common.mybatis

import com.github.bestheroz.standard.common.enums.ValueEnum
import com.github.bestheroz.standard.common.log.logger
import jakarta.persistence.Table
import org.apache.ibatis.jdbc.SQL
import java.lang.reflect.Field
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SqlCommand {
    companion object {
        val log = logger()
        const val SELECT_ITEMS = "getDistinctAndTargetItemsByMapOrderByLimitOffset"
        const val SELECT_ITEM_BY_MAP = "getItemByMap"
        const val COUNT_BY_MAP = "countByMap"
        const val COUNT_BY_DATATABLE = "countForDataTable"
        const val INSERT = "insert"
        const val INSERT_BATCH = "insertBatch"
        const val UPDATE_MAP_BY_MAP = "updateMapByMap"
        const val DELETE_BY_MAP = "deleteByMap"

        private val EXCLUDE_FIELD_SET =
            setOf(
                "SERIAL_VERSION_U_I_D",
                "serialVersionUID",
                "E_N_C_R_Y_P_T_E_D__C_O_L_U_M_N__L_I_S_T",
                "updater",
                "updatedByAdmin",
                "updatedByUser",
                "creator",
                "createdByAdmin",
                "createdByUser",
                "Companion",
            )

        private val METHOD_LIST =
            setOf(
                SELECT_ITEMS,
                SELECT_ITEM_BY_MAP,
                COUNT_BY_MAP,
                COUNT_BY_DATATABLE,
                INSERT,
                INSERT_BATCH,
                UPDATE_MAP_BY_MAP,
                DELETE_BY_MAP,
            )

        fun toMap(source: Any): Map<String, Any> =
            buildMap {
                getAllFields(source::class.java).forEach { field ->
                    field.isAccessible = true
                    try {
                        put(field.name, field.get(source))
                    } catch (e: Exception) {
                        log.warn(e.stackTraceToString())
                    }
                }
            }

        fun getTableName(entityClass: Class<*>): String =
            entityClass.getAnnotation(Table::class.java)?.name
                ?: entityClass.simpleName.camelCaseToSnakeCase().lowercase()

        private fun getAllFields(clazz: Class<*>): Array<Field> {
            val fields = mutableListOf<Field>()
            var currentClass: Class<*>? = clazz

            while (currentClass != null && currentClass != Any::class.java) {
                fields.addAll(currentClass.declaredFields)
                currentClass = currentClass.superclass
            }

            return fields.toTypedArray()
        }

        private fun String.camelCaseToSnakeCase(): String =
            buildString {
                append(this@camelCaseToSnakeCase.first().lowercase())
                this@camelCaseToSnakeCase.drop(1).forEach { char ->
                    if (char.isUpperCase()) {
                        append('_').append(char.lowercase())
                    } else {
                        append(char)
                    }
                }
            }
    }

    fun getTableName(): String = getTableName(getEntityClass())

    private fun verifyWhereKey(whereConditions: Map<String, Any>?) {
        if (whereConditions.isNullOrEmpty()) {
            log.warn("whereConditions is empty")
            throw RuntimeException("'where' Conditions is required")
        }
    }

    fun countByMap(whereConditions: Map<String, Any>): String =
        SQL()
            .apply {
                SELECT("COUNT(1) AS CNT")
                FROM(getTableName())
                getWhereSql(this, whereConditions)
            }.toString()

    private fun getEntityClass(): Class<*> =
        Throwable()
            .stackTrace
            .firstOrNull { isValidStackTraceElement(it) }
            ?.let { getClassFromStackTraceElement(it) }
            ?: throw RuntimeException("stackTraceElements is required")

    private fun isValidStackTraceElement(element: StackTraceElement): Boolean =
        try {
            val clazz = Class.forName(element.className)
            METHOD_LIST.contains(element.methodName) &&
                clazz.interfaces.isNotEmpty() &&
                clazz.interfaces[0].genericInterfaces.isNotEmpty()
        } catch (e: ClassNotFoundException) {
            log.warn(e.stackTraceToString())
            false
        }

    private fun getClassFromStackTraceElement(element: StackTraceElement): Class<*> =
        try {
            Class.forName(
                Class
                    .forName(element.className)
                    .interfaces[0]
                    .genericInterfaces[0]
                    .typeName
                    .substringAfter("<")
                    .substringBefore(">"),
            )
        } catch (e: ClassNotFoundException) {
            log.warn(e.stackTraceToString())
            throw RuntimeException("Failed::ClassNotFoundException", e)
        }

    private fun getEntityFields(): Set<String> = getEntityFields(getEntityClass())

    private fun <T> getEntityFields(entity: Class<T>): Set<String> =
        getAllFields(entity)
            .map { it.name }
            .distinct()
            .filter { !EXCLUDE_FIELD_SET.contains(it) }
            .toSet()

    fun getDistinctAndTargetItemsByMapOrderByLimitOffset(
        distinctColumns: Set<String>,
        targetColumns: Set<String>,
        whereConditions: Map<String, Any>,
        orderByConditions: List<String>,
        limit: Int?,
        offset: Int?,
    ): String =
        SQL()
            .apply {
                when {
                    distinctColumns.isEmpty() && targetColumns.isEmpty() -> {
                        getEntityFields().forEach { field ->
                            SELECT(wrapIdentifier(field.camelCaseToSnakeCase()))
                        }
                    }
                    else -> {
                        distinctColumns.forEach { column ->
                            SELECT_DISTINCT(wrapIdentifier(column.camelCaseToSnakeCase()))
                        }
                        targetColumns
                            .filter { it !in distinctColumns }
                            .forEach { column -> SELECT(wrapIdentifier(column.camelCaseToSnakeCase())) }
                    }
                }

                FROM(getTableName())
                limit?.let { LIMIT(it) }
                offset?.let { OFFSET(it.toLong()) }
                getWhereSql(this, whereConditions)

                orderByConditions.forEach { condition ->
                    val column = condition.camelCaseToSnakeCase()
                    ORDER_BY(
                        when {
                            column.startsWith("-") -> "${wrapIdentifier(column.substring(1))} desc"
                            else -> wrapIdentifier(column)
                        },
                    )
                }
            }.toString()

    fun getItemByMap(whereConditions: Map<String, Any>): String {
        verifyWhereKey(whereConditions)
        return getDistinctAndTargetItemsByMapOrderByLimitOffset(
            distinctColumns = emptySet(),
            targetColumns = emptySet(),
            whereConditions = whereConditions,
            orderByConditions = emptyList(),
            limit = null,
            offset = null,
        )
    }

    fun insert(entity: Any): String =
        SQL()
            .apply {
                INSERT_INTO(getTableName(entity::class.java))
                toMap(entity).forEach { (key, value) ->
                    if (key !in EXCLUDE_FIELD_SET) {
                        VALUES(wrapIdentifier(key.camelCaseToSnakeCase()), getFormattedValue(value))
                    }
                }
            }.toString()

    fun insertBatch(entities: List<Any>): String {
        require(entities.isNotEmpty()) { "entities empty" }

        return SQL()
            .apply {
                val firstEntity = entities.first()
                INSERT_INTO(wrapIdentifier(getTableName(firstEntity::class.java)))

                val columns =
                    getEntityFields(firstEntity::class.java)
                        .filter { it !in EXCLUDE_FIELD_SET }
                        .map { wrapIdentifier(it.camelCaseToSnakeCase()) }

                INTO_COLUMNS(columns.joinToString(", "))

                val valuesList =
                    entities.map { entity ->
                        toMap(entity).map { (column, value) -> getFormattedValue(value) }
                    }

                INTO_VALUES(valuesList.joinToString(") (") { it.joinToString(", ") })
            }.toString()
    }

    fun updateMapByMap(
        updateMap: Map<String, Any>,
        whereConditions: Map<String, Any>,
    ): String {
        verifyWhereKey(whereConditions)
        return SQL()
            .apply {
                UPDATE(getTableName())
                updateMap.forEach { (javaFieldName, value) ->
                    if (javaFieldName !in EXCLUDE_FIELD_SET) {
                        SET(getEqualSql(javaFieldName.camelCaseToSnakeCase(), value))
                    }
                }
                getWhereSql(this, whereConditions)
                if (!toString().contains("WHERE ", ignoreCase = true)) {
                    throw RuntimeException("whereConditions are empty")
                }
            }.toString()
    }

    fun deleteByMap(whereConditions: Map<String, Any>): String {
        verifyWhereKey(whereConditions)
        return SQL()
            .apply {
                DELETE_FROM(getTableName())
                getWhereSql(this, whereConditions)
                requiredWhereConditions(this)
            }.toString()
    }

    private fun requiredWhereConditions(sql: SQL) {
        if (!sql.toString().contains("WHERE ", ignoreCase = true)) {
            throw RuntimeException("whereConditions are required")
        }
    }

    private fun getWhereString(
        conditionType: String,
        dbColumnName: String,
        value: Any?,
    ): String =
        when (conditionType) {
            "ne",
            "not",
            -> "`$dbColumnName` <> ${getFormattedValue(value)}"
            "in" -> {
                require(value is Set<*>) { "conditionType 'in' requires Set, yours: ${value?.javaClass}" }
                require(value.isNotEmpty()) { "WHERE - empty in cause: $dbColumnName" }
                "`$dbColumnName` IN (${value.joinToString(", ") { getFormattedValue(it) }})"
            }
            "notIn" -> {
                require(value is Set<*>) {
                    "conditionType 'notIn' requires Set, yours: ${value?.javaClass}"
                }
                require(value.isNotEmpty()) { "WHERE - empty in cause: $dbColumnName" }
                "`$dbColumnName` NOT IN (${value.joinToString(", ") { getFormattedValue(it) }})"
            }
            "null" -> "`$dbColumnName` IS NULL"
            "notNull" -> "`$dbColumnName` IS NOT NULL"
            "contains" -> "INSTR(`$dbColumnName`, ${getFormattedValue(value)}) > 0"
            "notContains" -> "INSTR(`$dbColumnName`, ${getFormattedValue(value)}) = 0"
            "startsWith" -> "INSTR(`$dbColumnName`, ${getFormattedValue(value)}) = 1"
            "endsWith" ->
                "RIGHT(`$dbColumnName`, CHAR_LENGTH(${getFormattedValue(value)})) = ${getFormattedValue(value)}"
            "lt" -> "`$dbColumnName` < ${getFormattedValue(value)}"
            "lte" -> "`$dbColumnName` <= ${getFormattedValue(value)}"
            "gt" -> "`$dbColumnName` > ${getFormattedValue(value)}"
            "gte" -> "`$dbColumnName` >= ${getFormattedValue(value)}"
            else -> getEqualSql(dbColumnName, value)
        }

    private fun getEqualSql(
        dbColumnName: String,
        value: Any?,
    ): String = "`$dbColumnName` = ${getFormattedValue(value)}"

    private fun getFormattedValue(value: Any?): String =
        when (value) {
            null -> "null"
            is String ->
                when {
                    isISO8601String(value) -> "'${converterInstantToString(Instant.parse(value))}'"
                    else -> "'${value.replace("'", "''")}'"
                }
            is Instant -> "'${converterInstantToString(value)}'"
            is Enum<*> ->
                when (value) {
                    is ValueEnum -> "'${value.getValue()}'"
                    else -> "'${value.name}'"
                }
            is List<*> -> "'[${value.joinToString(", ") { getFormattedValue(it).replace("'", "\"") }}]'"
            is Set<*> -> "'[${value.joinToString(", ") { getFormattedValue(it) }}]'"
            is Map<*, *> ->
                "\"{${value.entries.joinToString(", ") {
                    "\"${it.key}\":${getFormattedValue(it.value)}"
                }}}\""
            else -> value.toString().replace("'", "''")
        }

    private fun getWhereSql(
        sql: SQL,
        whereConditions: Map<String, Any>,
    ) {
        whereConditions.forEach { (key, value) ->
            val columnName = key.substringBefore(":")
            val conditionType = key.substringAfter(":", "eq")
            sql.WHERE(getWhereString(conditionType, columnName.camelCaseToSnakeCase(), value))
        }
    }

    private fun wrapIdentifier(identifier: String): String = "`$identifier`"

    private fun isISO8601String(value: String): Boolean =
        value.count { it == '-' } == 2 &&
            value.count { it == ':' } == 2 &&
            value.count { it == 'T' } == 1 &&
            (value.endsWith('Z') || value.count { it == '+' } == 1)

    private fun converterInstantToString(instant: Instant): String =
        OffsetDateTime
            .ofInstant(instant, ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
}
