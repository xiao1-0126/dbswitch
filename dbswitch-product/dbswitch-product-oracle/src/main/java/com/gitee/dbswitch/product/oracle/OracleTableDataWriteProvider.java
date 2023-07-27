// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.product.oracle;

import com.gitee.dbswitch.common.util.ObjectCastUtils;
import com.gitee.dbswitch.provider.ProductFactoryProvider;
import com.gitee.dbswitch.provider.write.DefaultTableDataWriteProvider;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.SqlTypeValue;

public class OracleTableDataWriteProvider extends DefaultTableDataWriteProvider {

  public OracleTableDataWriteProvider(ProductFactoryProvider factoryProvider) {
    super(factoryProvider);
  }

  @Override
  public long write(List<String> fieldNames, List<Object[]> recordValues) {
    List<InputStream> iss = new ArrayList<>();
    recordValues.parallelStream().forEach((Object[] row) -> {
      for (int i = 0; i < row.length; ++i) {
        int dataType = this.columnType.get(fieldNames.get(i));
        row[i] = OracleCastUtils.castByJdbcType(dataType, row[i], iss);
      }
    });

    try {
      return super.write(fieldNames, recordValues);
    } finally {
      iss.forEach(is -> {
        try {
          is.close();
        } catch (Exception ignore) {
        }
      });
    }
  }
}
