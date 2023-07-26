// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.mariadb;

import com.gitee.dbswitch.annotation.Product;
import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.features.ProductFeatures;
import com.gitee.dbswitch.product.mysql.MysqlMetadataQueryProvider;
import com.gitee.dbswitch.product.mysql.MysqlTableSynchronizer;
import com.gitee.dbswitch.provider.AbstractFactoryProvider;
import com.gitee.dbswitch.provider.meta.MetadataProvider;
import com.gitee.dbswitch.provider.sync.TableDataSynchronizer;
import javax.sql.DataSource;

@Product(ProductTypeEnum.MARIADB)
public class MariadbFactoryProvider extends AbstractFactoryProvider {

  public MariadbFactoryProvider(DataSource dataSource) {
    super(dataSource);
  }

  public ProductFeatures getProductFeatures() {
    return new MariadbFeatures();
  }

  @Override
  public MetadataProvider createMetadataQueryProvider() {
    return new MysqlMetadataQueryProvider(this);
  }

  @Override
  public TableDataSynchronizer createTableDataSynchronizer() {
    return new MysqlTableSynchronizer(this);
  }

}
