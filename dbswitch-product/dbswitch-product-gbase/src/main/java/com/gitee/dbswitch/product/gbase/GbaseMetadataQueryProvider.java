package com.gitee.dbswitch.product.gbase;

import com.gitee.dbswitch.product.mysql.MysqlMetadataQueryProvider;
import com.gitee.dbswitch.provider.ProductFactoryProvider;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class GbaseMetadataQueryProvider extends MysqlMetadataQueryProvider {

  public GbaseMetadataQueryProvider(ProductFactoryProvider factoryProvider) {
    super(factoryProvider);
  }
  
  @Override
  public void postAppendCreateTableSql(StringBuilder builder, String tblComment, List<String> primaryKeys,
      Map<String, String> tblProperties) {
    builder.append("ENGINE=EXPRESS DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin");
    if (StringUtils.isNotBlank(tblComment)) {
      builder.append(String.format(" COMMENT='%s' ", tblComment.replace("'", "\\'")));
    }
  }
}
