// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2026/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.admin.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;
import org.dromara.dbswitch.common.type.CaseConvertEnum;

@Data
@ApiModel("单表DDL预览请求")
public class DdlSinglePreviewRequest {

  @ApiModelProperty(value = "源端连接ID", required = true)
  private Long sourceConnectionId;

  @ApiModelProperty(value = "源端Schema", required = true)
  private String sourceSchema;

  @ApiModelProperty(value = "目标端连接ID", required = true)
  private Long targetConnectionId;

  @ApiModelProperty(value = "目标端Schema", required = true)
  private String targetSchema;

  @ApiModelProperty(value = "需要预览的单张源表名", required = true)
  private String sourceTable;

  @ApiModelProperty(value = "表名映射规则")
  private List<org.dromara.dbswitch.common.entity.PatternMapper> tableNameMapper;

  @ApiModelProperty(value = "字段名映射规则")
  private List<org.dromara.dbswitch.common.entity.PatternMapper> columnNameMapper;

  @ApiModelProperty(value = "表名大小写转换")
  private CaseConvertEnum tableNameCase = CaseConvertEnum.NONE;

  @ApiModelProperty(value = "列名大小写转换")
  private CaseConvertEnum columnNameCase = CaseConvertEnum.NONE;

  @ApiModelProperty(value = "是否建表自增")
  private Boolean targetAutoIncrement = false;
}
