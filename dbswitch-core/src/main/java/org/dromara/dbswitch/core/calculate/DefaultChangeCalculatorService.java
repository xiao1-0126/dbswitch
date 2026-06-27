// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.core.calculate;

import org.dromara.dbswitch.common.consts.Constants;
import org.dromara.dbswitch.common.entity.ResultSetWrapper;
import org.dromara.dbswitch.common.util.ExamineUtils;
import org.dromara.dbswitch.common.util.JdbcTypesUtils;
import org.dromara.dbswitch.common.util.ObjectCastUtils;
import org.dromara.dbswitch.core.provider.ProductProviderFactory;
import org.dromara.dbswitch.core.provider.query.TableDataQueryProvider;
import org.dromara.dbswitch.core.provider.transform.RecordTransformProvider;
import org.dromara.dbswitch.core.service.DefaultMetadataService;
import org.dromara.dbswitch.core.service.MetadataService;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

/**
 * 数据变化量计算核心类
 *
 * @author tang
 */
@Slf4j
public final class DefaultChangeCalculatorService implements RecordRowChangeCalculator {

  /**
   * 是否记录不变化的记录
   */
  private boolean recordIdentical;

  /**
   * 是否进行jdbc数据type检查
   */
  private boolean checkJdbcType;

  /**
   * 是否使用MD5比较（代替逐字段比较）
   */
  private java.util.concurrent.atomic.AtomicBoolean md5DiffLogged = new java.util.concurrent.atomic.AtomicBoolean(false);

  private boolean useMd5Compare = false;

  /**
   * 批量读取数据的行数大小
   */
  private int queryFetchSize;

  /**
   * 中断检查函数
   */
  private Runnable checkInterrupt;

  public DefaultChangeCalculatorService() {
    this(false, true);
  }

  public DefaultChangeCalculatorService(boolean recordIdentical, boolean checkJdbcType) {
    this.recordIdentical = recordIdentical;
    this.checkJdbcType = checkJdbcType;
    this.queryFetchSize = Constants.DEFAULT_FETCH_SIZE;
  }

  public boolean isUseMd5Compare() {
    return useMd5Compare;
  }

  public void setUseMd5Compare(boolean useMd5Compare) {
    this.useMd5Compare = useMd5Compare;
  }

  @Override
  public boolean isRecordIdentical() {
    return this.recordIdentical;
  }

  @Override
  public void setRecordIdentical(boolean recordOrNot) {
    this.recordIdentical = recordOrNot;
  }

  @Override
  public boolean isCheckJdbcType() {
    return this.checkJdbcType;
  }

  @Override
  public void setCheckJdbcType(boolean checkOrNot) {
    this.checkJdbcType = checkOrNot;
  }

  @Override
  public int getFetchSize() {
    return this.queryFetchSize;
  }

  @Override
  public void setFetchSize(int size) {
    ExamineUtils.check(size >= Constants.MINIMUM_FETCH_SIZE,
        "设置的批量处理行数的大小fetchSize不得小于%d",
        Constants.MINIMUM_FETCH_SIZE);
    this.queryFetchSize = size;
  }

  @Override
  public void setInterruptCheck(Runnable checkInterrupt) {
    this.checkInterrupt = checkInterrupt;
  }

