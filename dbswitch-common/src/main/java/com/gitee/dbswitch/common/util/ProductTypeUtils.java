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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ProductTypeUtils {

  public static boolean isGreenplum(Connection connection) {
    try (PreparedStatement statement = connection.prepareStatement("SELECT version()")) {
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String version = resultSet.getString(1);
          if (Objects.nonNull(version) && version.contains("Greenplum")) {
            if (log.isDebugEnabled()) {
              log.debug("#### Target database is Greenplum Cluster");
            }
            return true;
          }
        }
      }
    } catch (SQLException sqlException) {
      if (log.isDebugEnabled()) {
        log.debug("#### Failed to execute sql :select version(), and guesses it is not Greenplum datasource!");
      }
    }
    return false;
  }

  public static boolean isStarRocks(Connection connection) {
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


}
