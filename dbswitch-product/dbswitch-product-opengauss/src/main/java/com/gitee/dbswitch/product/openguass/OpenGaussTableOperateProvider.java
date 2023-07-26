// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.openguass;

import com.gitee.dbswitch.provider.ProductFactoryProvider;
import com.gitee.dbswitch.provider.operate.DefaultTableOperateProvider;

public class OpenGaussTableOperateProvider extends DefaultTableOperateProvider {

  public OpenGaussTableOperateProvider(ProductFactoryProvider factoryProvider) {
    super(factoryProvider);
  }

  @Override
  public void dropTable(String schemaName, String tableName) {
    String sql = String.format("DROP TABLE \"%s\".\"%s\" CASCADE ",
        schemaName, tableName);
    this.executeSql(sql);
  }

}