  /**
   * 变化量计算函数
   * <p>
   * 说明 ： old 后缀的为目标段； new 后缀的为来源段；
   * <p>
   * 数据由 new 向 old 方向 同步。
   *
   * @param task    任务描述实体对象
   * @param handler 计算结果回调处理器
   */
  @Override
  public void executeCalculate(TaskParamEntity task, RecordRowHandler handler) {
    ExamineUtils.checkNotNull(task, "task");
    ExamineUtils.checkNotNull(handler, "handler");

    this.md5DiffLogged.set(false);

    if (log.isDebugEnabled()) {
      log.debug("###### Begin execute calculate table CDC data now");
    }

    Map<String, String> columnsMap = task.getColumnsMap();
    boolean useOwnFieldsColumns = !CollectionUtils.isEmpty(task.getFieldColumns());

    // 检查新旧两张表的主键字段与比较字段
    MetadataService oldMd = new DefaultMetadataService(task.getOldDataSource(), task.getOldProductType());
    MetadataService newMd = new DefaultMetadataService(task.getNewDataSource(), task.getNewProductType());
    List<String> fieldsPrimaryKeyOld = oldMd
        .queryTablePrimaryKeys(task.getOldSchemaName(), task.getOldTableName());
    List<String> fieldsAllColumnOld = oldMd
        .queryTableColumnName(task.getOldSchemaName(), task.getOldTableName());
    List<String> fieldsPrimaryKeyNew = newMd
        .queryTablePrimaryKeys(task.getNewSchemaName(), task.getNewTableName());
    List<String> fieldsAllColumnNew = newMd
        .queryTableColumnName(task.getNewSchemaName(), task.getNewTableName());
    List<String> fieldsMappedPrimaryKeyNew = fieldsPrimaryKeyNew.stream()
        .map(s -> columnsMap.getOrDefault(s, s))
        .collect(Collectors.toList());
    List<String> fieldsMappedAllColumnNew = fieldsAllColumnNew.stream()
        .map(s -> columnsMap.getOrDefault(s, s))
        .collect(Collectors.toList());

    boolean noPk = fieldsPrimaryKeyOld.isEmpty() || fieldsPrimaryKeyNew.isEmpty();
    if (noPk) {
      log.warn("表 [{}] 没有主键，使用全部列作为伪主键进行CDC比对", task.getOldTableName());
    } else {
      ExamineUtils.check(
          isListEqual(fieldsPrimaryKeyOld, fieldsMappedPrimaryKeyNew),
          "两个表的主键映射关系不匹配"
      );
    }

    if (useOwnFieldsColumns) {
      // 如果自己配置了字段列表，判断子集关系
      List<String> mappedFieldColumns = task.getFieldColumns().stream()
          .map(s -> columnsMap.getOrDefault(s, s))
          .collect(Collectors.toList());
      if (!fieldsAllColumnNew.containsAll(task.getFieldColumns())
          || !fieldsAllColumnOld.containsAll(mappedFieldColumns)) {
        throw new RuntimeException("指定的字段列不完全在两个表中存在");
      }
      if (!noPk) {
        boolean same = (mappedFieldColumns.containsAll(fieldsPrimaryKeyOld)
            && task.getFieldColumns().containsAll(fieldsPrimaryKeyNew));
        if (!same) {
          throw new RuntimeException("提供的比较字段中未包含主键");
        }
      }
      boolean same = (fieldsAllColumnOld.containsAll(mappedFieldColumns)
          && fieldsAllColumnNew.containsAll(task.getFieldColumns()));
      if (!same) {
        throw new RuntimeException("提供的比较字段中存在表中不存在(映射关系对不上)的字段");
      }
    } else {
      boolean same = (fieldsMappedAllColumnNew.containsAll(fieldsPrimaryKeyOld)
          && fieldsAllColumnOld.containsAll(fieldsMappedAllColumnNew));
      if (!same) {
        throw new RuntimeException("两个表的字段映射关系不匹配");
      }
    }

    // 计算除主键外的比较字段
    List<String> fieldsOfCompareValue = new ArrayList<>();
    if (!noPk) {
      if (useOwnFieldsColumns) {
        fieldsOfCompareValue.addAll(task.getFieldColumns());
      } else {
        fieldsOfCompareValue.addAll(fieldsAllColumnNew);
      }
      fieldsOfCompareValue.removeAll(fieldsPrimaryKeyNew);
    }

    // 构造查询列字段
    List<String> queryFieldColumn;
    List<String> mappedQueryFieldColumn;
    if (useOwnFieldsColumns) {
      queryFieldColumn = task.getFieldColumns();
    } else {
      queryFieldColumn = fieldsAllColumnOld;
    }
    mappedQueryFieldColumn = queryFieldColumn.stream()
        .map(s -> columnsMap.getOrDefault(s, s))
        .collect(Collectors.toList());

    ResultSetWrapper rsold = null;
    ResultSetWrapper rsnew = null;

    try {
      // 提取新旧两表数据的结果集
      TableDataQueryProvider oldQuery = ProductProviderFactory
          .newProvider(task.getOldProductType(), task.getOldDataSource())
          .createTableDataQueryProvider();
      oldQuery.setQueryFetchSize(this.queryFetchSize);
      TableDataQueryProvider newQuery = ProductProviderFactory
          .newProvider(task.getNewProductType(), task.getNewDataSource())
          .createTableDataQueryProvider();
      newQuery.setQueryFetchSize(this.queryFetchSize);

      if (log.isDebugEnabled()) {
        log.debug("###### Query data from two table now");
      }
      Optional.ofNullable(checkInterrupt).ifPresent(Runnable::run);

      rsold = oldQuery
          .queryTableData(task.getOldSchemaName(), task.getOldTableName(),
              mappedQueryFieldColumn, fieldsMappedPrimaryKeyNew);
      Optional.ofNullable(checkInterrupt).ifPresent(Runnable::run);

      rsnew = newQuery
          .queryTableData(task.getNewSchemaName(), task.getNewTableName(),
              queryFieldColumn, fieldsPrimaryKeyNew);
      Optional.ofNullable(checkInterrupt).ifPresent(Runnable::run);

      ResultSetMetaData metaData = rsnew.getResultSet().getMetaData();

      if (log.isDebugEnabled()) {
        log.debug("###### Check data validate now");
      }

      // 检查结果集列数一致
      int oldcnt = rsold.getResultSet().getMetaData().getColumnCount();
      int newcnt = rsnew.getResultSet().getMetaData().getColumnCount();
      if (oldcnt != newcnt) {
        throw new RuntimeException(String.format("两个表的字段总个数不相等，即：%d!=%d", oldcnt, newcnt));
      }

      // 计算主键字段序列在结果集中的索引号（无主键表使用全部列作为伪主键）
      int[] keyNumbers;
      if (noPk) {
        keyNumbers = new int[metaData.getColumnCount()];
        for (int i = 0; i < keyNumbers.length; ++i) {
          keyNumbers[i] = i;
        }
      } else {
        keyNumbers = new int[fieldsPrimaryKeyNew.size()];
        for (int i = 0; i < keyNumbers.length; ++i) {
          String fn = fieldsPrimaryKeyNew.get(i);
          keyNumbers[i] = getIndexOfField(fn, metaData);
        }
      }

      // 计算比较(非主键)字段序列在结果集中的索引号
      int[] valNumbers = new int[fieldsOfCompareValue.size()];
      for (int i = 0; i < valNumbers.length; ++i) {
        String fn = fieldsOfCompareValue.get(i);
        valNumbers[i] = getIndexOfField(fn, metaData);
      }

      // 初始化计算结果数据字段列信息
      int[] jdbcTypes = new int[metaData.getColumnCount()];
      List<String> targetColumns = new ArrayList<>();
      for (int k = 1; k <= metaData.getColumnCount(); ++k) {
        String key = metaData.getColumnLabel(k);
        if (null == key) {
          key = metaData.getColumnName(k);
        }
        targetColumns.add(columnsMap.getOrDefault(key, key));
        jdbcTypes[k - 1] = metaData.getColumnType(k);
      }
      Optional.ofNullable(checkInterrupt).ifPresent(Runnable::run);
      log.info("[CDC-v5] HashMap-based CDC, noPk={}, useMd5Compare={}", noPk, useMd5Compare);

      RecordTransformProvider transformer = task.getTransformer();

      // === HashMap-based CDC (avoids sort-order issues with cross-DB collation) ===
      java.util.LinkedHashMap<String, Object[]> targetMap = new java.util.LinkedHashMap<>();
      Object[] tr;
      while ((tr = getRowData(rsnew.getResultSet())) != null) {
        tr = transformer.doTransform(task.getNewSchemaName(), task.getNewTableName(),
            queryFieldColumn, tr);
        String key = buildPkKey(tr, keyNumbers, metaData);
        targetMap.put(key, tr);
      }

      int srcTotal = 0, insertCnt = 0, updateCnt = 0, deleteCnt = 0;
       java.util.HashSet<String> matchedKeys = new java.util.HashSet<>();
       Object[] sr;
       while ((sr = getRowData(rsold.getResultSet())) != null) {
        srcTotal++;
         Optional.ofNullable(checkInterrupt).ifPresent(Runnable::run);
         // Transform source row BEFORE comparison so PK key and value comparison
         // use the same post-transform data as target rows (stored transformed in map)
         Object[] srTransformed = transformer.doTransform(task.getNewSchemaName(),
             task.getNewTableName(), queryFieldColumn, sr);
         String key = buildPkKey(srTransformed, keyNumbers, metaData);
         Object[] matched = targetMap.get(key);
 
         if (matched == null) {
            insertCnt++;
             log.info("INSERT: src_key=[{}], target_map_size={}", key.substring(0, Math.min(80, key.length())), targetMap.size());
           if (!recordIdentical) {
             handler.handle(Collections.unmodifiableList(targetColumns), srTransformed, jdbcTypes,
                 RowChangeTypeEnum.VALUE_INSERT);
           }
         } else {
           matchedKeys.add(key);
           int cmp = useMd5Compare
               ? md5Compare(srTransformed, matched, valNumbers, metaData)
               : this.compare(srTransformed, matched, valNumbers, metaData);
           if (cmp != 0) {
            updateCnt++;
             log.info("VALUE_CHANGED: src_key=[{}]", key.substring(0, Math.min(80, key.length())));
             handler.handle(Collections.unmodifiableList(targetColumns), srTransformed, jdbcTypes,
                 RowChangeTypeEnum.VALUE_CHANGED);
           }
         }
       }
 
      // 未匹配的目标行 → Delete
       for (java.util.Map.Entry<String, Object[]> entry : targetMap.entrySet()) {
         if (!matchedKeys.contains(entry.getKey())) {
          deleteCnt++;
           handler.handle(Collections.unmodifiableList(targetColumns), entry.getValue(), jdbcTypes,
               RowChangeTypeEnum.VALUE_DELETED);
         }
       }

      log.info("[CDC-v5] {}: src={}, tgt={}, ins={}, upd={}, del={}",
          task.getOldTableName(), srcTotal, targetMap.size(), insertCnt, updateCnt, deleteCnt);

    } catch (SQLException e) {
      throw new RuntimeException(e);
    } finally {
      if (null != rsold) {
        rsold.close();
      }
      if (null != rsnew) {
        rsnew.close();
      }
    }

  }

