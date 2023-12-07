// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.dbswitch.admin.handler.ListPatternHandler;
import com.gitee.dbswitch.admin.handler.ListTypeHandler;
import com.gitee.dbswitch.common.entity.PatternMapper;
import com.gitee.dbswitch.common.type.CaseConvertEnum;
import com.gitee.dbswitch.common.type.ProductTableEnum;
import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.EnumTypeHandler;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "DBSWITCH_ASSIGNMENT_CONFIG", autoResultMap = true)
public class AssignmentConfigEntity {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("assignment_id")
  private Long assignmentId;

  @TableField("source_connection_id")
  private Long sourceConnectionId;

  @TableField("source_schema")
  private String sourceSchema;

  @TableField(value = "table_type", typeHandler = EnumTypeHandler.class)
  private ProductTableEnum tableType;

  @TableField(value = "source_tables", typeHandler = ListTypeHandler.class)
  private List<String> sourceTables;

  @TableField("excluded")
  private Boolean excluded;

  @TableField("target_connection_id")
  private Long targetConnectionId;

  @TableField("target_schema")
  private String targetSchema;

  @TableField(value = "table_name_case", typeHandler = EnumTypeHandler.class)
  private CaseConvertEnum tableNameCase;

  @TableField(value = "column_name_case", typeHandler = EnumTypeHandler.class)
  private CaseConvertEnum columnNameCase;

  @TableField(value = "table_name_map", typeHandler = ListPatternHandler.class)
  private List<PatternMapper> tableNameMap;

  @TableField(value = "column_name_map", typeHandler = ListPatternHandler.class)
  private List<PatternMapper> columnNameMap;

  @TableField("target_drop_table")
  private Boolean targetDropTable;

  @TableField("target_only_create")
  private Boolean targetOnlyCreate;

  @TableField("target_auto_increment")
  private Boolean targetAutoIncrement;

  @TableField("batch_size")
  private Integer batchSize;

  @TableField("first_flag")
  private Boolean firstFlag;

  @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
  private Timestamp createTime;
}
