// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.clickhouse;

import com.gitee.dbswitch.common.consts.Constants;
import com.gitee.dbswitch.provider.ProductFactoryProvider;
import com.gitee.dbswitch.provider.meta.AbstractMetadataProvider;
import com.gitee.dbswitch.schema.ColumnDescription;
import com.gitee.dbswitch.schema.ColumnMetaData;
import com.gitee.dbswitch.schema.TableDescription;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ClickhouseMetadataQueryProvider extends AbstractMetadataProvider {

  private static final String SHOW_CREATE_TABLE_SQL = "SHOW CREATE TABLE `%s`.`%s` ";
  private static final String SHOW_CREATE_VIEW_SQL = "SHOW CREATE VIEW `%s`.`%s` ";
  private static final String QUERY_SCHEMA_LIST_SQL =
      "SELECT name from `system`.`databases` where engine !='Memory'";
  private static final String QUERY_TABLE_LIST_SQL =
      "SELECT database ,name, comment, engine from `system`.`tables` where is_temporary =0 and database = ? ";
  private static final String QUERY_TABLE_META_SQL =
      "SELECT database ,name, comment, engine from `system`.`tables` where is_temporary =0 and database = ? and name = ?";
  private static final String QUERY_PRIMARY_KEY_SQL =
      "SELECT name from `system`.`columns` where `database` = ? and `table` = ? and is_in_primary_key =1 order by `position` ";

  public ClickhouseMetadataQueryProvider(ProductFactoryProvider factoryProvider) {
    super(factoryProvider);
  }

  @Override
  public List<String> querySchemaList(Connection connection) {
    List<String> result = new ArrayList<>();
    try (Statement st = connection.createStatement()) {
      try (ResultSet rs = st.executeQuery(QUERY_SCHEMA_LIST_SQL)) {
        while (rs.next()) {
          result.add(rs.getString(1));
        }
        return result;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<TableDescription> queryTableList(Connection connection, String schemaName) {
    List<TableDescription> result = new ArrayList<>();
    try (PreparedStatement ps = connection.prepareStatement(QUERY_TABLE_LIST_SQL)) {
      ps.setString(1, schemaName);
      try (ResultSet rs = ps.executeQuery();) {
        while (rs.next()) {
          TableDescription td = new TableDescription();
          td.setSchemaName(rs.getString(1));
          td.setTableName(rs.getString(2));
          td.setRemarks(rs.getString(3));
          String tableType = rs.getString(4);
          if (tableType.equalsIgnoreCase("VIEW")) {
            td.setTableType("VIEW");
          } else {
            td.setTableType("TABLE");
          }

          result.add(td);
        }

        return result;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public TableDescription queryTableMeta(Connection connection, String schemaName, String tableName) {
    try (PreparedStatement ps = connection.prepareStatement(QUERY_TABLE_META_SQL)) {
      ps.setString(1, schemaName);
      ps.setString(2, tableName);
      try (ResultSet rs = ps.executeQuery();) {
        if (rs.next()) {
          TableDescription td = new TableDescription();
          td.setSchemaName(rs.getString(1));
          td.setTableName(rs.getString(2));
          td.setRemarks(rs.getString(3));
          String tableType = rs.getString(4);
          if (tableType.equalsIgnoreCase("VIEW")) {
            td.setTableType("VIEW");
          } else {
            td.setTableType("TABLE");
          }

          return td;
        }

        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<String> queryTablePrimaryKeys(Connection connection, String schemaName, String tableName) {
    List<String> result = new ArrayList<>();
    try (PreparedStatement ps = connection.prepareStatement(QUERY_PRIMARY_KEY_SQL)) {
      ps.setString(1, schemaName);
      ps.setString(2, tableName);
      try (ResultSet rs = ps.executeQuery();) {
        while (rs.next()) {
          result.add(rs.getString(1));
        }
        return result;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getTableDDL(Connection connection, String schemaName, String tableName) {
    List<String> result = new ArrayList<>();
    try (Statement st = connection.createStatement()) {
      if (st.execute(String.format(SHOW_CREATE_TABLE_SQL, schemaName, tableName))) {
        try (ResultSet rs = st.getResultSet()) {
          if (rs != null) {
            while (rs.next()) {
              String value = rs.getString(1);
              Optional.ofNullable(value).ifPresent(result::add);
            }
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return result.stream().findAny().orElse(null);
  }

  @Override
  public String getViewDDL(Connection connection, String schemaName, String tableName) {
    List<String> result = new ArrayList<>();
    try (Statement st = connection.createStatement()) {
      if (st.execute(String.format(SHOW_CREATE_VIEW_SQL, schemaName, tableName))) {
        try (ResultSet rs = st.getResultSet()) {
          if (rs != null) {
            while (rs.next()) {
              String value = rs.getString(1);
              Optional.ofNullable(value).ifPresent(result::add);
            }
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return result.stream().findAny().orElse(null);
  }

  @Override
  public List<ColumnDescription> querySelectSqlColumnMeta(Connection connection, String sql) {
    String querySQL = String.format(" %s LIMIT 0,1", sql.replace(";", ""));
    return this.getSelectSqlColumnMeta(connection, querySQL);
  }

  @Override
  public void testQuerySQL(Connection connection, String sql) {
    String testQuerySql = String.format("explain %s", sql.replace(";", ""));
    if (log.isDebugEnabled()) {
      log.debug("Execute sql :{}", testQuerySql);
    }
    try (Statement st = connection.createStatement()) {
      st.execute(testQuerySql);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getFieldDefinition(ColumnMetaData v, List<String> pks, boolean useAutoInc,
      boolean addCr, boolean withRemarks) {
    String fieldname = v.getName();
    int length = v.getLength();
    int precision = v.getPrecision();
    int type = v.getType();
    boolean isPk = (null != pks && pks.contains(fieldname));
    String retval = " `" + fieldname + "`  ";

    switch (type) {
      case ColumnMetaData.TYPE_TIMESTAMP:
      case ColumnMetaData.TYPE_TIME:
        if (isPk) {
          retval += "DateTime64";
        } else {
          retval += "Nullable(DateTime64)";
        }
        break;
      case ColumnMetaData.TYPE_DATE:
        if (isPk) {
          retval += "Date";
        } else {
          retval += "Nullable(Date)";
        }
        break;
      case ColumnMetaData.TYPE_BOOLEAN:
        retval += "Nullable(Bool)";
        break;
      case ColumnMetaData.TYPE_NUMBER:
      case ColumnMetaData.TYPE_INTEGER:
      case ColumnMetaData.TYPE_BIGNUMBER:
        // Integer values...
        if (precision == 0) {
          if (isPk) {
            retval += "UInt64";
          } else {
            retval += "Nullable(UInt64)";
          }
        } else {
          // Floating point values...
          if (isPk) {
            retval += "Float64";
          } else {
            retval += "Nullable(Float64)";
          }
        }
        break;
      case ColumnMetaData.TYPE_STRING:
        if (isPk) {
          retval += "String";
        } else {
          if (length > 0) {
            if (length == 1) {
              retval += "Nullable(FixedString(1))";
            } else if (length < 4096) {
              retval += "Nullable(FixedString(" + length + "))";
            } else {
              retval += "Nullable(String)";
            }
          } else {
            retval += "Nullable(String)";
          }
        }

        break;
      case ColumnMetaData.TYPE_BINARY:
      default:
        if (isPk) {
          retval += "String";
        } else {
          retval += "Nullable(String)";
        }
        break;
    }

    if (withRemarks && StringUtils.isNotBlank(v.getRemarks())) {
      retval += String.format(" COMMENT '%s' ", v.getRemarks().replace("'", "\\'"));
    }

    if (addCr) {
      retval += Constants.CR;
    }

    return retval;
  }

  @Override
  public List<String> getTableColumnCommentDefinition(TableDescription td, List<ColumnDescription> cds) {
    return Collections.emptyList();
  }

}
