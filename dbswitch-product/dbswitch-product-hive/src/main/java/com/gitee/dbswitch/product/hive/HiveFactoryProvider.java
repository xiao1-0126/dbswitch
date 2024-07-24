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

import com.gitee.dbswitch.annotation.Product;
import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.features.ProductFeatures;
import com.gitee.dbswitch.provider.AbstractFactoryProvider;
import com.gitee.dbswitch.provider.manage.TableManageProvider;
import com.gitee.dbswitch.provider.meta.MetadataProvider;
import com.gitee.dbswitch.provider.query.TableDataQueryProvider;
import com.gitee.dbswitch.provider.sync.TableDataSynchronizeProvider;
import com.gitee.dbswitch.provider.write.TableDataWriteProvider;
import javax.sql.DataSource;

@Product(ProductTypeEnum.HIVE)
public class HiveFactoryProvider extends AbstractFactoryProvider {

  public HiveFactoryProvider(DataSource dataSource) {
    super(dataSource);
  }

  public ProductFeatures getProductFeatures() {
    return new HiveFeatures();
  }

  @Override
  public MetadataProvider createMetadataQueryProvider() {
    return new HiveMetadataQueryProvider(this);
  }

  @Override
  public TableDataQueryProvider createTableDataQueryProvider() {
    return new HiveTableDataQueryProvider(this);
  }

//  @Override
//  public TableManageProvider createTableManageProvider() {
//    throw new UnsupportedOperationException("Unsupported hive to manage tale!");
//  }
//
//  @Override
//  public TableDataWriteProvider createTableDataWriteProvider(boolean useInsert) {
//    throw new UnsupportedOperationException("Unsupported hive to write tale data!");
//  }
//
//  @Override
//  public TableDataSynchronizeProvider createTableDataSynchronizeProvider() {
//    throw new UnsupportedOperationException("Unsupported hive to sync tale data!");
//  }
}
