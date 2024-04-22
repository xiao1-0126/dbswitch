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

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitee.dbswitch.admin.common.exception.DbswitchException;
import com.gitee.dbswitch.admin.common.response.PageResult;
import com.gitee.dbswitch.admin.common.response.Result;
import com.gitee.dbswitch.admin.common.response.ResultCode;
import com.gitee.dbswitch.admin.controller.converter.AssignmentDetailConverter;
import com.gitee.dbswitch.admin.controller.converter.AssignmentInfoConverter;
import com.gitee.dbswitch.admin.controller.converter.AssignmentsConverter;
import com.gitee.dbswitch.admin.dao.AssignmentConfigDAO;
import com.gitee.dbswitch.admin.dao.AssignmentTaskDAO;
import com.gitee.dbswitch.admin.dao.DatabaseConnectionDAO;
import com.gitee.dbswitch.admin.entity.AssignmentConfigEntity;
import com.gitee.dbswitch.admin.entity.AssignmentJobEntity;
import com.gitee.dbswitch.admin.entity.AssignmentTaskEntity;
import com.gitee.dbswitch.admin.entity.DatabaseConnectionEntity;
import com.gitee.dbswitch.admin.mapper.AssignmentJobMapper;
import com.gitee.dbswitch.admin.model.request.AssigmentCreateRequest;
import com.gitee.dbswitch.admin.model.request.AssigmentUpdateRequest;
import com.gitee.dbswitch.admin.model.request.AssignmentSearchRequest;
import com.gitee.dbswitch.admin.model.response.AssignmentDetailResponse;
import com.gitee.dbswitch.admin.model.response.AssignmentInfoResponse;
import com.gitee.dbswitch.admin.model.response.AssignmentsDataResponse;
import com.gitee.dbswitch.admin.type.JobStatusEnum;
import com.gitee.dbswitch.admin.type.ScheduleModeEnum;
import com.gitee.dbswitch.admin.util.PageUtils;
import com.gitee.dbswitch.common.converter.ConverterFactory;
import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.data.config.DbswichPropertiesConfiguration;
import com.gitee.dbswitch.data.entity.GlobalParamConfigProperties;
import com.gitee.dbswitch.data.entity.SourceDataSourceProperties;
import com.gitee.dbswitch.data.entity.TargetDataSourceProperties;
import com.gitee.dbswitch.data.util.JsonUtils;

@Service
public class AssignmentService {

	@Resource
	private AssignmentTaskDAO assignmentTaskDAO;

	@Resource
	private AssignmentConfigDAO assignmentConfigDAO;

	@Resource
	private ScheduleService scheduleService;

	@Resource
	private DatabaseConnectionDAO databaseConnectionDAO;

	@Resource
	private DriverLoadService driverLoadService;

	@Resource
	private AssignmentJobMapper assignmentJobMapper;

//	@Resource
//	private AssignmentConvert assignmentConvert;

	@Transactional(rollbackFor = Exception.class)
	public AssignmentInfoResponse createAssignment(AssigmentCreateRequest request) {
		AssignmentTaskEntity assignment = request.toAssignmentTask();
		assignmentTaskDAO.insert(assignment);

		AssignmentConfigEntity assignmentConfigEntity = request.toAssignmentConfig(assignment.getId());
		assignmentConfigDAO.insert(assignmentConfigEntity);

		Long targetConnectionId = assignmentConfigEntity.getTargetConnectionId();
		DatabaseConnectionEntity targetEntity = databaseConnectionDAO.getById(targetConnectionId);
		if (ProductTypeEnum.SQLITE3 == targetEntity.getType()) {
			if (ProductTypeEnum.isUnsupportedTargetSqlite(targetEntity.getUrl())) {
				throw new DbswitchException(ResultCode.ERROR_INVALID_ASSIGNMENT_CONFIG,
						"不支持目的端数据源为远程服务器上的SQLite或内存方式下的SQLite");
			}
		}

		Long sourceConnectionId = assignmentConfigEntity.getSourceConnectionId();
		DatabaseConnectionEntity sourceEntity = databaseConnectionDAO.getById(sourceConnectionId);
		if (ProductTypeEnum.ELASTICSEARCH == sourceEntity.getType()) {
			throw new DbswitchException(ResultCode.ERROR_INVALID_ASSIGNMENT_CONFIG,
					"不支持源端数据源为ElasticSearch类型");
		}

		return ConverterFactory.getConverter(AssignmentInfoConverter.class)
				.convert(assignmentTaskDAO.getById(assignment.getId()));
	}

