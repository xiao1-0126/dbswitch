package com.gitee.dbswitch.product.oracle;

import com.gitee.dbswitch.common.util.ObjectCastUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.springframework.jdbc.core.SqlTypeValue;

@UtilityClass
public class OracleCastUtils {

  /**
   * 将java.sql.Array 类型转换为java.lang.String
   * <p>
   * Oracle 没有数组类型，这里以文本类型进行存在
   * <p>
   * Oracle的CLOB和BLOB类型写入请见：
   * <p>
   * oracle.jdbc.driver.OraclePreparedStatement.setObjectCritical
   */
  public static Object castByJdbcType(int jdbcType, Object value, List<InputStream> iss) {
    try {
      switch (jdbcType) {
        case Types.CLOB:
        case Types.NCLOB:
          return Objects.isNull(value)
              ? null
              : ObjectCastUtils.castToString(value);
        case Types.BLOB:
          final byte[] bytes = Objects.isNull(value)
              ? null
              : ObjectCastUtils.castToByteArray(value);
          return new SqlTypeValue() {
            @Override
            public void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType,
                String typeName) throws SQLException {
              if (null != bytes) {
                InputStream is = new ByteArrayInputStream(bytes);
                ps.setBlob(paramIndex, is);
                iss.add(is);
              } else {
                ps.setNull(paramIndex, sqlType);
              }
            }
          };
        case Types.ROWID:
        case Types.ARRAY:
        case Types.REF:
        case Types.SQLXML:
        default:
          return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

}