  private boolean isListEqual(List<String> left, List<String> right) {
    return left.containsAll(right) && right.containsAll(left);
  }

  /**
   * 获取字段的索引号
   *
   * @param key      字段名
   * @param metaData 结果集的元信息
   * @return 字段的索引号
   * @throws SQLException
   */
  private int getIndexOfField(String key, ResultSetMetaData metaData) throws SQLException {
    for (int k = 1; k <= metaData.getColumnCount(); ++k) {
      String fieldName = metaData.getColumnLabel(k);
      if (null == fieldName) {
        fieldName = metaData.getColumnName(k);
      }

      if (fieldName.equals(key)) {
        return k - 1;
      }
    }

    return -1;
  }

  /**
   * 记录比较
   *
   * @param obj1     记录1
   * @param obj2     记录2
   * @param fieldnrs 待比较的字段索引号
   * @param metaData 记录集的元信息
   * @return 比较的结果：0，-1，1
   * @throws SQLException
   */
  private int compare(Object[] obj1, Object[] obj2, int[] fieldnrs, ResultSetMetaData metaData)
      throws SQLException {
    if (obj1.length != obj2.length) {
      throw new RuntimeException("Invalid compare object list !");
    }

    for (int nr : fieldnrs) {
      int jdbcType = metaData.getColumnType(nr + 1);
      Object o1 = obj1[nr];
      Object o2 = obj2[nr];

      int cmp = typeCompare(jdbcType, o1, o2);
      if (cmp != 0) {
        return cmp;
      }
    }

    return 0;
  }

