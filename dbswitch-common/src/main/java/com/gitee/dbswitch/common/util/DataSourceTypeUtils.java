package com.gitee.dbswitch.common.util;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DataSourceTypeUtils {

  public static boolean isGreenplum(Connection connection) {

    try (PreparedStatement statement = connection.prepareStatement("SELECT version()")) {
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String version = resultSet.getString(1);
          if (Objects.nonNull(version) && version.contains("Greenplum")) {
            log.info("#### Target database is Greenplum Cluster");
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

}
