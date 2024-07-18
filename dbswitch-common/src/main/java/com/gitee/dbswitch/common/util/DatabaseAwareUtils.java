// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.common.util;

import com.gitee.dbswitch.common.type.ProductTypeEnum;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据库类型识别工具类
 *
 * @author tang
 */
@Slf4j
@UtilityClass
public final class DatabaseAwareUtils {

  private static final Map<String, ProductTypeEnum> productNameMap;

  private static final Map<String, ProductTypeEnum> driverNameMap;

  static {
    productNameMap = new HashMap<>();
    driverNameMap = new HashMap<>();

    productNameMap.put("Microsoft SQL Server", ProductTypeEnum.SQLSERVER);
    productNameMap.put("DM DBMS", ProductTypeEnum.DM);
    productNameMap.put("KingbaseES", ProductTypeEnum.KINGBASE);
    productNameMap.put("Apache Hive", ProductTypeEnum.HIVE);
    productNameMap.put("MySQL", ProductTypeEnum.MYSQL);
    productNameMap.put("MariaDB", ProductTypeEnum.MARIADB);
    productNameMap.put("Oracle", ProductTypeEnum.ORACLE);
    productNameMap.put("PostgreSQL", ProductTypeEnum.POSTGRESQL);
    productNameMap.put("Greenplum", ProductTypeEnum.GREENPLUM);
    productNameMap.put("Highgo", ProductTypeEnum.HIGHGO);
    productNameMap.put("DB2 for Unix/Windows", ProductTypeEnum.DB2);
    productNameMap.put("Hive", ProductTypeEnum.HIVE);
    productNameMap.put("SQLite", ProductTypeEnum.SQLITE3);
    productNameMap.put("OSCAR", ProductTypeEnum.OSCAR);
    productNameMap.put("GBase", ProductTypeEnum.GBASE8A);
    productNameMap.put("Adaptive Server Enterprise", ProductTypeEnum.SYBASE);
    productNameMap.put("ClickHouse", ProductTypeEnum.CLICKHOUSE);
    productNameMap.put("Mongo DB", ProductTypeEnum.MONGODB);

    driverNameMap.put("MySQL Connector Java", ProductTypeEnum.MYSQL);
    driverNameMap.put("MariaDB Connector/J", ProductTypeEnum.MARIADB);
    driverNameMap.put("Oracle JDBC driver", ProductTypeEnum.ORACLE);
    driverNameMap.put("PostgreSQL JDBC Driver", ProductTypeEnum.POSTGRESQL);
    driverNameMap.put("Kingbase8 JDBC Driver", ProductTypeEnum.KINGBASE);
    driverNameMap.put("IBM Data Server Driver for JDBC and SQLJ", ProductTypeEnum.DB2);
    driverNameMap.put("dm.jdbc.driver.DmDriver", ProductTypeEnum.DM);
    driverNameMap.put("Hive JDBC", ProductTypeEnum.HIVE);
    driverNameMap.put("SQLite JDBC", ProductTypeEnum.SQLITE3);
    driverNameMap.put("OSCAR JDBC DRIVER", ProductTypeEnum.OSCAR);
    driverNameMap.put("GBase JDBC Driver", ProductTypeEnum.GBASE8A);
    driverNameMap.put("jConnect (TM) for JDBC (TM)", ProductTypeEnum.SYBASE);
    driverNameMap.put("ClickHouse JDBC Driver", ProductTypeEnum.CLICKHOUSE);
    driverNameMap.put("MongoDB JDBC Driver", ProductTypeEnum.MONGODB);
    driverNameMap.put("esJestDriver", ProductTypeEnum.ELASTICSEARCH);
  }

  /**
   * 获取数据库的产品枚举
   *
   * @param dataSource 数据源
   * @return 数据库产品枚举
   */
  public static ProductTypeEnum getProductTypeByDataSource(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      String productName = connection.getMetaData().getDatabaseProductName();
      String driverName = connection.getMetaData().getDriverName();
      if (driverNameMap.containsKey(driverName)) {
        ProductTypeEnum productType = driverNameMap.get(driverName);
        if (productType == ProductTypeEnum.POSTGRESQL) {
          // String url = connection.getMetaData().getURL();
          // Set<ProductTypeEnum> excludes = Sets.immutableEnumSet(ProductTypeEnum.POSTGRESQL);
          // ProductTypeEnum pgLikeType = ProductTypeEnum.getProductType(url, excludes);
          // if (null != pgLikeType) {
          //   return pgLikeType;
          // }
          if (DataSourceTypeUtils.isGreenplum(connection)) {
            return ProductTypeEnum.GREENPLUM;
          }
        } else if (productType == ProductTypeEnum.MYSQL) {
          if (isStarRocks(connection)) {
            return ProductTypeEnum.STARROCKS;
          }
        }
        return productType;
      }

      ProductTypeEnum type = productNameMap.get(productName);
      if (null != type) {
        return type;
      }
      String url = connection.getMetaData().getURL();
      if (null != url && url.contains("mongodb://")) {
        return ProductTypeEnum.MONGODB;
      }
      if (null != url && url.contains("jest://")) {
        return ProductTypeEnum.ELASTICSEARCH;
      }
      type = ProductTypeEnum.getProductType(url);
      if (null != type) {
        return type;
      }
      throw new IllegalStateException("Unable to detect database type from data source instance");
    } catch (SQLException se) {
      throw new RuntimeException(se);
    }
  }

  private static boolean isStarRocks(Connection connection) {
    try (Statement statement = connection.createStatement()) {
      // 此查询语句是Starrocks查询be节点是否存活，可以用来判断是否是Starrocks数据源
      String sql = "SHOW BACKENDS";
      return statement.execute(sql);
    } catch (Exception sqlException) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to execute sql :show backends, and guesses it is mysql datasource!");
      }
    }
    return false;
  }

  /**
   * 检查MySQL数据库表的存储引擎是否为Innodb
   *
   * @param schemaName schema名
   * @param tableName  table名
   * @param dataSource 数据源
   * @return 为Innodb存储引擎时返回True, 否在为false
   */
  public static boolean isMysqlInnodbStorageEngine(String schemaName, String tableName,
      DataSource dataSource) {
    String sql = "SELECT count(*) as total FROM information_schema.tables "
        + "WHERE table_schema=? AND table_name=? AND ENGINE='InnoDB'";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, schemaName);
      ps.setString(2, tableName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }

      return false;
    } catch (SQLException se) {
      throw new RuntimeException(se);
    }
  }

}
