// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.greenplum;

import com.gitee.dbswitch.product.postgresql.PostgresTableManageProvider;
import com.gitee.dbswitch.provider.ProductFactoryProvider;
import com.gitee.dbswitch.provider.manage.DefaultTableManageProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GreenplumTableManageProvider extends PostgresTableManageProvider {

  public GreenplumTableManageProvider(ProductFactoryProvider factoryProvider) {
    super(factoryProvider);
  }

}
