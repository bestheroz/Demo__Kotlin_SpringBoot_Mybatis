package com.github.bestheroz.standard.common.mybatis.handler

import io.github.bestheroz.mybatis.type.ValueEnum
import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class GenericEnumTypeHandler<E : ValueEnum>(
    private val type: Class<E>?,
) : BaseTypeHandler<E>() {
    init {
        requireNotNull(type) { "Type argument cannot be null" }
    }

    override fun setNonNullParameter(
        ps: PreparedStatement,
        i: Int,
        parameter: E?,
        jdbcType: JdbcType?,
    ) {
        if (parameter == null) {
            // jdbcType?.TYPE_CODE가 null이면 기본값 0 지정
            ps.setNull(i, jdbcType?.TYPE_CODE ?: 0)
        } else {
            ps.setString(i, parameter.value)
        }
    }

    override fun getNullableResult(
        rs: ResultSet,
        columnName: String?,
    ): E? = getEnum(rs.getString(columnName))

    override fun getNullableResult(
        rs: ResultSet,
        columnIndex: Int,
    ): E? = getEnum(rs.getString(columnIndex))

    override fun getNullableResult(
        cs: CallableStatement,
        columnIndex: Int,
    ): E? = getEnum(cs.getString(columnIndex))

    @Throws(SQLException::class)
    private fun getEnum(value: String?): E? {
        if (value == null) {
            return null
        }
        return try {
            for (enumConstant in checkNotNull(type).enumConstants) {
                if (enumConstant.value == value) {
                    return enumConstant
                }
            }
            throw IllegalArgumentException("Unknown enum value: $value")
        } catch (e: Exception) {
            throw SQLException("Cannot convert $value to ${type?.simpleName} by name.", e)
        }
    }
}
