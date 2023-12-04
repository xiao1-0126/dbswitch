// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.data.handler;

import cn.hutool.core.date.StopWatch;
import com.gitee.dbswitch.core.exchange.BatchElement;
import com.gitee.dbswitch.core.exchange.MemChannel;
import com.gitee.dbswitch.core.robot.RobotReader;
import com.gitee.dbswitch.core.task.TaskProcessor;
import com.gitee.dbswitch.data.domain.WriterTaskParam;
import com.gitee.dbswitch.data.domain.WriterTaskResult;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据写入线程体（多个表的写）
 *
 * @author tang
 */
@Slf4j
public class WriterTaskThread extends TaskProcessor<WriterTaskResult> {

  private final WriterTaskParam taskParam;

  public WriterTaskThread(WriterTaskParam taskParam) {
    this.taskParam = taskParam;
  }

  @Override
  protected WriterTaskResult doProcess() {
    MemChannel memChannel = this.taskParam.getMemChannel();
    RobotReader robotReader = this.taskParam.getRobotReader();
    WriterTaskResult taskResult = WriterTaskResult.builder().success(true).build();
    StopWatch stopWatch = new StopWatch(Thread.currentThread().getName());
    stopWatch.start();

    try {
      BatchElement elem;
      while ((elem = memChannel.poll()) != null || robotReader.getRemainingCount() > 0) {
        if (Thread.currentThread().isInterrupted()) {
          break;
        }
        if (null != elem) {
          try {
            Long ret = Long.valueOf(elem.getArg2().size());
            elem.getHandler().apply(elem.getArg1(), elem.getArg2());
            Long count = taskResult.getPerf().get(elem.getTableNameMapString());
            Long total = ret + Optional.ofNullable(count).orElse(0L);
            taskResult.getPerf().put(elem.getTableNameMapString(), total);
          } catch (Throwable t) {
            taskResult.setSuccess(false);
            taskResult.getExcept().putIfAbsent(elem.getTableNameMapString(), t);
          }
        }
      }
    } finally {
      stopWatch.stop();
      taskResult.setDuration(stopWatch.getTotalTimeMillis());
    }
    return taskResult;
  }

  public WriterTaskResult exceptProcess(Throwable t) {
    return WriterTaskResult.builder()
        .success(false)
        .duration(0)
        .throwable(t)
        .build();
  }

}
