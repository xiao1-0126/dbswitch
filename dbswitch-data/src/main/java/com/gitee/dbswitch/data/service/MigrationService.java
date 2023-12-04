// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.data.service;

import com.gitee.dbswitch.common.entity.CloseableDataSource;
import com.gitee.dbswitch.common.entity.LoggingRunnable;
import com.gitee.dbswitch.common.entity.MdcKeyValue;
import com.gitee.dbswitch.common.entity.PrintablePerfStat;
import com.gitee.dbswitch.common.util.MachineInfoUtils;
import com.gitee.dbswitch.core.exchange.AbstractBatchExchanger;
import com.gitee.dbswitch.core.robot.RobotReader;
import com.gitee.dbswitch.core.robot.RobotWriter;
import com.gitee.dbswitch.data.config.DbswichPropertiesConfiguration;
import com.gitee.dbswitch.data.entity.GlobalParamConfigProperties;
import com.gitee.dbswitch.data.util.DataSourceUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

/**
 * 数据迁移主逻辑类
 *
 * @author tang
 */
@Slf4j
@Service
public class MigrationService {

  /**
   * 性能统计记录表
   */
  private final List<PrintablePerfStat> perfStats = new ArrayList<>();

  /**
   * 配置参数
   */
  private final DbswichPropertiesConfiguration configuration;
  private final AsyncTaskExecutor readExecutor;
  private final AsyncTaskExecutor writeExecutor;

  private RobotReader robotReader;
  private RobotWriter robotWriter;

  /**
   * 任务执行实时记录MDC
   */
  private MdcKeyValue mdcKeyValue;

  /**
   * 构造函数
   *
   * @param properties 配置信息
   */
  public MigrationService(DbswichPropertiesConfiguration properties,
      AsyncTaskExecutor tableReadExecutor,
      AsyncTaskExecutor tableWriteExecutor) {
    this.configuration = Objects.requireNonNull(properties, "properties is null");
    this.readExecutor = Objects.requireNonNull(tableReadExecutor, "tableReadExecutor is null");
    this.writeExecutor = Objects.requireNonNull(tableWriteExecutor, "tableWriteExecutor is null");
  }

  public void setMdcKeyValue(MdcKeyValue mdcKeyValue) {
    this.mdcKeyValue = Objects.requireNonNull(mdcKeyValue, "mdcKeyValue is null");
  }

  /**
   * 中断执行中的任务
   */
  synchronized public void interrupt() {
    if (null != robotReader) {
      robotReader.interrupt();
    }
    if (null != robotWriter) {
      robotWriter.interrupt();
    }
  }

  /**
   * 执行入口
   */
  public void run() {
    if (Objects.nonNull(mdcKeyValue)) {
      Runnable runnable = new LoggingRunnable(this::doRun, this.mdcKeyValue);
      runnable.run();
    } else {
      doRun();
    }
  }

  /**
   * 执行主逻辑
   */
  private void doRun() {
    StopWatch watch = new StopWatch();
    watch.start();

    log.info("dbswitch data service is started....");
    log.info(MachineInfoUtils.getOSInfo());
    //log.info("input configuration \n{}", JsonUtils.toJsonString(configuration));

    GlobalParamConfigProperties globalParam = configuration.getConfig();
    int maxQueueSize = globalParam.getChannelQueueSize();
    AbstractBatchExchanger exchanger = new DefaultBatchExchanger(readExecutor, writeExecutor, maxQueueSize, perfStats);
    try (CloseableDataSource targetDataSource = DataSourceUtils.createTargetDataSource(configuration.getTarget())) {
      try (CloseableDataSource sourceDataSource = DataSourceUtils.createSourceDataSource(configuration.getSource())) {
        robotReader = new DefaultReaderRobot(mdcKeyValue, configuration, sourceDataSource, targetDataSource);
        robotWriter = new DefaultWriterRobot(mdcKeyValue, robotReader, globalParam.getWriteThreadNum());
        exchanger.exchange(robotReader, robotWriter);
      }
    } catch (Throwable t) {
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      }
      throw new RuntimeException(t);
    } finally {
      watch.stop();
      log.info("total ellipse = {} s", watch.getTotalTimeSeconds());

      StringBuilder sb = new StringBuilder();
      sb.append("=====================================\n");
      sb.append(String.format("total ellipse time:\t %f s\n", watch.getTotalTimeSeconds()));
      sb.append("-------------------------------------\n");
      perfStats.forEach(st -> sb.append(st.getPrintableString()));
      sb.append("=====================================\n");
      log.info("\n\n" + sb.toString());
    }
  }

}
