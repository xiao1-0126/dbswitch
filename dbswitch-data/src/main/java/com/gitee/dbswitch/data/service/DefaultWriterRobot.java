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

import com.gitee.dbswitch.common.entity.LoggingSupplier;
import com.gitee.dbswitch.common.entity.MdcKeyValue;
import com.gitee.dbswitch.common.util.ExamineUtils;
import com.gitee.dbswitch.core.robot.RobotReader;
import com.gitee.dbswitch.core.robot.RobotWriter;
import com.gitee.dbswitch.data.domain.WriterTaskParam;
import com.gitee.dbswitch.data.domain.WriterTaskResult;
import com.gitee.dbswitch.data.handler.WriterTaskThread;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * 目标数据库表并发写入控制
 *
 * @author tang
 */
@Slf4j
public class DefaultWriterRobot extends RobotWriter<WriterTaskResult> {

  private final MdcKeyValue mdcKeyValue;
  private final RobotReader robotReader;
  private final int writeThreadNum; // 4 <= writeThreadNum <= 8
  private AsyncTaskExecutor threadExecutor;
  private List<Supplier> writeTaskThreads;
  private List<CompletableFuture> futures;

  public DefaultWriterRobot(MdcKeyValue mdcKeyValue, RobotReader robotReader, int writeThreadNum) {
    ExamineUtils.checkArgument(writeThreadNum > 0, "writeThreadNum(%s) must >0 ", writeThreadNum);
    this.mdcKeyValue = mdcKeyValue;
    this.robotReader = robotReader;
    this.writeThreadNum = writeThreadNum;
  }

  @Override
  public void interrupt() {
    Optional.ofNullable(futures).orElseGet(ArrayList::new).forEach(f -> f.cancel(true));
  }

  @Override
  public void init(AsyncTaskExecutor threadExecutor) {
    this.threadExecutor = threadExecutor;
    this.writeTaskThreads = new ArrayList<>();

    WriterTaskParam param = WriterTaskParam
        .builder()
        .robotReader(robotReader)
        .memChannel(robotReader.getChannel())
        .build();
    for (int i = 0; i < writeThreadNum; ++i) {
      if (Objects.nonNull(mdcKeyValue)) {
        writeTaskThreads.add(new LoggingSupplier(new WriterTaskThread(param), mdcKeyValue));
      } else {
        writeTaskThreads.add(new WriterTaskThread(param));
      }
    }
  }

  @Override
  public void startWrite() {
    futures = new ArrayList<>(writeTaskThreads.size());
    writeTaskThreads.forEach(
        task ->
            futures.add(CompletableFuture.supplyAsync(task, threadExecutor))
    );
  }

  @Override
  public void waitForFinish() {
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
  }

  @Override
  public Optional<WriterTaskResult> getWorkResult() {
    return futures.stream().map(CompletableFuture::join)
        .filter(Objects::nonNull)
        .map(f -> (WriterTaskResult) f)
        .peek(f -> f.padding())
        .reduce(
            (r1, r2) -> {
              Map<String, Long> perf = Maps.newHashMap(r1.getPerf());
              if (r2.isSuccess()) {
                r2.getPerf().forEach((k, v) -> {
                  Long count = Optional.ofNullable(perf.get(k)).orElse(0L) + v;
                  perf.put(k, count);
                });
              }
              Map<String, Throwable> except = Maps.newHashMap(r1.getExcept());
              if (r2.getExcept().size() > 0) {
                except.putAll(r2.getExcept());
              }
              return WriterTaskResult.builder()
                  .perf(perf)
                  .except(except)
                  .success(r1.isSuccess() && r2.isSuccess())
                  .duration(Math.max(r1.getDuration(), r2.getDuration()))
                  .throwable(Objects.nonNull(r1.getThrowable()) ? r1.getThrowable() : r2.getThrowable())
                  .build();
            }
        );
  }

}
