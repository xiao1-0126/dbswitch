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

import com.gitee.dbswitch.common.type.ProductTypeEnum;
import com.gitee.dbswitch.product.oracle.OracleTableManageProvider;
import com.gitee.dbswitch.provider.ProductFactoryProvider;
import com.gitee.dbswitch.provider.manage.DefaultTableManageProvider;
import com.gitee.dbswitch.provider.manage.TableManageProvider;

public class OceanbaseTableManageProvider extends DefaultTableManageProvider {

  private final TableManageProvider delegate;
  private final ProductTypeEnum dialect;

  public OceanbaseTableManageProvider(ProductFactoryProvider factoryProvider, TableManageProvider delegate) {
    super(factoryProvider);
    this.delegate = delegate;
    if (delegate instanceof OracleTableManageProvider) {
      this.dialect = ProductTypeEnum.ORACLE;
    } else {
      this.dialect = ProductTypeEnum.MYSQL;
    }
  }

  @Override
  protected String quoteName(String name) {
    return this.dialect.quoteName(name);
  }

  @Override
  protected String quoteSchemaTableName(String schemaName, String tableName) {
    return this.dialect.quoteSchemaTableName(schemaName, tableName);
  }

  @Override
  public void truncateTableData(String schemaName, String tableName) {
    this.delegate.truncateTableData(schemaName, tableName);
  }

  @Override
  public void dropTable(String schemaName, String tableName) {
    this.delegate.dropTable(schemaName, tableName);
  }
}
