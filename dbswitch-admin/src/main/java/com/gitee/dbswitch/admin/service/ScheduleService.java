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

import com.gitee.dbswitch.admin.dao.AssignmentJobDAO;
import com.gitee.dbswitch.admin.dao.AssignmentTaskDAO;
import com.gitee.dbswitch.admin.entity.AssignmentJobEntity;
import com.gitee.dbswitch.admin.entity.AssignmentTaskEntity;
import com.gitee.dbswitch.admin.execution.ExecuteJobTaskRunnable;
import com.gitee.dbswitch.admin.type.JobStatusEnum;
import com.gitee.dbswitch.admin.type.ScheduleModeEnum;
import com.gitee.dbswitch.common.event.TaskEventHub;
import com.gitee.dbswitch.common.util.UuidUtils;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScheduleService {

  private static final String evenName = "start";

  static {
    TaskEventHub.init(5);
  }

  /**
   * @Bean是一个方法级别上的注解，Bean的ID为方法名字。
   * @Resource默认按照ByName自动注入
   * @Autowired默认按照类型byType注入
   */
  @Autowired
  private SchedulerFactoryBean schedulerFactoryBean;

  @Resource
  private AssignmentTaskDAO assignmentTaskDAO;

  @Resource
  private AssignmentJobDAO assignmentJobDAO;

  private TaskEventHub manualRunEvenHub = new TaskEventHub("manualRun");

  private Map<String, ExecuteJobTaskRunnable> taskRunnableMap = new ConcurrentHashMap<>();

  @EventListener(ApplicationReadyEvent.class)
  public void registerEventListener() {
    manualRunEvenHub.listen(
        evenName,
        (event) -> {
          event.checkArgs(Long.class, String.class);
          Long taskId = (Long) event.getArgs()[0];
          Integer schedule = ScheduleModeEnum.MANUAL.getValue();
          String jobKey = (String) event.getArgs()[1];
          ExecuteJobTaskRunnable taskRunnable
              = new ExecuteJobTaskRunnable(taskId, schedule, jobKey);
          taskRunnableMap.put(jobKey, taskRunnable);
          try {
            taskRunnable.run();
          } finally {
            taskRunnableMap.remove(jobKey);
          }
          return null;
        }
    );
  }

  public void scheduleTask(Long taskId, ScheduleModeEnum scheduleMode) {
    /** 准备JobDetail */
    String jobKeyName = UuidUtils.generateUuid() + "@" + taskId.toString();
    String jobGroup = JobExecutorService.GROUP;
    JobKey jobKey = JobKey.jobKey(jobKeyName, jobGroup);

    JobBuilder jobBuilder = JobBuilder.newJob(JobExecutorService.class)
        .withIdentity(jobKey)
        .usingJobData(JobExecutorService.TASK_ID, taskId.toString())
        .usingJobData(JobExecutorService.SCHEDULE, scheduleMode.getValue().toString());

    /** 准备TriggerKey，注意这里的triggerName与jobName配置相同 */
    String triggerName = jobKeyName;
    String triggerGroup = JobExecutorService.GROUP;
    TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);

    log.info("Create schedule task, taskId: {}, jobKey: {}", taskId, jobKeyName);

    AssignmentTaskEntity task = assignmentTaskDAO.getById(taskId);
    if (ScheduleModeEnum.MANUAL == scheduleMode) {
      manualRunEvenHub.notify(evenName, taskId, jobKeyName);
      //scheduleOnce(jobBuilder.storeDurably(false).build(), triggerKey);
    } else {
      scheduleCron(jobBuilder.storeDurably(true).build(), triggerKey, task.getCronExpression());
    }

    task.setJobKey(jobKeyName);
    assignmentTaskDAO.updateById(task);
  }

  public void cancelByJobKey(String jobKeyName) {
    if (StringUtils.isBlank(jobKeyName)) {
      return;
    }

    ExecuteJobTaskRunnable runnable = taskRunnableMap.get(jobKeyName);
    if (null != runnable) {
      runnable.interrupt();
      return;
    }

    String jobGroup = JobExecutorService.GROUP;
    JobKey jobKey = JobKey.jobKey(jobKeyName, jobGroup);

    String triggerName = jobKeyName;
    String triggerGroup = JobExecutorService.GROUP;
    TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);

    Scheduler scheduler = schedulerFactoryBean.getScheduler();

    try {
      scheduler.interrupt(jobKey);
      scheduler.pauseTrigger(triggerKey);
      scheduler.unscheduleJob(triggerKey);
      scheduler.deleteJob(jobKey);
      log.info("Quartz delete task job for JobKey: {}", jobKey);
    } catch (SchedulerException e) {
      log.error("Quartz stop task job failed. JobKey: {}", jobKey);
    }

    log.info("cancel task by job key: {}", jobKeyName);
  }

  public void cancelJob(Long jobId) {
    AssignmentJobEntity assignmentJobEntity = assignmentJobDAO.getById(jobId);
    if (Objects.nonNull(assignmentJobEntity)) {
      String jobKeyName = assignmentJobEntity.getJobKey();
      cancelByJobKey(jobKeyName);
      assignmentJobEntity.setStatus(JobStatusEnum.CANCEL.getValue());
      assignmentJobEntity.setFinishTime(new Timestamp(System.currentTimeMillis()));
      assignmentJobEntity.setErrorLog("Job was canceled!!!!");
      assignmentJobDAO.updateSelective(assignmentJobEntity);
    }
  }

//  private void scheduleOnce(JobDetail jobDetail, TriggerKey triggerKey) {
//    Scheduler scheduler = schedulerFactoryBean.getScheduler();
//    Trigger simpleTrigger = TriggerBuilder.newTrigger()
//        .startAt(new Date())
//        .withIdentity(triggerKey)
//        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0))
//        .build();
//
//    try {
//      scheduler.scheduleJob(jobDetail, simpleTrigger);
//    } catch (SchedulerException e) {
//      log.error("Quartz schedule task by manual failed, taskId: {}.",
//          jobDetail.getJobDataMap().get(JobExecutorService.TASK_ID), e);
//      throw new RuntimeException(e);
//    }
//
//  }

  private void scheduleCron(JobDetail jobDetail, TriggerKey triggerKey, String cronExpression) {
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    Trigger cronTrigger = TriggerBuilder.newTrigger()
        .withIdentity(triggerKey)
        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
        .build();

    try {
      scheduler.scheduleJob(jobDetail, cronTrigger);
    } catch (SchedulerException e) {
      log.error("Quartz schedule task by expression failed, taskId: {}.",
          jobDetail.getJobDataMap().get(JobExecutorService.TASK_ID), e);
      throw new RuntimeException(e);
    }

  }

}
