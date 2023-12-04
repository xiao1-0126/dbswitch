// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.core.exchange;

import java.util.List;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 队列中的批元素结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchElement {

  /**
   * 表名映射字符串，形如 A --> B 的形式
   */
  private String tableNameMapString;

  /**
   * 数据写入回调函数
   */
  private BiFunction<List<String>, List<Object[]>, Long> handler;

  /**
   * 写入回调函数的第1个参数
   */
  private List<String> arg1;

  /**
   * 写入回调函数的第2个参数
   */
  private List<Object[]> arg2;
}