  /**
   * 字段值对象比较，将对象转换为字节数组来比较实现
   *
   * @param type 字段的JDBC数据类型
   * @param o1   对象1
   * @param o2   对象2
   * @return 0为相等，-1为小于，1为大于
   */
  private int typeCompare(int type, Object o1, Object o2) {
    boolean n1 = (o1 == null);
    boolean n2 = (o2 == null);
    if (n1 && !n2) {
      return -1;
    }
    if (!n1 && n2) {
      return 1;
    }
    if (n1 && n2) {
      return 0;
    }

    /**
     * <p>
     * 这里要比较的两个对象o1与o2可能类型不同，但值相同，例如： Integer o1=12，Long o2=12;
     * </p>
     * <p>
     * 但是这种不属于同一类的比较情况不应出现: String o1="12", Integer o2=12;
     * </p>
     */
    if (JdbcTypesUtils.isString(type)) {
      String s1 = normalizeForMd5(String.valueOf(o1));
      String s2 = normalizeForMd5(String.valueOf(o2));
      return s1.compareTo(s2);
    } else if (JdbcTypesUtils.isNumeric(type) && o1 instanceof java.lang.Number
        && o2 instanceof java.lang.Number) {
      java.lang.Number s1 = (java.lang.Number) o1;
      java.lang.Number s2 = (java.lang.Number) o2;
      return Double.compare(s1.doubleValue(), s2.doubleValue());
    } else if (JdbcTypesUtils.isInteger(type) && o1 instanceof java.lang.Number
        && o2 instanceof java.lang.Number) {
      java.lang.Number s1 = (java.lang.Number) o1;
      java.lang.Number s2 = (java.lang.Number) o2;
      return Long.compare(s1.longValue(), s2.longValue());
    } else if (JdbcTypesUtils.isDateTime(type)) {
      if (o1 instanceof LocalTime) {
        o1 = java.sql.Time.valueOf((LocalTime) o1);
      }
      if (o1 instanceof LocalDate) {
        o1 = java.sql.Date.valueOf((LocalDate) o1);
      }
      if (o1 instanceof LocalDateTime) {
        o1 = java.sql.Timestamp.valueOf((LocalDateTime) o1);
      }
      if (o2 instanceof LocalTime) {
        o2 = java.sql.Time.valueOf((LocalTime) o2);
      }
      if (o2 instanceof LocalDate) {
        o2 = java.sql.Date.valueOf((LocalDate) o2);
      }
      if (o2 instanceof LocalDateTime) {
        o2 = java.sql.Timestamp.valueOf((LocalDateTime) o2);
      }
      if (o1 instanceof java.sql.Time && o2 instanceof java.sql.Time) {
        java.sql.Time t1 = (java.sql.Time) o1;
        java.sql.Time t2 = (java.sql.Time) o2;
        return t1.compareTo(t2);
      } else if (o1 instanceof java.sql.Timestamp && o2 instanceof java.sql.Timestamp) {
        java.sql.Timestamp t1 = (java.sql.Timestamp) o1;
        java.sql.Timestamp t2 = (java.sql.Timestamp) o2;
        return t1.compareTo(t2);
      } else if (o1 instanceof java.sql.Date && o2 instanceof java.sql.Date) {
        java.sql.Date t1 = (java.sql.Date) o1;
        java.sql.Date t2 = (java.sql.Date) o2;
        return t1.compareTo(t2);
      } else {
        String s1 = String.valueOf(o1);
        String s2 = String.valueOf(o2);
        return s1.compareTo(s2);
      }
    } else {
      String s1 = normalizeForMd5(o1);
      String s2 = normalizeForMd5(o2);
      if (!s1.equals(s2)) {
        log.info("UDT compare diff: type={}, o1.class={}, o2.class={}, s1=[{}], s2=[{}]",
            type, o1 == null ? "null" : o1.getClass().getSimpleName(),
            o2 == null ? "null" : o2.getClass().getSimpleName(), s1, s2);
      }
      return s1.compareTo(s2);
    }
  }

