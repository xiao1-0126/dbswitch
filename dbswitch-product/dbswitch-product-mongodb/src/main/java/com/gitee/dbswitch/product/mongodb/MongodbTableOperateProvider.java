// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.mongodb;

import com.gitee.dbswitch.provider.ProductFactoryProvider;
import com.gitee.dbswitch.provider.operate.DefaultTableOperateProvider;

public class MongodbTableOperateProvider extends DefaultTableOperateProvider {

  public MongodbTableOperateProvider(ProductFactoryProvider factoryProvider) {
    super(factoryProvider);
  }

  @Override
  public void truncateTableData(String schemaName, String tableName) {
    cleanup(schemaName, tableName);
  }

  @Override
  public void dropTable(String schemaName, String tableName) {
    cleanup(schemaName, tableName);
  }

  private void cleanup(String schemaName, String tableName) {
    String sql = String.format("%s.getCollection('%s').drop();", schemaName, tableName);
    this.executeSql(sql);
  }
}
