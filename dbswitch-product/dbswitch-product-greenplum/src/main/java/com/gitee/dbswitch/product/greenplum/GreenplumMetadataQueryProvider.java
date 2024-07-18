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

import com.gitee.dbswitch.product.postgresql.PostgresMetadataQueryProvider;
import com.gitee.dbswitch.provider.ProductFactoryProvider;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GreenplumMetadataQueryProvider extends PostgresMetadataQueryProvider {


  public GreenplumMetadataQueryProvider(ProductFactoryProvider factoryProvider) {
    super(factoryProvider);
  }


  @Override
  public void postAppendCreateTableSql(StringBuilder builder, String tblComment, List<String> primaryKeys,
      Map<String, String> tblProperties) {
    // 有主键就优先使用主键作为分布键。
    if (Objects.nonNull(primaryKeys) && !primaryKeys.isEmpty()) {
      getPkOrDkAsString(builder, primaryKeys);
      log.info("using primary key as distributed key");
    }
  }

  private void getPkOrDkAsString(StringBuilder builder, List<String> primaryKeys) {
    String pk = getPrimaryKeyAsString(primaryKeys);
    builder.append("\n DISTRIBUTED BY (").append(pk).append(")");
  }
}
