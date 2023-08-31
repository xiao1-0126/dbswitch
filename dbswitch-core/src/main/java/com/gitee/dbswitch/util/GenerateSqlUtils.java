// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.util;

import com.gitee.dbswitch.common.consts.Constants;
import com.gitee.dbswitch.common.type.ProductTableEnum;
import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.common.util.DDLFormatterUtils;
import com.gitee.dbswitch.common.util.UuidUtils;
import com.gitee.dbswitch.provider.meta.MetadataProvider;
import com.gitee.dbswitch.schema.ColumnDescription;
import com.gitee.dbswitch.schema.ColumnMetaData;
import com.gitee.dbswitch.schema.TableDescription;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 拼接SQL工具类
 *
 * @author tang
 */
@UtilityClass
public final class GenerateSqlUtils {

  private static final boolean HIVE_USE_CTAS = false;

  public static String getDDLCreateTableSQL(
      MetadataProvider provider,
      List<ColumnDescription> fieldNames,
      List<String> primaryKeys,
      String schemaName,
      String tableName,
      boolean autoIncr) {
    return getDDLCreateTableSQL(
        provider,
        fieldNames,
        primaryKeys,
        schemaName,
        tableName,
        false,
        null,
        autoIncr,
        Collections.emptyMap());
  }

  public static String getDDLCreateTableSQL(
      MetadataProvider provider,
      List<ColumnDescription> fieldNames,
      List<String> primaryKeys,
      String schemaName,
      String tableName,
      boolean withRemarks,
      String tableRemarks,
      boolean autoIncr,
      Map<String, String> tblProperties) {
    ProductTypeEnum type = provider.getProductType();
    StringBuilder sb = new StringBuilder();
    List<String> pks = fieldNames.stream()
        .filter((cd) -> primaryKeys.contains(cd.getFieldName()))
        .map((cd) -> cd.getFieldName())
        .collect(Collectors.toList());

    sb.append(Constants.CREATE_TABLE);
    // if(ifNotExist && !type.isLikeOracle()) {
    // sb.append( Const.IF_NOT_EXISTS );
    // }
    sb.append(provider.getQuotedSchemaTableCombination(schemaName, tableName));
    sb.append("(");

    for (int i = 0; i < fieldNames.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      } else {
        sb.append("  ");
      }

      ColumnMetaData v = fieldNames.get(i).getMetaData();
      sb.append(provider.getFieldDefinition(v, pks, autoIncr, false, withRemarks));
    }

    if (!pks.isEmpty() && !type.isLikeHive()) {
      String pk = provider.getPrimaryKeyAsString(pks);
      sb.append(", PRIMARY KEY (").append(pk).append(")");
    }

    sb.append(")");
    if (type.isLikeMysql()) {
      sb.append("ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin");
      if (withRemarks && StringUtils.isNotBlank(tableRemarks)) {
        sb.append(String.format(" COMMENT='%s' ", tableRemarks.replace("'", "\\'")));
      }
    } else if (type.isLikeHive()) {
      if (null != tblProperties && !tblProperties.isEmpty()) {
        List<String> kvProperties = new ArrayList<>();
        tblProperties.forEach((k, v) -> kvProperties.add(String.format("\t\t'%s' = '%s'", k, v)));
        sb.append(Constants.CR);
        sb.append("STORED BY 'org.apache.hive.storage.jdbc.JdbcStorageHandler'");
        sb.append(Constants.CR);
        sb.append("TBLPROPERTIES (");
        sb.append(kvProperties.stream().collect(Collectors.joining(",\n")));
        sb.append(")");
      } else {
        sb.append(Constants.CR);
        sb.append("STORED AS ORC");
      }
    }

    return DDLFormatterUtils.format(sb.toString());
  }

  public static List<String> getDDLCreateTableSQL(
      MetadataProvider provider,
      List<ColumnDescription> fieldNames,
      List<String> primaryKeys,
      String schemaName,
      String tableName,
      String tableRemarks,
      boolean autoIncr,
      Map<String, String> tblProperties) {
    ProductTypeEnum productType = provider.getProductType();
    if (productType.isLikeHive()) {
      List<String> sqlLists = new ArrayList<>();
      String tmpTableName = "tmp_" + UuidUtils.generateUuid();
      String createTableSql = getDDLCreateTableSQL(provider, fieldNames, primaryKeys, schemaName,
          tmpTableName, true, tableRemarks, autoIncr, tblProperties);
      sqlLists.add(createTableSql);
      if (HIVE_USE_CTAS) {
        String createAsTableSql = String.format("CREATE TABLE `%s`.`%s` STORED AS ORC AS (SELECT * FROM `%s`.`%s`)",
            schemaName, tableName, schemaName, tmpTableName);
        sqlLists.add(createAsTableSql);
      } else {
        String createAsTableSql = getDDLCreateTableSQL(provider, fieldNames, primaryKeys, schemaName,
            tableName, true, tableRemarks, autoIncr, null);
        sqlLists.add(createAsTableSql);
        String selectColumns = fieldNames.stream()
            .map(s -> String.format("`%s`", s.getFieldName()))
            .collect(Collectors.joining(","));
        String insertIntoSql = String.format("INSERT INTO `%s`.`%s` SELECT %s FROM `%s`.`%s`",
            schemaName, tableName, selectColumns, schemaName, tmpTableName);
        sqlLists.add(insertIntoSql);
      }
      String dropTmpTableSql = String.format("DROP TABLE IF EXISTS `%s`.`%s`", schemaName, tmpTableName);
      sqlLists.add(dropTmpTableSql);
      return sqlLists;
    } else if (productType.noCommentStatement()) {
      String createTableSql = getDDLCreateTableSQL(provider, fieldNames, primaryKeys, schemaName,
          tableName, true, tableRemarks, autoIncr, tblProperties);
      return Arrays.asList(createTableSql);
    } else {
      String createTableSql = getDDLCreateTableSQL(provider, fieldNames, primaryKeys, schemaName,
          tableName, true, tableRemarks, autoIncr, tblProperties);
      TableDescription td = new TableDescription();
      td.setSchemaName(schemaName);
      td.setTableName(tableName);
      td.setRemarks(tableRemarks);
      td.setTableType(ProductTableEnum.TABLE.name());
      List<String> results = provider.getTableColumnCommentDefinition(td, fieldNames);
      if (CollectionUtils.isEmpty(results)) {
        results = Lists.newArrayList(createTableSql);
      } else {
        results.add(0, createTableSql);
      }
      return results;
    }
  }

}
