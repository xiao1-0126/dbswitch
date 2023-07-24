// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.common.type;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据库产品类型的枚举定义
 *
 * @author Tang
 */
@Getter
@AllArgsConstructor
public enum ProductTypeEnum {

  /**
   * MySQL数据库类型
   */
  MYSQL(1, "`", "mysql", "com.mysql.jdbc.Driver", 3306,
      "/* ping */ SELECT 1",
      "jdbc:mysql://",
      new String[]{"jdbc:mysql://{host}[:{port}]/[{database}][\\?{params}]"}),

  /**
   * MariaDB数据库类型
   */
  MARIADB(2, "`", "mariadb", "org.mariadb.jdbc.Driver", 3306,
      "SELECT 1",
      "jdbc:mariadb://",
      new String[]{"jdbc:mariadb://{host}[:{port}]/[{database}][\\?{params}]"}),

  /**
   * Oracle数据库类型
   */
  ORACLE(3, "\"", "oracle", "oracle.jdbc.driver.OracleDriver", 1521,
      "SELECT 'Hello' from DUAL",
      "jdbc:oracle:thin:@",
      new String[]{"jdbc:oracle:thin:@{host}:{port}:{database}",
          "jdbc:oracle:thin:@//{host}[:{port}]/{database}"}),

  /**
   * Microsoft SQL Server数据库类型(>=2005)
   */
  SQLSERVER(4, "\"", "sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433,
      "SELECT 1+2 as a",
      "jdbc:sqlserver://",
      new String[]{"jdbc:sqlserver://{host}[:{port}][;DatabaseName={database}][;{params}]"}),

  /**
   * PostgreSQL数据库类型
   */
  POSTGRESQL(5, "\"", "postgresql", "org.postgresql.Driver", 5432,
      "SELECT 1",
      "jdbc:postgresql://",
      new String[]{"jdbc:postgresql://{host}[:{port}]/[{database}][\\?{params}]"}),

  /**
   * DB2数据库类型
   */
  DB2(6, "\"", "db2", "com.ibm.db2.jcc.DB2Driver", 50000,
      "SELECT 1 FROM SYSIBM.SYSDUMMY1",
      "jdbc:db2://",
      new String[]{"jdbc:db2://{host}:{port}/{database}[:{params}]"}),

  /**
   * [国产] 达梦(DM)数据库类型
   */
  DM(7, "\"", "dm", "dm.jdbc.driver.DmDriver", 5236,
      "SELECT 'Hello' from DUAL",
      "jdbc:dm://",
      new String[]{"jdbc:dm://{host}:{port}[/{database}][\\?{params}]"}),

  /**
   * [国产] 金仓(Kingbase)数据库类型
   */
  KINGBASE(8, "\"", "kingbase", "com.kingbase8.Driver", 54321,
      "SELECT 1",
      "jdbc:kingbase8://",
      new String[]{"jdbc:kingbase8://{host}[:{port}]/[{database}][\\?{params}]"}),

  /**
   * [国产] 神通(Oscar)数据库类型
   */
  OSCAR(9, "\"", "oscar", "com.oscar.Driver", 2003,
      "SELECT 1",
      "jdbc:oscar://",
      new String[]{"jdbc:oscar://{host}[:{port}]/[{database}][\\?{params}]"}),

  /**
   * [国产] 南大通用(GBase8A)数据库类型
   */
  GBASE8A(10, "`", "gbase8a", "com.gbase.jdbc.Driver", 5258,
      "/* ping */ SELECT 1",
      "jdbc:gbase://",
      new String[]{"jdbc:gbase://{host}[:{port}]/[{database}][\\?{params}]"}),

  /**
   * Sybase 数据库类型
   */
  SYBASE(11, "\"", "sybase", "com.sybase.jdbc4.jdbc.SybDriver", 5000,
      "SELECT 1+2 as a",
      "jdbc:sybase:Tds:",
      new String[]{"jdbc:sybase:Tds:{host}[:{port}][/{database}][\\?{params}]"}),

  /**
   * Hive 数据库类型
   */
  HIVE(12, "`", "hive", "org.apache.hive.jdbc.HiveDriver", 10000,
      "SELECT 1",
      "jdbc:hive2://",
      new String[]{"jdbc:hive2://{host}[:{port}]/[{database}][\\?{params}]"}),

  /**
   * Sqlite v3数据库类型
   */
  // 参考文章：https://blog.csdn.net/wank1259162/article/details/104946744
  SQLITE3(13, "\"", "sqlite3", "org.sqlite.JDBC", 0,
      "SELECT 1",
      "jdbc:sqlite:",
      new String[]{"jdbc:sqlite:{file}", "jdbc:sqlite::resource:{file}"}),
  ;

  private int id;
  private String quote;
  private String name;
  private String driver;
  private int port;
  private String sql;
  private String urlPrefix;
  private String[] url;

  public boolean hasDatabaseName() {
    return !Arrays.asList(DM, SQLITE3).contains(this);
  }

  public boolean hasFilePath() {
    return this == SQLITE3;
  }

  public boolean hasAddress() {
    return this != SQLITE3;
  }

  public boolean noCommentStatement() {
    return Arrays.asList(
        ProductTypeEnum.MYSQL,
        ProductTypeEnum.MARIADB,
        ProductTypeEnum.GBASE8A,
        ProductTypeEnum.HIVE,
        ProductTypeEnum.SQLITE3,
        ProductTypeEnum.SYBASE
    ).contains(this);
  }

  public String quoteName(String name) {
    return String.format("%s%s%s", quote, name, quote);
  }

  public String quoteSchemaTableName(String schema, String table) {
    return String.format("%s%s%s.%s%s%s", quote, schema, quote, quote, table, quote);
  }

  public static boolean exists(String name) {
    return Arrays.stream(values()).anyMatch(item -> item.name().equalsIgnoreCase(name));
  }

  public static ProductTypeEnum of(String name) {
    if (!StringUtils.isEmpty(name)) {
      for (ProductTypeEnum type : ProductTypeEnum.values()) {
        if (type.getName().equalsIgnoreCase(name)) {
          return type;
        }
      }
    }

    throw new IllegalArgumentException("cannot find enum name: " + name);
  }

  public static boolean isUnsupportedTargetSqlite(String url) {
    String prefix1 = "jdbc:sqlite::resource:";
    //String prefix2 = "jdbc:sqlite::memory:";
    return url.startsWith(prefix1);
  }

}
