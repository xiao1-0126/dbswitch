package com.gitee.dbswitch.admin.service;

import com.gitee.dbswitch.admin.common.response.PageResult;
import com.gitee.dbswitch.admin.common.response.Result;
import com.gitee.dbswitch.admin.entity.DatabaseConnectionEntity;
import com.gitee.dbswitch.admin.model.response.MetadataColumnDetailResponse;
import com.gitee.dbswitch.admin.model.response.MetadataSchemaDetailResponse;
import com.gitee.dbswitch.admin.model.response.MetadataTableDetailResponse;
import com.gitee.dbswitch.admin.model.response.MetadataTableInfoResponse;
import com.gitee.dbswitch.admin.model.response.SchemaTableDataResponse;
import com.gitee.dbswitch.admin.util.PageUtils;
import com.gitee.dbswitch.schema.SchemaTableData;
import com.gitee.dbswitch.schema.SchemaTableMeta;
import com.gitee.dbswitch.schema.TableDescription;
import com.gitee.dbswitch.service.MetadataService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@Service
public class MetaDataService {

  @Resource
  private ConnectionService connectionService;

  public PageResult<MetadataSchemaDetailResponse> allSchemas(Long id, Integer page, Integer size) {
    DatabaseConnectionEntity dbConn = connectionService.getDatabaseConnectionById(id);
    MetadataService metaDataService = connectionService.getMetaDataCoreService(dbConn);
    try {
      List<String> schemas = metaDataService.querySchemaList();
      List<MetadataSchemaDetailResponse> responses = schemas.stream()
          .map(s -> new MetadataSchemaDetailResponse(dbConn.getName(), s))
          .collect(Collectors.toList());
      return PageUtils.getPage(responses, page, size);
    } finally {
      metaDataService.close();
    }
  }

  public PageResult<MetadataTableInfoResponse> allTables(Long id, String schema, Integer page,
      Integer size) {
    DatabaseConnectionEntity dbConn = connectionService.getDatabaseConnectionById(id);
    MetadataService metaDataService = connectionService.getMetaDataCoreService(dbConn);
    try {
      List<TableDescription> tables = metaDataService.queryTableList(schema);
      List<MetadataTableInfoResponse> responses = tables.stream()
          .map(one -> MetadataTableInfoResponse.builder()
              .tableName(one.getTableName())
              .schemaName(one.getSchemaName())
              .remarks(one.getRemarks())
              .type(one.getTableType())
              .build()
          ).collect(Collectors.toList());
      return PageUtils.getPage(responses, page, size);
    } finally {
      metaDataService.close();
    }
  }

  public Result<MetadataTableDetailResponse> tableDetail(Long id, String schema, String table) {
    DatabaseConnectionEntity dbConn = connectionService.getDatabaseConnectionById(id);
    MetadataService metaDataService = connectionService.getMetaDataCoreService(dbConn);
    try {
      SchemaTableMeta tableMeta = metaDataService.queryTableMeta(schema, table);
      List<String> pks = tableMeta.getPrimaryKeys();
      List<MetadataColumnDetailResponse> columnDetailResponses = tableMeta.getColumns().stream()
          .map(one -> MetadataColumnDetailResponse.builder()
              .fieldName(one.getFieldName())
              .typeName(one.getFieldTypeName())
              .typeClassName(one.getFiledTypeClassName())
              .fieldType(String.valueOf(one.getFieldType()))
              .displaySize(String.valueOf(one.getDisplaySize()))
              .precisionSize(String.valueOf(one.getPrecisionSize()))
              .scaleSize(String.valueOf(one.getScaleSize()))
              .isPrimaryKey(
                  toStr(
                      CollectionUtils.isNotEmpty(pks)
                          && pks.contains(one.getFieldName())))
              .isAutoIncrement(toStr(one.isAutoIncrement()))
              .isNullable(toStr(one.isNullable()))
              .remarks(one.getRemarks())
              .build()
          ).collect(Collectors.toList());
      return Result.success(MetadataTableDetailResponse.builder()
          .tableName(tableMeta.getTableName())
          .schemaName(tableMeta.getSchemaName())
          .remarks(tableMeta.getRemarks())
          .type(tableMeta.getTableType())
          .createSql(tableMeta.getCreateSql())
          .primaryKeys(tableMeta.getPrimaryKeys())
          .columns(columnDetailResponses)
          .indexes(tableMeta.getIndexes())
          .build());
    } finally {
      metaDataService.close();
    }
  }

  public Result<SchemaTableDataResponse> tableData(Long id, String schema, String table) {
    DatabaseConnectionEntity dbConn = connectionService.getDatabaseConnectionById(id);
    MetadataService metaDataService = connectionService.getMetaDataCoreService(dbConn);
    try {
      SchemaTableData data = metaDataService.queryTableData(schema, table, 10);
      // el-table问题：https://www.cnblogs.com/LanTianYou/p/9649735.html
      List<String> headers = data.getColumns().stream()
          .map(one -> one.replaceAll("\\.", "_"))
          .collect(Collectors.toList());
      return Result.success(SchemaTableDataResponse.builder()
          .schemaName(data.getSchemaName())
          .tableName(data.getTableName())
          .columns(headers)
          .rows(convertRows(headers, data.getRows()))
          .build()
      );
    } finally {
      metaDataService.close();
    }
  }

  private List<Map<String, Object>> convertRows(List<String> columns, List<List<Object>> rows) {
    if (null == rows || rows.isEmpty()) {
      return Collections.emptyList();
    }
    List<Map<String, Object>> result = new ArrayList<>(rows.size());
    for (List<Object> row : rows) {
      Map<String, Object> map = new LinkedHashMap<>();
      for (int i = 0; i < columns.size(); ++i) {
        Object v = row.get(i);
        map.put(columns.get(i), Objects.nonNull(v) ? v.toString() : null);
      }
      result.add(map);
    }
    return result;
  }

  private String toStr(Boolean value) {
    if (null == value) {
      return "未知";
    }
    if (value) {
      return "是";
    }

    return "否";
  }

}
