// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.oceanbase;

import com.gitee.dbswitch.annotation.Product;
import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.features.DefaultProductFeatures;
import com.gitee.dbswitch.features.ProductFeatures;
import com.gitee.dbswitch.product.mysql.MysqlFeatures;
import com.gitee.dbswitch.product.mysql.MysqlMetadataQueryProvider;
import com.gitee.dbswitch.product.oracle.OracleMetadataQueryProvider;
import com.gitee.dbswitch.product.oracle.OracleTableDataSynchronizer;
import com.gitee.dbswitch.product.oracle.OracleTableDataWriteProvider;
import com.gitee.dbswitch.product.oracle.OracleTableManageProvider;
import com.gitee.dbswitch.provider.AbstractFactoryProvider;
import com.gitee.dbswitch.provider.manage.DefaultTableManageProvider;
import com.gitee.dbswitch.provider.manage.TableManageProvider;
import com.gitee.dbswitch.provider.meta.MetadataProvider;
import com.gitee.dbswitch.provider.sync.AutoCastTableDataSynchronizeProvider;
import com.gitee.dbswitch.provider.sync.TableDataSynchronizeProvider;
import com.gitee.dbswitch.provider.write.AutoCastTableDataWriteProvider;
import com.gitee.dbswitch.provider.write.TableDataWriteProvider;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Product(ProductTypeEnum.OCEANBASE)
public class OceanbaseFactoryProvider extends AbstractFactoryProvider {

  private Boolean isMySqlMode;

  public OceanbaseFactoryProvider(DataSource dataSource) {
    super(dataSource);

    try (Connection connection = getDataSource().getConnection()) {
      this.isMySqlMode = OceanbaseUtils.isOceanBaseUseMysqlMode(connection);
      if (log.isDebugEnabled()) {
        log.debug("#### Target OceanBase is {} Mode ", this.isMySqlMode ? "MySQL" : "Oracle");
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ProductFeatures getProductFeatures() {
    return isMySqlMode ? new MysqlFeatures() : new DefaultProductFeatures();
  }

  @Override
  public MetadataProvider createMetadataQueryProvider() {
    MetadataProvider provider = isMySqlMode
        ? new MysqlMetadataQueryProvider(this)
        : new OracleMetadataQueryProvider(this);
    return new OceanbaseMetadataQueryProvider(this, provider);
  }

  @Override
  public TableDataWriteProvider createTableDataWriteProvider(boolean useInsert) {
    TableDataWriteProvider provider = isMySqlMode
        ? new AutoCastTableDataWriteProvider(this)
        : new OracleTableDataWriteProvider(this);
    return new OceanbaseTableDataWriteProvider(this, provider);
  }

  @Override
  public TableManageProvider createTableManageProvider() {
    TableManageProvider provider = isMySqlMode
        ? new DefaultTableManageProvider(this)
        : new OracleTableManageProvider(this);
    return new OceanbaseTableManageProvider(this, provider);
  }

  @Override
  public TableDataSynchronizeProvider createTableDataSynchronizeProvider() {
    TableDataSynchronizeProvider provider = isMySqlMode
        ? new AutoCastTableDataSynchronizeProvider(this)
        : new OracleTableDataSynchronizer(this);
    return new OceanbaseTableDataSynchronizer(this, provider);
  }
}
