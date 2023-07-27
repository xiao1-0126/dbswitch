// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.db2;

import com.gitee.dbswitch.annotation.Product;
import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.features.ProductFeatures;
import com.gitee.dbswitch.provider.AbstractFactoryProvider;
import com.gitee.dbswitch.provider.meta.MetadataProvider;
import com.gitee.dbswitch.provider.operate.TableOperateProvider;
import com.gitee.dbswitch.provider.sync.AutoCastTableDataSynchronizer;
import com.gitee.dbswitch.provider.sync.TableDataSynchronizer;
import com.gitee.dbswitch.provider.write.AutoCastTableDataWriteProvider;
import com.gitee.dbswitch.provider.write.TableDataWriteProvider;
import javax.sql.DataSource;

@Product(ProductTypeEnum.DB2)
public class DB2FactoryProvider extends AbstractFactoryProvider {

  public DB2FactoryProvider(DataSource dataSource) {
    super(dataSource);
  }

  public ProductFeatures getProductFeatures() {
    return new DB2Features();
  }

  @Override
  public MetadataProvider createMetadataQueryProvider() {
    return new DB2MetadataQueryProvider(this);
  }

  @Override
  public TableDataWriteProvider createTableDataWriteProvider(boolean useInsert) {
    return new AutoCastTableDataWriteProvider(this);
  }

  @Override
  public TableOperateProvider createTableOperateProvider() {
    return new DB2TableOperateProvider(this);
  }

  @Override
  public TableDataSynchronizer createTableDataSynchronizer() {
    return new AutoCastTableDataSynchronizer(this);
  }

}
