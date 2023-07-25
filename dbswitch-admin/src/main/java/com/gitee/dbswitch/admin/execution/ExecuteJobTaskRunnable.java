// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.admin.execution;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.gitee.dbswitch.admin.config.ExecutorConfig;
import com.gitee.dbswitch.admin.dao.AssignmentConfigDAO;
import com.gitee.dbswitch.admin.dao.AssignmentJobDAO;
import com.gitee.dbswitch.admin.dao.AssignmentTaskDAO;
import com.gitee.dbswitch.admin.entity.AssignmentConfigEntity;
import com.gitee.dbswitch.admin.entity.AssignmentJobEntity;
import com.gitee.dbswitch.admin.entity.AssignmentTaskEntity;
import com.gitee.dbswitch.admin.logback.LogbackAppenderRegister;
import com.gitee.dbswitch.admin.type.JobStatusEnum;
import com.gitee.dbswitch.common.entity.MdcKeyValue;
import com.gitee.dbswitch.data.config.DbswichProperties;
import com.gitee.dbswitch.data.service.MigrationService;
import com.gitee.dbswitch.data.util.JsonUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.sql.Timestamp;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;

@Slf4j
public class ExecuteJobTaskRunnable implements Runnable {

  private final static String MDC_KEY = LogbackAppenderRegister.LOG_MDC_KEY_NAME;

  // 相同taskId的任务限制并发执行的粒度锁缓存对象
  private static Cache<String, ReentrantLock> mutexes = CacheBuilder.newBuilder()
      .expireAfterWrite(24 * 60L, TimeUnit.MINUTES)
      .build();

  private volatile boolean interrupted = false;

  private MigrationService migrationService;

  private AssignmentTaskDAO assignmentTaskDAO;

  private AssignmentConfigDAO assignmentConfigDAO;

  private AssignmentJobDAO assignmentJobDAO;

  private AsyncTaskExecutor migrationTaskExecutor;

  private Long taskId;

  private Integer schedule;

  private String keyName;

  public ExecuteJobTaskRunnable(Long taskId, Integer schedule, String keyName) {
    this.assignmentTaskDAO = SpringUtil.getBean(AssignmentTaskDAO.class);
    this.assignmentConfigDAO = SpringUtil.getBean(AssignmentConfigDAO.class);
    this.assignmentJobDAO = SpringUtil.getBean(AssignmentJobDAO.class);
    this.migrationTaskExecutor = SpringUtil.getBean(
        ExecutorConfig.TASK_EXECUTOR_BEAN_NAME, AsyncTaskExecutor.class);
    this.taskId = taskId;
    this.schedule = schedule;
    this.keyName = keyName;
  }

  public void interrupt() {
    this.interrupted = true;
    if (null != this.migrationService) {
      this.migrationService.interrupt();
    }
  }

  @Override
  public void run() {
    AssignmentJobEntity assignmentJobEntity = assignmentJobDAO
        .newAssignmentJob(taskId, schedule, keyName);
    MdcKeyValue mdcKeyValue = new MdcKeyValue(MDC_KEY, assignmentJobEntity.getId().toString());

    try {
      ReentrantLock lock = mutexes.get(taskId.toString(), ReentrantLock::new);
      while (!lock.tryLock(1, TimeUnit.SECONDS)) {
        if (interrupted) {
          log.info("Quartz task id:{} interrupted when get lock", taskId);
          return;
        }
        TimeUnit.SECONDS.sleep(1);
      }

      try {
        log.info("Execute Job, and task id is : {} , job id is: {}",
            taskId, assignmentJobEntity.getId());

        AssignmentTaskEntity task = assignmentTaskDAO.getById(taskId);
        AssignmentConfigEntity assignmentConfigEntity = assignmentConfigDAO
            .getByAssignmentTaskId(task.getId());

        log.info("Execute Assignment [taskId={}],Task Name: {} ,configuration properties：{}",
            task.getId(),
            task.getName(),
            task.getContent());

        try {
          DbswichProperties properties = JsonUtils.toBeanObject(
              task.getContent(), DbswichProperties.class);
          if (!assignmentConfigEntity.getFirstFlag()) {
            if (!assignmentConfigEntity.getTargetOnlyCreate()) {
              properties.getTarget().setTargetDrop(false);
              properties.getTarget().setOnlyCreate(false);
              properties.getTarget().setChangeDataSync(true);
            }
          }
          if (assignmentConfigEntity.getTargetOnlyCreate()) {
            properties.getTarget().setTargetDrop(true);
          }

          migrationService = new MigrationService(properties, migrationTaskExecutor);
          if (interrupted) {
            log.info("Quartz task id:{} interrupted when prepare stage", taskId);
            return;
          }

          // 实际执行JOB
          migrationService.setMdcKeyValue(mdcKeyValue);
          migrationService.run();

          if (assignmentConfigEntity.getFirstFlag()) {
            AssignmentConfigEntity config = new AssignmentConfigEntity();
            config.setId(assignmentConfigEntity.getId());
            config.setTargetDropTable(assignmentConfigEntity.getTargetOnlyCreate());
            config.setFirstFlag(Boolean.FALSE);
            assignmentConfigDAO.updateSelective(config);
          }

          assignmentJobEntity.setStatus(JobStatusEnum.PASS.getValue());
          log.info("Execute Assignment Success [taskId={},jobId={}],Task Name: {}",
              task.getId(), assignmentJobEntity.getId(), task.getName());
        } catch (Throwable e) {
          assignmentJobEntity.setStatus(JobStatusEnum.FAIL.getValue());
          assignmentJobEntity.setErrorLog(ExceptionUtil.stacktraceToString(e));
          log.info("Execute Assignment Failed [taskId={},jobId={}],Task Name: {}",
              task.getId(), assignmentJobEntity.getId(), task.getName(), e);
        } finally {
          assignmentJobEntity.setFinishTime(new Timestamp(System.currentTimeMillis()));
          assignmentJobDAO.updateSelective(assignmentJobEntity);
        }
      } finally {
        lock.unlock();
      }
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
