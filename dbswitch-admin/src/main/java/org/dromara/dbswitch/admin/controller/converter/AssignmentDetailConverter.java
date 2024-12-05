// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.admin.controller.converter;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.dbswitch.admin.dao.AssignmentConfigDAO;
import org.dromara.dbswitch.admin.dao.DatabaseConnectionDAO;
import org.dromara.dbswitch.admin.model.response.AssignmentDetailResponse;
import org.dromara.dbswitch.admin.type.IncludeExcludeEnum;
import org.dromara.dbswitch.admin.entity.AssignmentConfigEntity;
import org.dromara.dbswitch.admin.entity.AssignmentTaskEntity;
import org.dromara.dbswitch.admin.entity.DatabaseConnectionEntity;
import org.dromara.dbswitch.common.converter.AbstractConverter;

public class AssignmentDetailConverter extends
    AbstractConverter<AssignmentTaskEntity, AssignmentDetailResponse> {

  @Override
  public AssignmentDetailResponse convert(AssignmentTaskEntity assignmentTaskEntity) {
    DatabaseConnectionDAO databaseConnectionDAO = SpringUtil.getBean(DatabaseConnectionDAO.class);
    AssignmentConfigDAO assignmentConfigDAO = SpringUtil.getBean(AssignmentConfigDAO.class);
    AssignmentConfigEntity taskConfig = assignmentConfigDAO.getByAssignmentTaskId(
        assignmentTaskEntity.getId());
    DatabaseConnectionEntity srcConn = databaseConnectionDAO
        .getById(taskConfig.getSourceConnectionId());
    DatabaseConnectionEntity dstConn = databaseConnectionDAO
        .getById(taskConfig.getTargetConnectionId());

    AssignmentDetailResponse.Configuration config = new AssignmentDetailResponse.Configuration();
    config.setSourceConnectionId(srcConn.getId());
    config.setSourceConnectionName(srcConn.getName());
    config.setSourceSchema(taskConfig.getSourceSchema());
    config.setTableType(taskConfig.getTableType());
    config.setIncludeOrExclude(taskConfig.getExcludedFlag()
        ? IncludeExcludeEnum.EXCLUDE
        : IncludeExcludeEnum.INCLUDE);
    config.setSourceTables(taskConfig.getSourceTables());
    config.setTargetConnectionId(dstConn.getId());
    config.setTargetConnectionName(dstConn.getName());
    config.setTargetSchema(taskConfig.getTargetSchema());
    config.setTargetOnlyCreate(taskConfig.getTargetOnlyCreate());
    config.setTargetDropTable(taskConfig.getTargetDropTable());
    config.setTargetAutoIncrement(taskConfig.getTargetAutoIncrement());
    config.setTableNameCase(taskConfig.getTableNameCase());
    config.setColumnNameCase(taskConfig.getColumnNameCase());
    config.setTableNameMapper(taskConfig.getTableNameMap());
    config.setColumnNameMapper(taskConfig.getColumnNameMap());
    config.setBatchSize(taskConfig.getBatchSize());
    config.setChannelSize(taskConfig.getChannelSize());
    config.setTargetSyncOption(taskConfig.getTargetSyncOption());
    config.setBeforeSqlScripts(taskConfig.getBeforeSqlScripts());
    config.setAfterSqlScripts(taskConfig.getAfterSqlScripts());

    AssignmentDetailResponse detailResponse = new AssignmentDetailResponse();
    detailResponse.setId(assignmentTaskEntity.getId());
    detailResponse.setName(assignmentTaskEntity.getName());
    detailResponse.setDescription(assignmentTaskEntity.getDescription());
    detailResponse.setScheduleMode(assignmentTaskEntity.getScheduleMode());
    detailResponse.setCronExpression(assignmentTaskEntity.getCronExpression());
    detailResponse.setCreateTime(assignmentTaskEntity.getCreateTime());
    detailResponse.setUpdateTime(assignmentTaskEntity.getUpdateTime());
    detailResponse.setConfiguration(config);

    return detailResponse;
  }

}
