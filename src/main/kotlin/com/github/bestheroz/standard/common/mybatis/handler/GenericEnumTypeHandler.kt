package com.github.bestheroz.standard.common.mybatis.handler

import com.github.bestheroz.standard.common.enums.ValueEnum
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
        parameter: E,
        jdbcType: JdbcType?,
    ) {
        if (parameter == null) {
            // jdbcType?.TYPE_CODE가 null이면 기본값 0 지정
            ps.setNull(i, jdbcType?.TYPE_CODE ?: 0)
        } else {
            ps.setString(i, parameter.getValue())
        }
    }

    override fun getNullableResult(
        rs: ResultSet,
        columnName: String?,
    ): E? {
        val value = rs.getString(columnName)
        return getEnum(value)
    }

    override fun getNullableResult(
        rs: ResultSet,
        columnIndex: Int,
    ): E? {
        val value = rs.getString(columnIndex)
        return getEnum(value)
    }

    override fun getNullableResult(
        cs: CallableStatement,
        columnIndex: Int,
    ): E? {
        val value = cs.getString(columnIndex)
        return getEnum(value)
    }

    @Throws(SQLException::class)
    private fun getEnum(value: String?): E? {
        if (value == null) {
            return null
        }
        return try {
            for (enumConstant in type!!.enumConstants) {
                if (enumConstant.getValue() == value) {
                    return enumConstant
                }
            }
            throw IllegalArgumentException("Unknown enum value: $value")
        } catch (e: Exception) {
            throw SQLException("Cannot convert $value to ${type?.simpleName} by name.", e)
        }
    }
}
