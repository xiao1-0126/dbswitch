// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.product.uxdb;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dbswitch.common.util.ObjectCastUtils;
import org.dromara.dbswitch.core.provider.ProductFactoryProvider;
import org.dromara.dbswitch.core.provider.write.DefaultTableDataWriteProvider;

@Slf4j
public class UxdbTableCopyWriteProvider extends DefaultTableDataWriteProvider {

  private static final int COPY_BUFFER_SIZE = 64 * 1024;
  private static final String COPY_NULL = "\\N";
  private static final DateTimeFormatter COPY_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter COPY_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
  private static final DateTimeFormatter COPY_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

  public UxdbTableCopyWriteProvider(ProductFactoryProvider factoryProvider) {
    super(factoryProvider);
  }

  @Override
  public long write(List<String> fieldNames, List<Object[]> recordValues) {
    if (recordValues.isEmpty()) {
      return 0;
    }
    if (fieldNames.isEmpty()) {
      throw new IllegalArgumentException("绗竴涓弬鏁癧fieldNames]涓虹┖,鏃犳晥!");
    }
    if (null == this.columnType || this.columnType.isEmpty()) {
      throw new RuntimeException("璇峰厛璋冪敤prepareWrite()鍑芥暟锛屾垨鑰呭嚭鐜板唴閮ㄤ唬鐮侀泦鎴愯皟鐢ㄩ敊璇紒");
    }

    try {
      return doCopyWrite(fieldNames, recordValues);
    } catch (Exception e) {
      log.warn("UXDB text copy failed for {}.{} , fallback to batch insert: {}",
          schemaName, tableName, e.getMessage(), e);
      return super.write(fieldNames, recordValues);
    }
  }

  private long doCopyWrite(List<String> fieldNames, List<Object[]> recordValues) throws Exception {
    String copySql = getCopyInSql(fieldNames);
    byte[] payload = buildCopyPayload(fieldNames, recordValues).getBytes(StandardCharsets.UTF_8);

    try (Connection connection = getDataSource().getConnection();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(payload)) {
      Object uxdbConnection = unwrapUxdbConnection(connection);
      Object copyManager = uxdbConnection.getClass().getMethod("getCopyAPI").invoke(uxdbConnection);
      Object result = copyManager.getClass()
          .getMethod("copyIn", String.class, java.io.InputStream.class, int.class)
          .invoke(copyManager, copySql, inputStream, COPY_BUFFER_SIZE);
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
      long written = ((Number) result).longValue();
      log.info("UXDB text copy succeeded for {}.{} rows={} bytes={} sql={}",
          schemaName, tableName, written, payload.length, copySql);
      return written;
    }
  }

  private Object unwrapUxdbConnection(Connection connection) throws Exception {
    try {
      return invokeGetCopyApiTarget(connection);
    } catch (NoSuchMethodException ignored) {
      // fall through and try standard JDBC unwrap.
    }

    ClassLoader loader = connection.getClass().getClassLoader();
    for (String className : new String[]{
        "com.uxsino.uxdb.core.BaseConnection",
        "com.uxsino.uxdb.UXConnection",
        "com.uxsino.uxdb.jdbc.UxConnection"
    }) {
      try {
        Class<?> uxdbClass = Class.forName(className, true, loader);
        if (connection.isWrapperFor(uxdbClass)) {
          Object unwrapped = connection.unwrap(uxdbClass);
          return unwrapCandidate(unwrapped);
        }
      } catch (ClassNotFoundException ignored) {
        // Try next UXDB connection class name.
      }
    }

    Object delegate = extractDelegateConnection(connection);
    if (delegate != null) {
      return unwrapCandidate(delegate);
    }

    throw new NoSuchMethodException(connection.getClass().getName() + ".getCopyAPI()");
  }

  private Object invokeGetCopyApiTarget(Object connection) throws Exception {
    connection.getClass().getMethod("getCopyAPI");
    return connection;
  }

  private Object extractDelegateConnection(Connection connection) {
    for (String methodName : new String[]{"getDelegate", "getTargetConnection", "getInnermostDelegate"}) {
      try {
        Object delegate = connection.getClass().getMethod(methodName).invoke(connection);
        if (delegate != null) {
          return delegate;
        }
      } catch (Exception ignored) {
        // Try next known proxy accessor.
      }
    }
    Class<?> current = connection.getClass();
    while (current != null) {
      try {
        Field field = current.getDeclaredField("delegate");
        field.setAccessible(true);
        Object delegate = field.get(connection);
        if (delegate != null) {
          return delegate;
        }
      } catch (Exception ignored) {
        // Try parent class.
      }
      current = current.getSuperclass();
    }
    return null;
  }

  private Object unwrapCandidate(Object candidate) throws Exception {
    try {
      return invokeGetCopyApiTarget(candidate);
    } catch (NoSuchMethodException ignored) {
      if (candidate instanceof Connection) {
        Object nested = extractDelegateConnection((Connection) candidate);
        if (nested != null && nested != candidate) {
          return unwrapCandidate(nested);
        }
      }
      throw ignored;
    }
  }

