// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.admin.service;

import cn.hutool.core.io.FileUtil;
import com.gitee.dbswitch.admin.common.exception.DbswitchException;
import com.gitee.dbswitch.admin.common.response.PageResult;
import com.gitee.dbswitch.admin.common.response.Result;
import com.gitee.dbswitch.admin.common.response.ResultCode;
import com.gitee.dbswitch.admin.controller.converter.DbConnectionDetailConverter;
import com.gitee.dbswitch.admin.dao.DatabaseConnectionDAO;
import com.gitee.dbswitch.admin.entity.DatabaseConnectionEntity;
import com.gitee.dbswitch.admin.model.request.DbConnectionCreateRequest;
import com.gitee.dbswitch.admin.model.request.DbConnectionSearchRequest;
import com.gitee.dbswitch.admin.model.request.DbConnectionUpdateRequest;
import com.gitee.dbswitch.admin.model.response.DatabaseTypeDetailResponse;
import com.gitee.dbswitch.admin.model.response.DatabaseTypeDriverResponse;
import com.gitee.dbswitch.admin.model.response.DbConnectionDetailResponse;
import com.gitee.dbswitch.admin.model.response.DbConnectionNameResponse;
import com.gitee.dbswitch.admin.util.PageUtils;
import com.gitee.dbswitch.common.converter.ConverterFactory;
import com.gitee.dbswitch.common.entity.CloseableDataSource;
import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.common.util.JdbcUrlUtils;
import com.gitee.dbswitch.data.util.DataSourceUtils;
import com.gitee.dbswitch.service.DefaultMetadataService;
import com.gitee.dbswitch.service.MetadataService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class ConnectionService {

  @Resource
  private DriverLoadService driverLoadService;
  @Resource
  private DatabaseConnectionDAO databaseConnectionDAO;

  public MetadataService getMetaDataCoreService(DatabaseConnectionEntity dbConn) {
    String typeName = dbConn.getType().getName().toUpperCase();
    ProductTypeEnum supportDbType = ProductTypeEnum.valueOf(typeName);
    if (supportDbType.hasAddress()) {
      for (String pattern : supportDbType.getUrl()) {
        final Matcher matcher = JdbcUrlUtils.getPattern(pattern).matcher(dbConn.getUrl());
        if (!matcher.matches()) {
          if (1 == supportDbType.getUrl().length) {
            throw new DbswitchException(ResultCode.ERROR_CANNOT_CONNECT_REMOTE, dbConn.getName());
          } else {
            continue;
          }
        }

        String host = matcher.group("host");
        String port = matcher.group("port");
        if (StringUtils.isBlank(port)) {
          port = String.valueOf(supportDbType.getPort());
        }

        if (!JdbcUrlUtils.reachable(host, port)) {
          throw new DbswitchException(ResultCode.ERROR_CANNOT_CONNECT_REMOTE, dbConn.getName());
        }
      }
    }
    File driverVersionFile = driverLoadService.getVersionDriverFile(dbConn.getType(), dbConn.getVersion());
    String driverPath = driverVersionFile.getAbsolutePath();
    CloseableDataSource dataSource = DataSourceUtils.createCommonDataSource(dbConn.getUrl(), dbConn.getDriver(),
        driverPath, dbConn.getUsername(), dbConn.getPassword());
    MetadataService metaDataService = new DefaultMetadataService(dataSource);
    return metaDataService;
  }

  public List<DatabaseTypeDetailResponse> getTypes() {
    List<DatabaseTypeDetailResponse> lists = new ArrayList<>();
    for (ProductTypeEnum type : ProductTypeEnum.values()) {
      DatabaseTypeDetailResponse detail = new DatabaseTypeDetailResponse();
      detail.setId(type.getId());
      detail.setType(type.getName().toUpperCase());
      detail.setDriver(type.getDriver());
      detail.setSample(type.getSample());

      lists.add(detail);
    }

    return lists;
  }

  public List<DatabaseTypeDriverResponse> getDrivers(ProductTypeEnum dbTypeEnum) {
    List<DatabaseTypeDriverResponse> lists = new ArrayList<>();
    driverLoadService.getDriverVersionWithPath(dbTypeEnum)
        .forEach(
            (k, v) ->
                lists.add(
                    DatabaseTypeDriverResponse.builder()
                        .driverVersion(k)
                        .driverClass(dbTypeEnum.getDriver())
                        .driverPath(v.getAbsolutePath())
                        .jarFiles(
                            FileUtil.listFileNames(v.getAbsolutePath())
                        )
                        .build()
                )
        );
    return lists;
  }

  public PageResult<DbConnectionDetailResponse> getConnections(
      DbConnectionSearchRequest request) {
    Supplier<List<DbConnectionDetailResponse>> method = () -> {
      List<DatabaseConnectionEntity> databaseConnectionEntities = databaseConnectionDAO
          .listAll(request.getSearchText());
      return ConverterFactory.getConverter(DbConnectionDetailConverter.class)
          .convert(databaseConnectionEntities);
    };

    return PageUtils.getPage(method, request.getPage(), request.getSize());
  }

  public DbConnectionDetailResponse getDetailById(Long id) {
    return ConverterFactory.getConverter(DbConnectionDetailConverter.class)
        .convert(getDatabaseConnectionById(id));
  }

  public Result test(Long id) {
    DatabaseConnectionEntity dbConn = getDatabaseConnectionById(id);
    MetadataService metaDataService = getMetaDataCoreService(dbConn);
    try {
      metaDataService.testQuerySQL(dbConn.getType().getSql());
    } finally {
      metaDataService.close();
    }
    return Result.success();
  }

  public Result<List<String>> getSchemas(Long id) {
    DatabaseConnectionEntity dbConn = getDatabaseConnectionById(id);
    MetadataService metaDataService = getMetaDataCoreService(dbConn);
    try {
      List<String> schemas = metaDataService.querySchemaList();
      return Result.success(schemas);
    } finally {
      metaDataService.close();
    }
  }

  public Result<List<String>> getSchemaTables(Long id, String schema) {
    DatabaseConnectionEntity dbConn = getDatabaseConnectionById(id);
    MetadataService metaDataService = getMetaDataCoreService(dbConn);
    try {
      List<String> tables = Optional.ofNullable(
          metaDataService.queryTableList(schema))
          .orElseGet(ArrayList::new).stream()
          .filter(t -> !t.isViewTable())
          .map(t -> t.getTableName())
          .collect(Collectors.toList());
      return Result.success(tables);
    } finally {
      metaDataService.close();
    }
  }

  public Result<List<String>> getSchemaViews(Long id, String schema) {
    DatabaseConnectionEntity dbConn = getDatabaseConnectionById(id);
    MetadataService metaDataService = getMetaDataCoreService(dbConn);
    try {
      List<String> tables = Optional.ofNullable(
          metaDataService.queryTableList(schema))
          .orElseGet(ArrayList::new).stream()
          .filter(t -> t.isViewTable())
          .map(t -> t.getTableName())
          .collect(Collectors.toList());
      return Result.success(tables);
    } finally {
      metaDataService.close();
    }
  }

  public Result<DbConnectionDetailResponse> addDatabaseConnection(
      DbConnectionCreateRequest request) {
    if (StringUtils.isBlank(request.getName())) {
      return Result.failed(ResultCode.ERROR_INVALID_ARGUMENT, "name is empty");
    }

    if (Objects.nonNull(databaseConnectionDAO.getByName(request.getName()))) {
      return Result.failed(ResultCode.ERROR_RESOURCE_ALREADY_EXISTS, "name=" + request.getName());
    }

    DatabaseConnectionEntity conn = request.toDatabaseConnection();
    validJdbcUrlFormat(conn);
    databaseConnectionDAO.insert(conn);

    return Result.success(ConverterFactory.getConverter(DbConnectionDetailConverter.class)
        .convert(databaseConnectionDAO.getById(conn.getId())));
  }

  public Result<DbConnectionDetailResponse> updateDatabaseConnection(
      DbConnectionUpdateRequest request) {
    if (Objects.isNull(request.getId()) || Objects
        .isNull(databaseConnectionDAO.getById(request.getId()))) {
      return Result.failed(ResultCode.ERROR_RESOURCE_NOT_EXISTS, "id=" + request.getId());
    }

    DatabaseConnectionEntity exist = databaseConnectionDAO.getByName(request.getName());
    if (Objects.nonNull(exist) && !exist.getId().equals(request.getId())) {
      return Result.failed(ResultCode.ERROR_RESOURCE_ALREADY_EXISTS, "name=" + request.getName());
    }

    DatabaseConnectionEntity conn = request.toDatabaseConnection();
    validJdbcUrlFormat(conn);
    databaseConnectionDAO.updateById(conn);

    return Result.success(ConverterFactory.getConverter(DbConnectionDetailConverter.class)
        .convert(databaseConnectionDAO.getById(conn.getId())));
  }

  public void deleteDatabaseConnection(Long id) {
    databaseConnectionDAO.deleteById(id);
  }

  public PageResult<DbConnectionNameResponse> getNameList(Integer page, Integer size) {
    Supplier<List<DbConnectionNameResponse>> method = () -> {
      List<DatabaseConnectionEntity> lists = databaseConnectionDAO.listAll(null);
      return lists.stream()
          .map(c -> new DbConnectionNameResponse(c.getId(), c.getName()))
          .collect(Collectors.toList());
    };

    return PageUtils.getPage(method, page, size);
  }

  public DatabaseConnectionEntity getDatabaseConnectionById(Long id) {
    DatabaseConnectionEntity dbConn = databaseConnectionDAO.getById(id);
    if (Objects.isNull(dbConn)) {
      throw new DbswitchException(ResultCode.ERROR_RESOURCE_NOT_EXISTS, "id=" + id);
    }

    return dbConn;
  }

  private void validJdbcUrlFormat(DatabaseConnectionEntity conn) {
    String typeName = conn.getType().getName().toUpperCase();
    ProductTypeEnum supportDbType = ProductTypeEnum.valueOf(typeName);
    if (!conn.getUrl().startsWith(supportDbType.getUrlPrefix())) {
      throw new DbswitchException(ResultCode.ERROR_INVALID_JDBC_URL, conn.getUrl());
    }

    for (int i = 0; i < supportDbType.getUrl().length; ++i) {
      String pattern = supportDbType.getUrl()[i];
      Matcher matcher = JdbcUrlUtils.getPattern(pattern).matcher(conn.getUrl());
      if (!matcher.matches()) {
        if (i == supportDbType.getUrl().length - 1) {
          throw new DbswitchException(ResultCode.ERROR_INVALID_JDBC_URL, conn.getUrl());
        }
      } else {
        if (supportDbType.hasDatabaseName() && StringUtils.isBlank(matcher.group("database"))) {
          throw new DbswitchException(ResultCode.ERROR_INVALID_JDBC_URL,
              "库名没有指定 :" + conn.getUrl());
        }
        if (supportDbType.hasFilePath() && StringUtils.isBlank(matcher.group("file"))) {
          throw new DbswitchException(ResultCode.ERROR_INVALID_JDBC_URL,
              "文件路径没有指定 :" + conn.getUrl());
        }

        break;
      }
    }
  }

}
