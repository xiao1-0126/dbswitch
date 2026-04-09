// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2026/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.admin.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dbswitch.admin.common.response.Result;
import org.dromara.dbswitch.admin.common.response.ResultCode;
import org.dromara.dbswitch.admin.dao.DatabaseConnectionDAO;
import org.dromara.dbswitch.admin.entity.DatabaseConnectionEntity;
import org.dromara.dbswitch.admin.model.request.DdlSinglePreviewRequest;
import org.dromara.dbswitch.admin.model.response.DdlPreviewResponse;
import org.dromara.dbswitch.common.entity.CloseableDataSource;
import org.dromara.dbswitch.common.entity.PatternMapper;
import org.dromara.dbswitch.common.type.CaseConvertEnum;
import org.dromara.dbswitch.common.type.ProductTypeEnum;
import org.dromara.dbswitch.common.util.PatterNameUtils;
import org.dromara.dbswitch.core.provider.ProductFactoryProvider;
import org.dromara.dbswitch.core.provider.ProductProviderFactory;
import org.dromara.dbswitch.core.provider.meta.MetadataProvider;
import org.dromara.dbswitch.core.schema.ColumnDescription;
import org.dromara.dbswitch.core.schema.SourceProperties;
import org.dromara.dbswitch.core.service.DefaultMetadataService;
import org.dromara.dbswitch.core.service.MetadataService;
import org.springframework.stereotype.Service;

@Service
public class DdlPreviewService {

  @Resource
  private ConnectionService connectionService;

  @Resource
  private DatabaseConnectionDAO databaseConnectionDAO;

  /**
   * 按需预览单张表的DDL（懒加载，用于弹窗中逐表加载）
   */
  public Result<DdlPreviewResponse> previewSingleDdl(DdlSinglePreviewRequest request) {
    DatabaseConnectionEntity sourceConn = databaseConnectionDAO.getById(request.getSourceConnectionId());
    DatabaseConnectionEntity targetConn = databaseConnectionDAO.getById(request.getTargetConnectionId());

    MetadataService sourceMetaDataService = null;
    try {
      sourceMetaDataService = connectionService.getMetaDataCoreService(sourceConn);

      CloseableDataSource targetDataSource = connectionService.getDataSource(targetConn);
      ProductTypeEnum targetType = targetConn.getType();

      ProductFactoryProvider targetFactoryProvider = ProductProviderFactory.newProvider(targetType, targetDataSource);
      MetadataProvider targetMetaProvider = targetFactoryProvider.createMetadataQueryProvider();

      CaseConvertEnum tableNameCase = request.getTableNameCase() != null ? request.getTableNameCase() : CaseConvertEnum.NONE;
      CaseConvertEnum columnNameCase = request.getColumnNameCase() != null ? request.getColumnNameCase() : CaseConvertEnum.NONE;
      boolean autoIncrement = request.getTargetAutoIncrement() != null && request.getTargetAutoIncrement();
      List<PatternMapper> tableNameMappers = request.getTableNameMapper() != null ? request.getTableNameMapper() : new ArrayList<>();
      List<PatternMapper> columnNameMappers = request.getColumnNameMapper() != null ? request.getColumnNameMapper() : new ArrayList<>();

      String sourceTableName = request.getSourceTable();
      String targetTableName = tableNameCase.convert(
          PatterNameUtils.getFinalName(sourceTableName, tableNameMappers));

      if (StringUtils.isEmpty(targetTableName)) {
        return Result.failed(ResultCode.ERROR_INVALID_ARGUMENT, "目标表名为空，请检查表名映射规则");
      }

      try {
        List<ColumnDescription> columns = sourceMetaDataService.queryTableColumnMeta(
            request.getSourceSchema(), sourceTableName);

        List<ColumnDescription> targetColumns = columns.stream().map(column -> {
          String newName = columnNameCase.convert(
              PatterNameUtils.getFinalName(column.getFieldName(), columnNameMappers));
          ColumnDescription desc = column.copy();
          desc.setFieldName(newName);
          desc.setLabelName(newName);
          return desc;
        }).collect(Collectors.toList());

        targetColumns = targetColumns.stream()
            .filter(c -> StringUtils.isNotBlank(c.getFieldName()))
            .collect(Collectors.toList());

        List<String> primaryKeys = sourceMetaDataService.queryTablePrimaryKeys(
            request.getSourceSchema(), sourceTableName);
        primaryKeys = primaryKeys.stream()
            .map(name -> columnNameCase.convert(
                PatterNameUtils.getFinalName(name, columnNameMappers)))
            .collect(Collectors.toList());

        String tableRemarks = sourceMetaDataService.getTableRemark(
            request.getSourceSchema(), sourceTableName);

        SourceProperties tblProps = new SourceProperties();
        tblProps.setProductType(sourceConn.getType());
        tblProps.setDriverClass(sourceConn.getDriver());
        tblProps.setJdbcUrl(sourceConn.getUrl());
        tblProps.setUsername(sourceConn.getUsername());
        tblProps.setPassword(sourceConn.getPassword());
        tblProps.setSchemaName(request.getSourceSchema());
        tblProps.setTableName(sourceTableName);
        tblProps.setColumnNames(targetColumns.stream()
            .map(ColumnDescription::getFieldName).collect(Collectors.toList()));

        List<String> ddlList = sourceMetaDataService.getDDLCreateTableSQL(
            targetMetaProvider, targetColumns, primaryKeys,
            request.getTargetSchema(), targetTableName, tableRemarks, autoIncrement, tblProps);

        String ddl = String.join(";\n", ddlList);

        return Result.success(DdlPreviewResponse.builder()
            .sourceTableName(sourceTableName)
            .targetTableName(targetTableName)
            .ddlSql(ddl)
            .build());
      } catch (Exception e) {
        return Result.success(DdlPreviewResponse.builder()
            .sourceTableName(sourceTableName)
            .targetTableName(targetTableName)
            .ddlSql("-- 生成失败: " + e.getMessage())
            .build());
      }
    } finally {
      if (sourceMetaDataService != null) {
        sourceMetaDataService.close();
      }
    }
  }
}
