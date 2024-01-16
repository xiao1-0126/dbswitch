// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.admin.model.request;

import com.gitee.dbswitch.admin.entity.AssignmentConfigEntity;
import com.gitee.dbswitch.admin.type.IncludeExcludeEnum;
import com.gitee.dbswitch.common.entity.PatternMapper;
import com.gitee.dbswitch.common.type.CaseConvertEnum;
import com.gitee.dbswitch.common.type.ProductTableEnum;
import com.gitee.dbswitch.common.type.SyncOptionEnum;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AssigmentBaseRequest {

  @NoArgsConstructor
  @Data
  public static class AssigmentConfig {

    private Long sourceConnectionId;
    private String sourceSchema;
    private ProductTableEnum tableType;
    private IncludeExcludeEnum includeOrExclude;
    private List<String> sourceTables;
    private Long targetConnectionId;
    private String targetSchema;
    private CaseConvertEnum tableNameCase;
    private CaseConvertEnum columnNameCase;
    private List<PatternMapper> tableNameMapper;
    private List<PatternMapper> columnNameMapper;
    private Boolean targetDropTable;
    private Boolean targetOnlyCreate;
    private Boolean targetAutoIncrement;
    private SyncOptionEnum targetSyncOption;
    private String beforeSqlScripts;
    private String afterSqlScripts;
    private Integer batchSize;
    private Integer channelSize;
  }

  protected AssignmentConfigEntity toAssignmentConfig(final Long assignmentId, final AssigmentConfig config) {
    AssignmentConfigEntity assignmentConfigEntity = new AssignmentConfigEntity();
    assignmentConfigEntity.setAssignmentId(assignmentId);
    assignmentConfigEntity.setSourceConnectionId(config.getSourceConnectionId());
    assignmentConfigEntity.setSourceSchema(config.getSourceSchema());
    assignmentConfigEntity.setTableType(config.getTableType());
    assignmentConfigEntity.setSourceTables(config.getSourceTables());
    assignmentConfigEntity.setExcluded(
        config.getIncludeOrExclude() == IncludeExcludeEnum.EXCLUDE
    );
    assignmentConfigEntity.setTargetConnectionId(config.getTargetConnectionId());
    assignmentConfigEntity.setTargetSchema(config.getTargetSchema());
    assignmentConfigEntity.setTableNameCase(config.getTableNameCase());
    assignmentConfigEntity.setColumnNameCase(config.getColumnNameCase());
    assignmentConfigEntity.setTableNameMap(config.getTableNameMapper());
    assignmentConfigEntity.setColumnNameMap(config.getColumnNameMapper());
    assignmentConfigEntity.setTargetDropTable(config.getTargetDropTable());
    assignmentConfigEntity.setTargetOnlyCreate(config.getTargetOnlyCreate());
    assignmentConfigEntity.setTargetAutoIncrement(config.getTargetAutoIncrement());
    assignmentConfigEntity.setBeforeSqlScripts(getTrimValueOrNull(config.getBeforeSqlScripts()));
    assignmentConfigEntity.setAfterSqlScripts(getTrimValueOrNull(config.getAfterSqlScripts()));
    assignmentConfigEntity.setTargetSyncOption(config.getTargetSyncOption());
    assignmentConfigEntity.setBatchSize(getValueOrDefault(config.getBatchSize(), 10000));
    assignmentConfigEntity.setChannelSize(getValueOrDefault(config.getChannelSize(), 100));
    assignmentConfigEntity.setFirstFlag(Boolean.FALSE);

    return assignmentConfigEntity;
  }

  protected int getValueOrDefault(Integer value, int defaultValue) {
    return Objects.nonNull(value) ? value : defaultValue;
  }

  protected String getTrimValueOrNull(String value) {
    return Objects.nonNull(value) ? value.trim() : null;
  }
}
