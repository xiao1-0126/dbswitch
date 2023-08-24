package com.gitee.dbswitch.product.mongodb;

import cn.hutool.json.JSONUtil;
import com.gitee.dbswitch.common.consts.Constants;
import com.gitee.dbswitch.common.entity.ResultSetWrapper;
import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.provider.ProductFactoryProvider;
import com.gitee.dbswitch.provider.query.TableDataQueryProvider;
import com.gitee.dbswitch.schema.SchemaTableData;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;

public class MongodbTableDataQueryProvider implements TableDataQueryProvider {

  private ProductFactoryProvider factoryProvider;
  private DataSource dataSource;

  public MongodbTableDataQueryProvider(ProductFactoryProvider factoryProvider) {
    this.factoryProvider = factoryProvider;
    this.dataSource = factoryProvider.getDataSource();
  }

  @Override
  public ProductTypeEnum getProductType() {
    return this.factoryProvider.getProductType();
  }

  @Override
  public int getQueryFetchSize() {
    return 0;
  }

  @Override
  public void setQueryFetchSize(int size) {
  }

  @Override
  public ResultSetWrapper queryTableData(String schemaName, String tableName, List<String> fields,
      List<String> orders) {
    String sql = String.format("%s.%s.find().sort({ %s })",
        schemaName, tableName, orders.stream().map(s -> String.format("'%s' : 1", s))
            .collect(Collectors.joining(",")));
    try {
      Connection connection = this.dataSource.getConnection();
      Statement statement = connection.createStatement();
      //statement.setQueryTimeout(Constants.DEFAULT_QUERY_TIMEOUT_SECONDS);
      return ResultSetWrapper.builder()
          .connection(connection)
          .statement(statement)
          .resultSet(statement.executeQuery(sql))
          .build();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public SchemaTableData queryTableData(Connection connection, String schemaName, String tableName, int rowCount) {
    String querySQL = String.format("%s.%s.find({});", schemaName, tableName);
    SchemaTableData data = new SchemaTableData();
    data.setSchemaName(schemaName);
    data.setTableName(tableName);
    data.setColumns(new ArrayList<>());
    data.setRows(new ArrayList<>());
    try (Statement st = connection.createStatement()) {
      try (ResultSet rs = st.executeQuery(querySQL)) {
        ResultSetMetaData m = rs.getMetaData();
        int count = m.getColumnCount();
        for (int i = 1; i <= count; i++) {
          data.getColumns().add(m.getColumnLabel(i));
        }

        int counter = 0;
        while (rs.next() && counter++ < rowCount) {
          List<Object> row = new ArrayList<>(count);
          for (int i = 1; i <= count; i++) {
            Object value = rs.getObject(i);
            if (value instanceof Map || value instanceof List) {
              row.add(JSONUtil.toJsonStr(value));
            } else {
              row.add(null == value ? null : value.toString());
            }
          }
          data.getRows().add(row);
        }

        return data;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
