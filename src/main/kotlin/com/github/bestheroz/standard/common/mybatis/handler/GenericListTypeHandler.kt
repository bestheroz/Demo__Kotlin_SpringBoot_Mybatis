package com.github.bestheroz.standard.common.mybatis.handler

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class GenericListTypeHandler<T>(
    private val type: Class<T>,
) : BaseTypeHandler<List<T>>() {
    init {
        requireNotNull(type) { "Type argument cannot be null" }
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.dateFormat = StdDateFormat().withColonInTimeZone(true)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

    override fun setNonNullParameter(
        ps: PreparedStatement,
        i: Int,
        parameter: List<T>,
        jdbcType: JdbcType?,
    ) {
        try {
            val json = objectMapper.writeValueAsString(parameter)
            ps.setString(i, json)
        } catch (e: JsonProcessingException) {
            throw SQLException("Error converting List to JSON", e)
        }
    }

    override fun getNullableResult(
        rs: ResultSet,
        columnName: String?,
    ): List<T>? = parseJson(rs.getString(columnName))

    override fun getNullableResult(
        rs: ResultSet,
        columnIndex: Int,
    ): List<T>? = parseJson(rs.getString(columnIndex))

    override fun getNullableResult(
        cs: CallableStatement,
        columnIndex: Int,
    ): List<T>? = parseJson(cs.getString(columnIndex))

    private fun parseJson(json: String?): List<T>? {
        if (json.isNullOrEmpty()) {
            return null
        }
        return try {
            objectMapper.readValue(
                json,
                objectMapper.typeFactory.constructCollectionType(List::class.java, type),
            )
        } catch (e: JsonProcessingException) {
            throw SQLException("Error parsing JSON to List", e)
        }
    }

    companion object {
        private val objectMapper = ObjectMapper()
    }
}
