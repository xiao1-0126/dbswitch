// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.oceanbase;

import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.product.mysql.MysqlMetadataQueryProvider;
import com.gitee.dbswitch.provider.ProductFactoryProvider;
import com.gitee.dbswitch.provider.meta.AbstractMetadataProvider;
import com.gitee.dbswitch.provider.meta.MetadataProvider;
import com.gitee.dbswitch.schema.ColumnDescription;
import com.gitee.dbswitch.schema.ColumnMetaData;
import com.gitee.dbswitch.schema.IndexDescription;
import com.gitee.dbswitch.schema.SourceProperties;
import com.gitee.dbswitch.schema.TableDescription;
import java.sql.Connection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class OceanbaseMetadataQueryProvider extends AbstractMetadataProvider {

  private final MetadataProvider delegate;

  public OceanbaseMetadataQueryProvider(ProductFactoryProvider factoryProvider, MetadataProvider delegate) {
    super(factoryProvider);
    this.delegate = delegate;
  }

  @Override
  public List<String> querySchemaList(Connection connection) {
    return this.delegate.querySchemaList(connection);
  }

  @Override
  public List<TableDescription> queryTableList(Connection connection, String schemaName) {
    return this.delegate.queryTableList(connection, schemaName);
  }

  @Override
  public TableDescription queryTableMeta(Connection connection, String schemaName, String tableName) {
    return this.delegate.queryTableMeta(connection, schemaName, tableName);
  }

  @Override
  public String getTableDDL(Connection connection, String schemaName, String tableName) {
    return this.delegate.getTableDDL(connection, schemaName, tableName);
  }

  @Override
  public String getViewDDL(Connection connection, String schemaName, String tableName) {
    return this.delegate.getViewDDL(connection, schemaName, tableName);
  }

  @Override
  public List<String> queryTableColumnName(Connection connection, String schemaName,
      String tableName) {
    return this.delegate.queryTableColumnName(connection, schemaName, tableName);
  }

  @Override
  public List<ColumnDescription> queryTableColumnMeta(Connection connection, String schemaName,
      String tableName) {
    return this.delegate.queryTableColumnMeta(connection, schemaName, tableName);
  }

  @Override
  public List<ColumnDescription> querySelectSqlColumnMeta(Connection connection, String sql) {
    return this.delegate.querySelectSqlColumnMeta(connection, sql);
  }

  @Override
  public List<String> queryTablePrimaryKeys(Connection connection, String schemaName, String tableName) {
    return this.delegate.queryTablePrimaryKeys(connection, schemaName, tableName);
  }

  @Override
  public List<IndexDescription> queryTableIndexes(Connection connection, String schemaName, String tableName) {
    return this.delegate.queryTableIndexes(connection, schemaName, tableName);
  }

  @Override
  public void testQuerySQL(Connection connection, String sql) {
    if (StringUtils.equals(ProductTypeEnum.OCEANBASE.getSql(), sql)) {
      if (this.delegate instanceof MysqlMetadataQueryProvider) {
        this.delegate.testQuerySQL(connection, ProductTypeEnum.MYSQL.getSql());
      } else {
        this.delegate.testQuerySQL(connection, ProductTypeEnum.ORACLE.getSql());
      }
    } else {
      this.delegate.testQuerySQL(connection, sql);
    }
  }

  @Override
  public String getQuotedSchemaTableCombination(String schemaName, String tableName) {
    return this.delegate.getQuotedSchemaTableCombination(schemaName, tableName);
  }

  @Override
  public String getFieldDefinition(ColumnMetaData v, List<String> pks, boolean useAutoInc, boolean addCr,
      boolean withRemarks) {
    return this.delegate.getFieldDefinition(v, pks, useAutoInc, addCr, withRemarks);
  }

  @Override
  public void preAppendCreateTableSql(StringBuilder builder) {
    this.delegate.preAppendCreateTableSql(builder);
  }

  @Override
  public void appendPrimaryKeyForCreateTableSql(StringBuilder builder, List<String> primaryKeys) {
    this.delegate.appendPrimaryKeyForCreateTableSql(builder, primaryKeys);
  }

  @Override
  public void postAppendCreateTableSql(StringBuilder builder, String tblComment, List<String> primaryKeys,
      SourceProperties tblProperties) {
    this.delegate.postAppendCreateTableSql(builder, tblComment, primaryKeys, tblProperties);
  }

  @Override
  public String getPrimaryKeyAsString(List<String> pks) {
    return this.delegate.getPrimaryKeyAsString(pks);
  }

  @Override
  public List<String> getTableColumnCommentDefinition(TableDescription td, List<ColumnDescription> cds) {
    return this.delegate.getTableColumnCommentDefinition(td, cds);
  }

  @Override
  public List<String> getCreateTableSqlList(List<ColumnDescription> fieldNames, List<String> primaryKeys,
      String schemaName, String tableName, String tableRemarks, boolean autoIncr, SourceProperties tblProperties) {
    return this.delegate
        .getCreateTableSqlList(fieldNames, primaryKeys, schemaName, tableName, tableRemarks, autoIncr, tblProperties);
  }
}