	public void deleteAssignment(Long id) {
		AssignmentTaskEntity taskEntity = assignmentTaskDAO.getById(id);
		if (null != taskEntity && null != taskEntity.getPublished() && taskEntity.getPublished()) {
			throw new DbswitchException(ResultCode.ERROR_RESOURCE_HAS_DEPLOY,
					"已经发布的任务需先下线后方可执行删除操作");
		}
		assignmentTaskDAO.deleteById(id);
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateAssignment(AssigmentUpdateRequest request) {
		AssignmentTaskEntity assignmentTaskEntity = assignmentTaskDAO.getById(request.getId());
		if (Objects.isNull(assignmentTaskEntity)) {
			throw new DbswitchException(ResultCode.ERROR_RESOURCE_NOT_EXISTS, "ID=" + request.getId());
		} else if (assignmentTaskEntity.getPublished()) {
			throw new DbswitchException(ResultCode.ERROR_RESOURCE_HAS_DEPLOY, "ID=" + request.getId());
		}

		AssignmentTaskEntity newAssignmentTaskEntity = request.toAssignmentTask();
		assignmentTaskDAO.updateById(newAssignmentTaskEntity);

		AssignmentConfigEntity assignmentConfigEntity = request
				.toAssignmentConfig(assignmentTaskEntity.getId());
		assignmentConfigDAO.deleteByAssignmentTaskId(assignmentTaskEntity.getId());
		assignmentConfigDAO.insert(assignmentConfigEntity);

		Long targetConnectionId = assignmentConfigEntity.getTargetConnectionId();
		DatabaseConnectionEntity entity = databaseConnectionDAO.getById(targetConnectionId);
		if (ProductTypeEnum.SQLITE3 == entity.getType()) {
			if (ProductTypeEnum.isUnsupportedTargetSqlite(entity.getUrl())) {
				throw new DbswitchException(ResultCode.ERROR_INVALID_ASSIGNMENT_CONFIG,
						"不支持目的端数据源为远程服务器上的SQLite或内存方式下的SQLite");
			}
		}
	}

	public PageResult<AssignmentInfoResponse> listAll(AssignmentSearchRequest request) {
		Supplier<List<AssignmentInfoResponse>> method = () -> {
			List<AssignmentInfoResponse> assignmentInfoResponseList = ConverterFactory.getConverter(AssignmentInfoConverter.class)
					.convert(assignmentTaskDAO.listAll(request.getSearchText()));
			assignmentInfoResponseList.forEach((e) -> {
				AssignmentConfigEntity assignmentConfigEntity = this.assignmentConfigDAO.getByAssignmentTaskId(e.getId());

				Long sourceConnectionId = assignmentConfigEntity.getSourceConnectionId();
				DatabaseConnectionEntity databaseConnectionEntity = this.databaseConnectionDAO.getById(sourceConnectionId);
				String sourceSchema = assignmentConfigEntity.getSourceSchema();
				e.setSourceSchema(sourceSchema);
				String sourceType = databaseConnectionEntity.getType().getName();
				e.setSourceType(sourceType);

				Long targetConnectionId = assignmentConfigEntity.getTargetConnectionId();
				DatabaseConnectionEntity databaseConnectionEntity1 = this.databaseConnectionDAO.getById(targetConnectionId);
				String targetSchema = assignmentConfigEntity.getTargetSchema();
				e.setTargetSchema(targetSchema);
				String targetType = databaseConnectionEntity1.getType().getName();
				e.setTargetType(targetType);

				AssignmentJobEntity assignmentJobEntity = this.assignmentJobMapper.selectOne(
						new LambdaQueryWrapper<AssignmentJobEntity>()
								.eq(AssignmentJobEntity::getAssignmentId, e.getId()).orderByDesc(AssignmentJobEntity::getCreateTime)
								.last(" limit 1 "));
				Integer status = (assignmentJobEntity == null || assignmentJobEntity.getStatus() == null) ?
						JobStatusEnum.INIT.getValue() :
						assignmentJobEntity.getStatus();
				e.setRunStatus(JobStatusEnum.of(status).getName());

			});
			return assignmentInfoResponseList;
		};
		return PageUtils.getPage(method, request.getPage(), request.getSize());
	}

	public Result<AssignmentDetailResponse> detailAssignment(Long id) {
		AssignmentTaskEntity assignmentTaskEntity = assignmentTaskDAO.getById(id);
		if (Objects.isNull(assignmentTaskEntity)) {
			return Result.failed(ResultCode.ERROR_RESOURCE_NOT_EXISTS, "ID=" + id);
		}

		AssignmentDetailResponse detailResponse = ConverterFactory
				.getConverter(AssignmentDetailConverter.class).convert(assignmentTaskEntity);
		return Result.success(detailResponse);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deployAssignments(List<Long> ids) {
		checkAssignmentAllExist(ids);
		ids.forEach(id -> {
			AssignmentTaskEntity assignmentTaskEntity = assignmentTaskDAO.getById(id);
			if (assignmentTaskEntity.getPublished()) {
				throw new DbswitchException(ResultCode.ERROR_RESOURCE_HAS_DEPLOY, "ID=" + id);
			}
		});

		for (Long id : ids) {
			AssignmentTaskEntity assignmentTaskEntity = assignmentTaskDAO.getById(id);
			AssignmentConfigEntity assignmentConfigEntity = assignmentConfigDAO.getByAssignmentTaskId(id);

			DbswichPropertiesConfiguration properties = new DbswichPropertiesConfiguration();
			properties.setSource(this.getSourceDataSourceProperties(assignmentConfigEntity));
			properties.setTarget(this.getTargetDataSourceProperties(assignmentConfigEntity));
			properties.setConfig(this.getGlobalParamConfigProperties(assignmentConfigEntity));

			assignmentTaskEntity.setPublished(Boolean.TRUE);
			assignmentTaskEntity.setContent(JsonUtils.toJsonString(properties));
			assignmentTaskDAO.updateById(assignmentTaskEntity);

			ScheduleModeEnum systemScheduled = ScheduleModeEnum.SYSTEM_SCHEDULED;
			if (assignmentTaskEntity.getScheduleMode() == systemScheduled) {
				scheduleService.scheduleTask(assignmentTaskEntity.getId(), systemScheduled);
			}
		}

	}

	@Transactional(rollbackFor = Exception.class)
	public void runAssignments(List<Long> ids) {
		checkAssignmentAllExist(ids);
		List<AssignmentTaskEntity> tasks = new ArrayList<>();
		for (Long id : ids) {
			AssignmentTaskEntity assignmentTaskEntity = assignmentTaskDAO.getById(id);
			if (assignmentTaskEntity.getPublished()) {
				tasks.add(assignmentTaskEntity);
			} else {
				throw new DbswitchException(ResultCode.ERROR_RESOURCE_NOT_DEPLOY, "ID=" + id);
			}
		}

		tasks.forEach(assignmentTask -> {
			scheduleService.scheduleTask(assignmentTask.getId(), ScheduleModeEnum.MANUAL);
		});

	}

	@Transactional(rollbackFor = Exception.class)
	public void retireAssignments(List<Long> ids) {
		checkAssignmentAllExist(ids);
		for (Long id : ids) {
			AssignmentTaskEntity assignmentTaskEntity = assignmentTaskDAO.getById(id);
			if (Objects.nonNull(assignmentTaskEntity.getPublished())
					&& assignmentTaskEntity.getPublished()) {
				String jobKey = assignmentTaskEntity.getJobKey();
				scheduleService.cancelByJobKey(jobKey);
				scheduleService.cancelManualJob(id);
				assignmentTaskEntity.setPublished(Boolean.FALSE);
				assignmentTaskEntity.setContent("{}");
				assignmentTaskEntity.setJobKey("");
				assignmentTaskDAO.updateById(assignmentTaskEntity);
			}
		}
	}

	private void checkAssignmentAllExist(List<Long> ids) {
		for (Long id : ids) {
			if (Objects.isNull(assignmentTaskDAO.getById(id))) {
				throw new DbswitchException(ResultCode.ERROR_RESOURCE_NOT_EXISTS, "ID=" + id);
			}
		}
	}

	private SourceDataSourceProperties getSourceDataSourceProperties(
			AssignmentConfigEntity assignmentConfigEntity) {
		SourceDataSourceProperties sourceDataSourceProperties = new SourceDataSourceProperties();
		DatabaseConnectionEntity sourceDatabaseConnectionEntity = databaseConnectionDAO.getById(
				assignmentConfigEntity.getSourceConnectionId()
		);
		File driverVersionFile = driverLoadService.getVersionDriverFile(
				sourceDatabaseConnectionEntity.getType(),
				sourceDatabaseConnectionEntity.getVersion());
		sourceDataSourceProperties.setUrl(sourceDatabaseConnectionEntity.getUrl());
		sourceDataSourceProperties.setDriverClassName(sourceDatabaseConnectionEntity.getDriver());
		sourceDataSourceProperties.setDriverPath(driverVersionFile.getAbsolutePath());
		sourceDataSourceProperties.setUsername(sourceDatabaseConnectionEntity.getUsername());
		sourceDataSourceProperties.setPassword(sourceDatabaseConnectionEntity.getPassword());

		String sourceSchema = assignmentConfigEntity.getSourceSchema();
		if (assignmentConfigEntity.getExcluded()) {
			if (CollectionUtils.isEmpty(assignmentConfigEntity.getSourceTables())) {
				sourceDataSourceProperties.setSourceExcludes("");
			} else {
				sourceDataSourceProperties.setSourceExcludes(
						assignmentConfigEntity.getSourceTables()
								.stream().collect(Collectors.joining(","))
				);
			}
		} else {
			if (CollectionUtils.isEmpty(assignmentConfigEntity.getSourceTables())) {
				sourceDataSourceProperties.setSourceIncludes("");
			} else {
				sourceDataSourceProperties.setSourceIncludes(
						assignmentConfigEntity.getSourceTables()
								.stream().collect(Collectors.joining(","))
				);
			}
		}
		sourceDataSourceProperties.setSourceSchema(sourceSchema);
		sourceDataSourceProperties.setRegexTableMapper(assignmentConfigEntity.getTableNameMap());
		sourceDataSourceProperties.setRegexColumnMapper(assignmentConfigEntity.getColumnNameMap());
		sourceDataSourceProperties.setFetchSize(assignmentConfigEntity.getBatchSize());
		sourceDataSourceProperties.setTableType(assignmentConfigEntity.getTableType().name());
		return sourceDataSourceProperties;
	}

	private TargetDataSourceProperties getTargetDataSourceProperties(
			AssignmentConfigEntity assignmentConfigEntity) {
		TargetDataSourceProperties targetDataSourceProperties = new TargetDataSourceProperties();
		DatabaseConnectionEntity targetDatabaseConnectionEntity = databaseConnectionDAO
				.getById(assignmentConfigEntity.getTargetConnectionId());
		File driverVersionFile = driverLoadService.getVersionDriverFile(
				targetDatabaseConnectionEntity.getType(),
				targetDatabaseConnectionEntity.getVersion());
		targetDataSourceProperties.setUrl(targetDatabaseConnectionEntity.getUrl());
		targetDataSourceProperties.setDriverClassName(targetDatabaseConnectionEntity.getDriver());
		targetDataSourceProperties.setDriverPath(driverVersionFile.getAbsolutePath());
		targetDataSourceProperties.setUsername(targetDatabaseConnectionEntity.getUsername());
		targetDataSourceProperties.setPassword(targetDatabaseConnectionEntity.getPassword());
		targetDataSourceProperties.setTargetSchema(assignmentConfigEntity.getTargetSchema());
		if (assignmentConfigEntity.getTargetDropTable()) {
			targetDataSourceProperties.setTargetDrop(Boolean.TRUE);
			targetDataSourceProperties.setChangeDataSync(Boolean.FALSE);
		} else {
			targetDataSourceProperties.setTargetDrop(Boolean.FALSE);
			targetDataSourceProperties.setChangeDataSync(Boolean.TRUE);
		}
		if (assignmentConfigEntity.getTargetOnlyCreate()) {
			targetDataSourceProperties.setOnlyCreate(Boolean.TRUE);
		}
		if (assignmentConfigEntity.getTargetAutoIncrement()) {
			targetDataSourceProperties.setCreateTableAutoIncrement(Boolean.TRUE);
		}
		targetDataSourceProperties.setTableNameCase(assignmentConfigEntity.getTableNameCase());
		targetDataSourceProperties.setColumnNameCase(assignmentConfigEntity.getColumnNameCase());
		targetDataSourceProperties.setTargetSyncOption(assignmentConfigEntity.getTargetSyncOption());
		targetDataSourceProperties.setBeforeSqlScripts(assignmentConfigEntity.getBeforeSqlScripts());
		targetDataSourceProperties.setAfterSqlScripts(assignmentConfigEntity.getAfterSqlScripts());

		return targetDataSourceProperties;
	}

	private GlobalParamConfigProperties getGlobalParamConfigProperties(
			AssignmentConfigEntity assignmentConfigEntity) {
		GlobalParamConfigProperties configProperties = new GlobalParamConfigProperties();
		configProperties.setChannelQueueSize(assignmentConfigEntity.getChannelSize());
		return configProperties;
	}

	public void exportAssignments(List<Long> ids, HttpServletResponse response) {
		checkAssignmentAllExist(ids);
		List<AssignmentsDataResponse> assignmentsDataResponses = new ArrayList<>();
		// TODO 任务导出
		for (Long id : ids) {
			AssignmentTaskEntity assignmentTaskEntity = assignmentTaskDAO.getById(id);
//			AssignmentsDataResponse assignmentsDataResponse =
//					this.assignmentConvert.toAssignmentsDataResponse(assignmentTaskEntity);
			AssignmentsDataResponse assignmentsDataResponse = ConverterFactory.getConverter(AssignmentsConverter.class)
					.convert(assignmentTaskEntity);

			AssignmentConfigEntity assignmentConfigEntity = this.assignmentConfigDAO.getByAssignmentTaskId(id);

			Long sourceConnectionId = assignmentConfigEntity.getSourceConnectionId();
			DatabaseConnectionEntity databaseConnectionEntity = this.databaseConnectionDAO.getById(sourceConnectionId);
			String sourceSchema = assignmentConfigEntity.getSourceSchema();
			assignmentsDataResponse.setSourceSchema(sourceSchema);
			String sourceType = databaseConnectionEntity.getType().getName();
			assignmentsDataResponse.setSourceType(sourceType);

			Long targetConnectionId = assignmentConfigEntity.getTargetConnectionId();
			DatabaseConnectionEntity databaseConnectionEntity1 = this.databaseConnectionDAO.getById(targetConnectionId);
			String targetSchema = assignmentConfigEntity.getTargetSchema();
			assignmentsDataResponse.setTargetSchema(targetSchema);
			String targetType = databaseConnectionEntity1.getType().getName();
			assignmentsDataResponse.setTargetType(targetType);

			AssignmentJobEntity assignmentJobEntity = this.assignmentJobMapper.selectOne(
					new LambdaQueryWrapper<AssignmentJobEntity>()
							.eq(AssignmentJobEntity::getAssignmentId, assignmentsDataResponse.getId()).orderByDesc(AssignmentJobEntity::getCreateTime)
							.last(" limit 1 "));
			Integer status = (assignmentJobEntity == null || assignmentJobEntity.getStatus() == null) ?
					JobStatusEnum.INIT.getValue() :
					assignmentJobEntity.getStatus();
			assignmentsDataResponse.setRunStatus(JobStatusEnum.of(status).getName());
			assignmentsDataResponses.add(assignmentsDataResponse);

		}
		try {
			// 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
//			response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			response.setContentType("application/vnd.ms-excel");
			response.setCharacterEncoding("utf-8");
			// 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
			String fileName = URLEncoder.encode("测试", "UTF-8").replaceAll("\\+", "%20");
//			response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
			EasyExcel.write(response.getOutputStream(), AssignmentsDataResponse.class)
					.sheet("模板")
					.doWrite(assignmentsDataResponses);
		} catch (IOException ex) {
			throw new DbswitchException(ResultCode.ERROR_INTERNAL_ERROR, ex.getMessage());
		}
	}
}
