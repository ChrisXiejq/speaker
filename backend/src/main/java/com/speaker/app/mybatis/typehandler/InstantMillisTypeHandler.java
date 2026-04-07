package com.speaker.app.mybatis.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

@MappedTypes(Instant.class)
@MappedJdbcTypes(JdbcType.BIGINT)
public class InstantMillisTypeHandler extends BaseTypeHandler<Instant> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Instant parameter, JdbcType jdbcType) throws SQLException {
        ps.setLong(i, parameter.toEpochMilli());
    }

    @Override
    public Instant getNullableResult(ResultSet rs, String columnName) throws SQLException {
        long v = rs.getLong(columnName);
        return rs.wasNull() ? null : Instant.ofEpochMilli(v);
    }

    @Override
    public Instant getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        long v = rs.getLong(columnIndex);
        return rs.wasNull() ? null : Instant.ofEpochMilli(v);
    }

    @Override
    public Instant getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        long v = cs.getLong(columnIndex);
        return cs.wasNull() ? null : Instant.ofEpochMilli(v);
    }
}
