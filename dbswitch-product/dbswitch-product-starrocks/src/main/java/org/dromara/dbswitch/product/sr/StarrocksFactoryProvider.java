// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.product.sr;

import org.dromara.dbswitch.core.annotation.Product;
import org.dromara.dbswitch.common.type.ProductTypeEnum;
import org.dromara.dbswitch.core.features.ProductFeatures;
import org.dromara.dbswitch.core.provider.AbstractFactoryProvider;
import org.dromara.dbswitch.core.provider.meta.MetadataProvider;
import org.dromara.dbswitch.core.provider.sync.AutoCastTableDataSynchronizeProvider;
import org.dromara.dbswitch.core.provider.sync.TableDataSynchronizeProvider;
import org.dromara.dbswitch.core.provider.write.AutoCastTableDataWriteProvider;
import org.dromara.dbswitch.core.provider.write.TableDataWriteProvider;
import javax.sql.DataSource;

@Product(ProductTypeEnum.STARROCKS)
public class StarrocksFactoryProvider extends AbstractFactoryProvider {

  public StarrocksFactoryProvider(DataSource dataSource) {
    super(dataSource);
  }

  public ProductFeatures getProductFeatures() {
    return new StarrocksFeatures();
  }

  @Override
  public MetadataProvider createMetadataQueryProvider() {
    return new StarrocksMetadataQueryProvider(this);
  }

  @Override
  public TableDataWriteProvider createTableDataWriteProvider(boolean useInsert) {
    return new AutoCastTableDataWriteProvider(this);
  }

  @Override
  public TableDataSynchronizeProvider createTableDataSynchronizeProvider() {
    return new AutoCastTableDataSynchronizeProvider(this);
  }


}
