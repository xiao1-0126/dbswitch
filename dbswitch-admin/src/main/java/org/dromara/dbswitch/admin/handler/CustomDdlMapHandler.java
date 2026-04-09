// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2026/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.admin.handler;

import org.dromara.dbswitch.data.util.JsonUtils;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class CustomDdlMapHandler extends BaseTypeHandler<Map<String, String>> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Map<String, String> map,
      JdbcType jdbcType) throws SQLException {
    ps.setString(i, map2string(map));
  }

  @Override
  public Map<String, String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String r = rs.getString(columnName);
    if (rs.wasNull()) {
      return null;
    }
    return string2map(r);
  }

  @Override
  public Map<String, String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String r = rs.getString(columnIndex);
    if (rs.wasNull()) {
      return null;
    }
    return string2map(r);
  }

  @Override
  public Map<String, String> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    String r = cs.getString(columnIndex);
    if (cs.wasNull()) {
      return null;
    }
    return string2map(r);
  }

  private String map2string(Map<String, String> map) {
    if (map == null || map.isEmpty()) {
      return null;
    }
    return JsonUtils.toJsonString(map);
  }

  private Map<String, String> string2map(String str) {
    if (str == null || str.isEmpty()) {
      return null;
    }
    return JsonUtils.toMap(str);
  }

}