  /**
   * 使用MD5 hash比较两行数据是否相等（仅判断相等/不等，不排序）
   *
   * @param obj1     记录1
   * @param obj2     记录2
   * @param fieldnrs 待比较的非主键字段索引号
   * @param metaData 记录集的元信息
   * @return 0为相等，非0为不等
   */
  private int md5Compare(Object[] obj1, Object[] obj2, int[] fieldnrs, ResultSetMetaData metaData) {
    try {
      java.security.MessageDigest md5_1 = java.security.MessageDigest.getInstance("MD5");
      java.security.MessageDigest md5_2 = java.security.MessageDigest.getInstance("MD5");
      for (int nr : fieldnrs) {
        int jdbcType;
        try { jdbcType = metaData.getColumnType(nr + 1); } catch (Exception e) { jdbcType = java.sql.Types.OTHER; }
        String s1 = normalizeForMd5(obj1[nr], jdbcType);
        String s2 = normalizeForMd5(obj2[nr], jdbcType);
        md5_1.update(s1.getBytes("UTF-8"));
        md5_2.update(s2.getBytes("UTF-8"));
        md5_1.update((byte) 0);
        md5_2.update((byte) 0);
      }
      int cmp = compareTo(md5_1.digest(), md5_2.digest());
      if (cmp != 0) {
        for (int nr : fieldnrs) {
          int jdbcType;
          try { jdbcType = metaData.getColumnType(nr + 1); } catch (Exception e) { jdbcType = java.sql.Types.OTHER; }
          String s1 = normalizeForMd5(obj1[nr], jdbcType);
          String s2 = normalizeForMd5(obj2[nr], jdbcType);
          if (!s1.equals(s2)) {
            String cn;
            try { cn = metaData.getColumnLabel(nr + 1); } catch (Exception e) { cn = "col#" + nr; }
            log.info("MD5 diff field [{}]: jdbcType={}, t1={}, t2={}, v1=[{}], v2=[{}]",
                cn, jdbcType,
                obj1[nr] == null ? "null" : obj1[nr].getClass().getSimpleName(),
                obj2[nr] == null ? "null" : obj2[nr].getClass().getSimpleName(),
                s1.substring(0, Math.min(80, s1.length())),
                s2.substring(0, Math.min(80, s2.length())));
            break;
          }
        }
      }
      return cmp;
    } catch (Exception e) {
      log.warn("MD5 compare failed: {}", e.getMessage());
      return -1;
    }
  }

