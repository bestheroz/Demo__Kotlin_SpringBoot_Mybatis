package com.github.bestheroz.standard.common.mybatis.handler;

import com.github.bestheroz.standard.common.enums.ValueEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GenericEnumTypeHandler<E extends ValueEnum> extends BaseTypeHandler<E> {

  private final Class<E> type;

  public GenericEnumTypeHandler(Class<E> type) {
    if (type == null) throw new IllegalArgumentException("Type argument cannot be null");
    this.type = type;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
      throws SQLException {
    if (parameter == null) {
      ps.setNull(i, jdbcType.TYPE_CODE);
    } else {
      ps.setString(i, parameter.value);
    }
  }

  @Override
  public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String value = rs.getString(columnName);
    return getEnum(value);
  }

  @Override
  public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String value = rs.getString(columnIndex);
    return getEnum(value);
  }

  @Override
  public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String value = cs.getString(columnIndex);
    return getEnum(value);
  }

  private E getEnum(String value) throws SQLException {
    if (value == null) return null;
    try {
      for (E enumConstant : type.getEnumConstants()) {
        if (enumConstant.value.equals(value)) {
          return enumConstant;
        }
      }
      throw new IllegalArgumentException("Unknown enum value: " + value);
    } catch (Exception e) {
      throw new SQLException(
          "Cannot convert " + value + " to " + type.getSimpleName() + " by name.", e);
    }
  }
}
