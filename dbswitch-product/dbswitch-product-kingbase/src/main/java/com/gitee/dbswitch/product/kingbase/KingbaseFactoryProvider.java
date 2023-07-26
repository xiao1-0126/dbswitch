// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.kingbase;

import com.gitee.dbswitch.annotation.Product;
import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.features.ProductFeatures;
import com.gitee.dbswitch.product.postgresql.PostgresTableOperateProvider;
import com.gitee.dbswitch.product.postgresql.PostgresTableSynchronizer;
import com.gitee.dbswitch.provider.AbstractFactoryProvider;
import com.gitee.dbswitch.provider.meta.MetadataProvider;
import com.gitee.dbswitch.provider.operate.TableOperateProvider;
import com.gitee.dbswitch.provider.sync.TableDataSynchronizer;
import com.gitee.dbswitch.provider.write.TableDataWriteProvider;
import javax.sql.DataSource;

@Product(ProductTypeEnum.KINGBASE)
public class KingbaseFactoryProvider extends AbstractFactoryProvider {

  public KingbaseFactoryProvider(DataSource dataSource) {
    super(dataSource);
  }

  public ProductFeatures getProductFeatures() {
    return new KingbaseFeatures();
  }

  @Override
  public MetadataProvider createMetadataQueryProvider() {
    return new KingbaseMetadataQueryProvider(this);
  }

  @Override
  public TableOperateProvider createTableOperateProvider() {
    return new PostgresTableOperateProvider(this);
  }

  @Override
  public TableDataWriteProvider createTableDataWriteProvider(boolean useInsert) {
    return new KingbaseTableWriteProvider(this);
  }

  @Override
  public TableDataSynchronizer createTableDataSynchronizer() {
    return new PostgresTableSynchronizer(this);
  }
}
