// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.dromara.dbswitch.admin.entity.JobLogbackEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface JobLogbackMapper extends BaseMapper<JobLogbackEntity> {

  @Delete("<script>"
      + "<if test='_databaseId == \"mysql\" '>"
      + "DELETE FROM DBSWITCH_JOB_LOGBACK WHERE date(create_time) &lt; DATE_SUB( CURDATE(), INTERVAL ${days} DAY )"
      + "</if>"
      + "<if test='_databaseId == \"postgresql\" '>"
      + "DELETE FROM DBSWITCH_JOB_LOGBACK WHERE create_time::date &lt; CURRENT_DATE - INTERVAL'${days} day'"
      + "</if>"
      + "</script>")
  void deleteByDays(@Param("days") Integer days);
}