  private String buildPkKey(Object[] row, int[] keyNumbers, ResultSetMetaData metaData) {
    StringBuilder sb = new StringBuilder();
    for (int nr : keyNumbers) {
      try {
        int jdbcType = metaData.getColumnType(nr + 1);
        sb.append(normalizeForMd5(row[nr], jdbcType));
      } catch (Exception e) {
        sb.append(normalizeForMd5(row[nr]));
      }
      sb.append('\0');
    }
    return sb.toString();
  }

  /**
   * 按JDBC列类型归一化字段值，消除不同JDBC驱动返回Java类型差异导致的MD5不一致。
   */

  public static String normalizeForMd5(Object o) {
    if (o == null) return "";
    if (o instanceof Boolean) return ((Boolean) o) ? "1" : "0";
    if (o instanceof java.math.BigDecimal) return ((java.math.BigDecimal) o).stripTrailingZeros().toPlainString();
    if (o instanceof java.lang.Double) return new java.math.BigDecimal(o.toString()).stripTrailingZeros().toPlainString();
    if (o instanceof java.lang.Float) return new java.math.BigDecimal(o.toString()).stripTrailingZeros().toPlainString();
    if (o instanceof Number) return new java.math.BigDecimal(o.toString()).stripTrailingZeros().toPlainString();
    if (o instanceof java.time.LocalDateTime) {
      return String.valueOf(java.sql.Timestamp.valueOf((java.time.LocalDateTime) o).getTime() / 1000);
    }
    if (o instanceof java.time.LocalDate) {
      return String.valueOf(java.sql.Date.valueOf((java.time.LocalDate) o).getTime() / 1000);
    }
    if (o instanceof java.time.LocalTime) {
      return String.valueOf(java.sql.Time.valueOf((java.time.LocalTime) o).getTime() / 1000);
    }
    if (o instanceof java.util.Date) return String.valueOf(((java.util.Date) o).getTime() / 1000);
    if (o instanceof java.sql.Clob) try { java.sql.Clob c = (java.sql.Clob) o; return c.getSubString(1, (int) c.length()); } catch (Exception e) { return ""; }
    if (o instanceof java.sql.NClob) try { java.sql.NClob c = (java.sql.NClob) o; return c.getSubString(1, (int) c.length()); } catch (Exception e) { return ""; }
    if (o instanceof byte[]) {
      StringBuilder sb = new StringBuilder();
      for (byte b : (byte[]) o) sb.append(String.format("%02x", b));
      return sb.toString();
    }
    return java.text.Normalizer.normalize(String.valueOf(o).trim(), java.text.Normalizer.Form.NFC).toLowerCase();
  }
  public static String normalizeForMd5(Object o, int jdbcType) {
    if (o == null) return "";
    
    // Boolean / Bit → "0" or "1" regardless of driver Java type (Boolean, Integer, byte[], etc.)
    if (org.dromara.dbswitch.common.util.JdbcTypesUtils.isBoolean(jdbcType)
        || org.dromara.dbswitch.common.util.JdbcTypesUtils.isInteger(jdbcType) && jdbcType == java.sql.Types.BIT) {
      if (o instanceof Boolean) return ((Boolean) o) ? "1" : "0";
      if (o instanceof Number) return ((Number) o).intValue() != 0 ? "1" : "0";
      if (o instanceof byte[]) return ((byte[]) o).length > 0 && ((byte[]) o)[0] != 0 ? "1" : "0";
      return "0";
    }
    
    // Integer types → BigDecimal plain string (handles Short, Integer, Long, BigInteger uniformly)
    if (org.dromara.dbswitch.common.util.JdbcTypesUtils.isInteger(jdbcType)) {
      if (o instanceof Number) return new java.math.BigDecimal(o.toString()).stripTrailingZeros().toPlainString();
      if (o instanceof Boolean) return ((Boolean) o) ? "1" : "0";
      return String.valueOf(o).trim();
    }
    
    // Numeric types → BigDecimal plain string (handles BigDecimal, Double, Float uniformly)
    if (org.dromara.dbswitch.common.util.JdbcTypesUtils.isNumeric(jdbcType)) {
      if (o instanceof Number) return new java.math.BigDecimal(o.toString()).stripTrailingZeros().toPlainString();
      return String.valueOf(o).trim();
    }
    
    // DateTime types → epoch seconds (handles Timestamp, Date, Time, LocalDateTime, LocalDate, LocalTime)
    if (org.dromara.dbswitch.common.util.JdbcTypesUtils.isDateTime(jdbcType)) {
      long epoch;
      if (o instanceof java.time.LocalDateTime) {
        epoch = java.sql.Timestamp.valueOf((java.time.LocalDateTime) o).getTime() / 1000;
      } else if (o instanceof java.time.LocalDate) {
        epoch = java.sql.Date.valueOf((java.time.LocalDate) o).getTime() / 1000;
      } else if (o instanceof java.time.LocalTime) {
        epoch = java.sql.Time.valueOf((java.time.LocalTime) o).getTime() / 1000;
      } else if (o instanceof java.util.Date) {
        epoch = ((java.util.Date) o).getTime() / 1000;
      } else {
        return String.valueOf(o).trim().toLowerCase();
      }
      return String.valueOf(epoch);
    }
    
    // Binary types → hex string (handles byte[], Blob uniformly)
    if (org.dromara.dbswitch.common.util.JdbcTypesUtils.isBinary(jdbcType)) {
      byte[] bytes;
      if (o instanceof byte[]) {
        bytes = (byte[]) o;
      } else if (o instanceof java.sql.Blob) {
        try {
          java.sql.Blob blob = (java.sql.Blob) o;
          bytes = blob.getBytes(1, (int) blob.length());
        } catch (Exception e) {
          return "";
        }
      } else {
        return String.valueOf(o).trim().toLowerCase();
      }
      StringBuilder sb = new StringBuilder();
      for (byte b : bytes) sb.append(String.format("%02x", b));
      return sb.toString();
    }
    
    // String types → trimmed + lowercased + NFC normalized
    if (org.dromara.dbswitch.common.util.JdbcTypesUtils.isString(jdbcType)) {
      if (o instanceof java.sql.Clob) try { java.sql.Clob c = (java.sql.Clob) o; return java.text.Normalizer.normalize(c.getSubString(1, (int) c.length()).trim(), java.text.Normalizer.Form.NFC).toLowerCase(); } catch (Exception e) { return ""; }
      if (o instanceof java.sql.NClob) try { java.sql.NClob c = (java.sql.NClob) o; return java.text.Normalizer.normalize(c.getSubString(1, (int) c.length()).trim(), java.text.Normalizer.Form.NFC).toLowerCase(); } catch (Exception e) { return ""; }
      return java.text.Normalizer.normalize(String.valueOf(o).trim(), java.text.Normalizer.Form.NFC).toLowerCase();
    }
    
    // Fallback: use the object-type-based version
    return normalizeForMd5(o);
  }

  /**
   * 字节数组的比较
   *
   * @param s1 字节数组1
   * @param s2 字节数组2
   * @return 0为相等，-1为小于，1为大于
   */
  public int compareTo(byte[] s1, byte[] s2) {
    int len1 = s1.length;
    int len2 = s2.length;
    int lim = Math.min(len1, len2);
    byte[] v1 = s1;
    byte[] v2 = s2;

    int k = 0;
    while (k < lim) {
      byte c1 = v1[k];
      byte c2 = v2[k];
      if (c1 != c2) {
        return c1 - c2;
      }
      k++;
    }
    return len1 - len2;
  }

  /**
   * 从结果集中取出一条记录
   *
   * @param rs 记录集
   * @return 一条记录，到记录结尾时返回null
   * @throws SQLException
   */
  private Object[] getRowData(ResultSet rs) throws SQLException {
    ResultSetMetaData metaData = rs.getMetaData();
    Object[] rowData = null;

    if (rs.next()) {
      rowData = new Object[metaData.getColumnCount()];
      for (int j = 1; j <= metaData.getColumnCount(); ++j) {
        rowData[j - 1] = rs.getObject(j);
      }
    }

    return rowData;
  }

}
