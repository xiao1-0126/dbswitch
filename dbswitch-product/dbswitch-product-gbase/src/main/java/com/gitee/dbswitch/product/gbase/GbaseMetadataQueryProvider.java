package com.gitee.dbswitch.product.gbase;

import com.gitee.dbswitch.common.util.JdbcUrlUtils;
import com.gitee.dbswitch.product.mysql.MysqlMetadataQueryProvider;
import com.gitee.dbswitch.provider.ProductFactoryProvider;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

public class GbaseMetadataQueryProvider extends MysqlMetadataQueryProvider {

  public GbaseMetadataQueryProvider(ProductFactoryProvider factoryProvider) {
    super(factoryProvider);
  }

  @Override
  public List<String> querySchemaList(Connection connection) {
    try {
      final Matcher matcher = JdbcUrlUtils
          .getPattern("jdbc:gbase:://{host}[:{port}]/[{database}][\\?{params}]")
          .matcher(connection.getMetaData().getURL());
      if (matcher.matches()) {
        return Collections.singletonList(matcher.group("database"));
      }
      throw new RuntimeException("get database name from jdbc url failed!");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    } catch (RuntimeException e) {
      throw e;
    }
  }
  
}