  private String getCopyInSql(List<String> fieldNames) {
    String fullTableName = quoteSchemaTableName(schemaName, tableName);
    String columns = fieldNames.stream()
        .map(this::quoteName)
        .collect(Collectors.joining(","));
    return String.format("COPY %s (%s) FROM STDIN", fullTableName, columns);
  }

  private String buildCopyPayload(List<String> fieldNames, List<Object[]> recordValues) {
    StringBuilder builder = new StringBuilder(recordValues.size() * Math.max(64, fieldNames.size() * 16));
    for (Object[] values : recordValues) {
      if (fieldNames.size() != values.length) {
        throw new RuntimeException(
            String.format("浼犲叆鐨勫弬鏁版湁璇紝瀛楁鍒楁暟%d涓庤褰曚腑鐨勫€间釜鏁?d涓嶇浉绗﹀悎", fieldNames.size(), values.length));
      }
      appendRecord(builder, fieldNames, values);
    }
    return builder.toString();
  }

  private void appendRecord(StringBuilder builder, List<String> fieldNames, Object[] values) {
    for (int i = 0; i < values.length; ++i) {
      String fieldName = fieldNames.get(i);
      Integer fieldType = this.columnType.get(fieldName);
      if (null == fieldType) {
        throw new RuntimeException(
            String.format("琛?s.%s 涓笉瀛樺湪瀛楁鍚嶄负%s鐨勫瓧娈碉紝璇锋鏌ュ弬鏁颁紶鍏?", schemaName, tableName, fieldName));
      }
      if (i > 0) {
        builder.append('\t');
      }
      builder.append(formatCopyValue(fieldName, fieldType, values[i]));
    }
    builder.append('\n');
  }

  private String formatCopyValue(String fieldName, int fieldType, Object fieldValue) {
    if (fieldValue == null) {
      return COPY_NULL;
    }
    switch (fieldType) {
      case Types.CHAR:
      case Types.NCHAR:
      case Types.VARCHAR:
      case Types.LONGVARCHAR:
      case Types.NVARCHAR:
      case Types.LONGNVARCHAR:
      case Types.CLOB:
      case Types.NCLOB:
      case Types.NULL:
      case Types.OTHER:
        return escapeCopyText(ObjectCastUtils.castToString(fieldValue));
      case Types.TINYINT:
        return String.valueOf(ObjectCastUtils.castToByte(fieldValue));
      case Types.SMALLINT:
        return String.valueOf(ObjectCastUtils.castToShort(fieldValue));
      case Types.INTEGER:
        return String.valueOf(ObjectCastUtils.castToInteger(fieldValue));
      case Types.BIGINT:
        return String.valueOf(ObjectCastUtils.castToLong(fieldValue));
      case Types.NUMERIC:
      case Types.DECIMAL:
        return String.valueOf(ObjectCastUtils.castToNumeric(fieldValue));
      case Types.FLOAT:
      case Types.REAL:
        return String.valueOf(ObjectCastUtils.castToFloat(fieldValue));
      case Types.DOUBLE:
        return String.valueOf(ObjectCastUtils.castToDouble(fieldValue));
      case Types.BOOLEAN:
        return Boolean.TRUE.equals(ObjectCastUtils.castToBoolean(fieldValue)) ? "t" : "f";
      case Types.BIT:
        return Boolean.TRUE.equals(ObjectCastUtils.castToBoolean(fieldValue)) ? "1" : "0";
      case Types.TIME:
        return COPY_TIME_FORMATTER.format(ObjectCastUtils.castToLocalTime(fieldValue));
      case Types.DATE:
        return COPY_DATE_FORMATTER.format(ObjectCastUtils.castToLocalDate(fieldValue));
      case Types.TIMESTAMP:
        return COPY_TIMESTAMP_FORMATTER.format(ObjectCastUtils.castToLocalDateTime(fieldValue));
      case Types.BINARY:
      case Types.VARBINARY:
      case Types.BLOB:
      case Types.LONGVARBINARY:
        return "\\\\x" + toHex(ObjectCastUtils.castToByteArray(fieldValue));
      default:
        throw new RuntimeException(
            String.format("涓嶆敮鎸佺殑鏁版嵁搴撳瓧娈电被鍨?琛ㄥ悕[%s.%s] 瀛楁鍚峓%s].", schemaName,
                tableName, fieldName));
    }
  }

  private String escapeCopyText(String value) {
    if (null == value) {
      return COPY_NULL;
    }
    StringBuilder builder = new StringBuilder(value.length() + 8);
    for (int i = 0; i < value.length(); ++i) {
      char c = value.charAt(i);
      switch (c) {
        case '\\':
          builder.append("\\\\");
          break;
        case '\t':
          builder.append("\\t");
          break;
        case '\n':
          builder.append("\\n");
          break;
        case '\r':
          builder.append("\\r");
          break;
        case '\b':
          builder.append("\\b");
          break;
        case '\f':
          builder.append("\\f");
          break;
        default:
          builder.append(c);
          break;
      }
    }
    return builder.toString();
  }

  private String toHex(byte[] bytes) {
    if (null == bytes || bytes.length == 0) {
      return StringUtils.EMPTY;
    }
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      int value = b & 0xFF;
      if (value < 0x10) {
        builder.append('0');
      }
      builder.append(Integer.toHexString(value));
    }
    return builder.toString();
  }
}
