// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.hive;

import com.gitee.dbswitch.features.ProductFeatures;

public class HiveFeatures implements ProductFeatures {

  /**
   * 是否使用CreateTableAs方式建表
   *
   * @return boolean
   */
  public boolean useCTAS() {
    return false;
  }
}
