package com.github.bestheroz.standard.common.mybatis.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class GenericListTypeHandler<T> extends BaseTypeHandler<List<T>> {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final Class<T> type;

  public GenericListTypeHandler(Class<T> type) {
    if (type == null) throw new IllegalArgumentException("Type argument cannot be null");
    this.type = type;
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, List<T> parameter, JdbcType jdbcType)
      throws SQLException {
    try {
      String json = parameter != null ? objectMapper.writeValueAsString(parameter) : null;
      ps.setString(i, json);
    } catch (JsonProcessingException e) {
      throw new SQLException("Error converting List to JSON", e);
    }
  }

  @Override
  public List<T> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return parseJson(rs.getString(columnName));
  }

  @Override
  public List<T> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return parseJson(rs.getString(columnIndex));
  }

  @Override
  public List<T> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return parseJson(cs.getString(columnIndex));
  }

  private List<T> parseJson(String json) throws SQLException {
    if (json == null) {
      return null;
    }
    try {
      return objectMapper.readValue(
          json, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    } catch (JsonProcessingException e) {
      throw new SQLException("Error parsing JSON to List", e);
    }
  }
}
