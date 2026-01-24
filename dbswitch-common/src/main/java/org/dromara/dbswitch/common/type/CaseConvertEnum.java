// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.common.type;

import cn.hutool.core.util.StrUtil;
import java.util.function.Function;

/**
 * 处理名称转换的枚举类
 *
 * @author tang
 */
public enum CaseConvertEnum {
  /**
   * 不做任何处理
   */
  NONE(s -> s),
  /**
   * 全大写
   */
  UPPER(String::toUpperCase),
  /**
   * 全小写
   */
  LOWER(String::toLowerCase),
  /**
   * 驼峰转下划线
   */
  SNAKE(StrUtil::toUnderlineCase),
  /**
   * 下划线转驼峰
   */
  CAMEL(StrUtil::toCamelCase);

  private Function<String, String> function;

  CaseConvertEnum(Function<String, String> function) {
    this.function = function;
  }

  public String convert(String name) {
    return function.apply(name);
  }
}
