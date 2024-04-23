// Copyright tang.  All rights reserved.
// https://gitee.com/benbenyezi/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: Li Zemin
// Date : 2024/4/23 16:12
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.admin.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.alibaba.excel.EasyExcel;
import com.gitee.dbswitch.admin.common.exception.DbswitchException;
import com.gitee.dbswitch.admin.common.response.ResultCode;

/**
 * excel工具类
 *
 * @author Li Zemin
 * @since 2024/4/23 16:12
 */
public final class EasyexcelUtils {

	public static <T> void write(HttpServletResponse response, Class<T> clazz, List<T> list, String fileName,
	                             String sheetName) {

		try {
			// 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
			// response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			response.setContentType("application/vnd.ms-excel");
			response.setCharacterEncoding("utf-8");
			// 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
			fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
			// response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
			EasyExcel.write(response.getOutputStream(), clazz)
					.sheet(sheetName)
					.doWrite(list);
		} catch (IOException ex) {
			throw new DbswitchException(ResultCode.ERROR_INTERNAL_ERROR, ex.getMessage());
		}

	}
}
