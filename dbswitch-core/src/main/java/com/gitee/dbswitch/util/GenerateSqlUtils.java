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
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;

/**
 * 拼接SQL工具类
 *
 * @author tang
 */
@UtilityClass
public final class GenerateSqlUtils {

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
    Set<String> fieldNameSets = fieldNames.stream()
        .map(ColumnDescription::getFieldName)
        .collect(Collectors.toSet());
    List<String> pks = primaryKeys.stream()
        .filter(fieldNameSets::contains)
        .collect(Collectors.toList());

    sb.append(Constants.CREATE_TABLE);
    provider.preAppendCreateTableSql(sb);
    sb.append(provider.getQuotedSchemaTableCombination(schemaName, tableName));
    sb.append("(");

    // StarRocks 当中，字段主键的情况下，必须将字段放在最前面，并且顺序一致。
    if (type.isPrimaryKeyShouldAtFirst()) {
      List<ColumnDescription> copyFieldNames = new ArrayList<>();
      Integer fieldIndex = 0;
      for (int i = 0; i < fieldNames.size(); i++) {
        ColumnDescription cd = fieldNames.get(i);
        if (primaryKeys.contains(cd.getFieldName())) {
          copyFieldNames.add(fieldIndex++, cd);
        } else {
          copyFieldNames.add(cd);
        }
      }
      fieldNames = copyFieldNames;
    }

    for (int i = 0; i < fieldNames.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      } else {
        sb.append("  ");
      }

      ColumnMetaData v = fieldNames.get(i).getMetaData();
      sb.append(provider.getFieldDefinition(v, pks, autoIncr, false, withRemarks));
    }

    provider.appendPrimaryKeyForCreateTableSql(sb, pks);
    sb.append(")");
    provider.postAppendCreateTableSql(sb, tableRemarks, pks, tblProperties);

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
      return provider.getCreateTableSqlList(
          fieldNames, primaryKeys, schemaName, tableName, tableRemarks, autoIncr, tblProperties);
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
