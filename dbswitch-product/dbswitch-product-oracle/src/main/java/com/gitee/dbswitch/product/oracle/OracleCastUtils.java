package com.gitee.dbswitch.product.oracle;

import com.gitee.dbswitch.common.util.ObjectCastUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.SqlTypeValue;

@Slf4j
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
    if (null == value) {
      return null;
    }

    if (jdbcType == Types.BLOB) {
      try {
        final byte[] bytes = ObjectCastUtils.castToByteArray(value);
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
      } catch (Exception e) {
        log.warn("Convert from {} to Oracle BLOB failed:{}", value.getClass().getName(), e.getMessage());
        return null;
      }
    }
    return ObjectCastUtils.castByJdbcType(jdbcType, value);
  }

}
