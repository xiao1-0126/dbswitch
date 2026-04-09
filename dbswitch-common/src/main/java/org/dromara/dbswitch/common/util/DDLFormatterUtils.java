// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.common.util;

import java.util.Locale;
import java.util.StringTokenizer;
import lombok.experimental.UtilityClass;

/**
 * DDL的SQL语句格式化(摘自hibernate)
 *
 * @author tang
 */
@UtilityClass
public class DDLFormatterUtils {

  public static String format(String sql) {
    if (null == sql || 0 == sql.length()) {
      return sql;
    }
    if (sql.toLowerCase(Locale.ROOT).startsWith("create table")) {
      return formatCreateTable(sql);
    } else if (sql.toLowerCase(Locale.ROOT).startsWith("alter table")) {
      return formatAlterTable(sql);
    } else if (sql.toLowerCase(Locale.ROOT).startsWith("comment on")) {
      return formatCommentOn(sql);
    } else {
      return "\n    " + sql;
    }
  }

  private static String formatCommentOn(String sql) {
    final StringBuilder result = new StringBuilder(60).append("    ");
    final StringTokenizer tokens = new StringTokenizer(sql, " '[]\"", true);

    boolean quoted = false;
    while (tokens.hasMoreTokens()) {
      final String token = tokens.nextToken();
      result.append(token);
      if (isQuote(token)) {
        quoted = !quoted;
      } else if (!quoted) {
        if ("is".equals(token)) {
          result.append("\n       ");
        }
      }
    }

    return result.toString();
  }

  private static String formatAlterTable(String sql) {
    final StringBuilder result = new StringBuilder(60).append("    ");
    final StringTokenizer tokens = new StringTokenizer(sql, " (,)'[]\"", true);

    boolean quoted = false;
    while (tokens.hasMoreTokens()) {
      final String token = tokens.nextToken();
      if (isQuote(token)) {
        quoted = !quoted;
      } else if (!quoted) {
        if (isBreak(token)) {
          result.append("\n        ");
        }
      }
      result.append(token);
    }

    return result.toString();
  }

  private static String formatCreateTable(String sql) {
    final StringBuilder result = new StringBuilder(60).append("    ");
    final StringTokenizer tokens = new StringTokenizer(sql, "(,)'[]\" \t\n\r\f", true);

    int depth = 0;
    boolean quoted = false;
    boolean afterOpenParenOrComma = false;
    while (tokens.hasMoreTokens()) {
      final String token = tokens.nextToken();
      if (isQuote(token)) {
        quoted = !quoted;
        afterOpenParenOrComma = false;
        result.append(token);
      } else if (quoted) {
        afterOpenParenOrComma = false;
        result.append(token);
      } else if (isWhitespace(token)) {
        if (afterOpenParenOrComma) {
          // skip whitespace after '(' or ',' at depth 1 (already added newline+indent)
          continue;
        }
        if (depth <= 1) {
          result.append(' ');
        } else {
          result.append(token);
        }
      } else {
        afterOpenParenOrComma = false;
        if (")".equals(token)) {
          depth--;
          if (depth == 0) {
            result.append("\n    ");
          }
        }
        result.append(token);
        if (",".equals(token) && depth == 1) {
          result.append("\n       ");
          afterOpenParenOrComma = true;
        }
        if ("(".equals(token)) {
          depth++;
          if (depth == 1) {
            result.append("\n       ");
            afterOpenParenOrComma = true;
          }
        }
      }
    }

    return result.toString();
  }

  private static boolean isBreak(String token) {
    return "drop".equals(token) ||
        "add".equals(token) ||
        "references".equals(token) ||
        "foreign".equals(token) ||
        "on".equals(token);
  }

  private static boolean isWhitespace(String token) {
    return " ".equals(token) ||
        "\t".equals(token) ||
        "\n".equals(token) ||
        "\r".equals(token) ||
        "\f".equals(token);
  }

  private static boolean isQuote(String tok) {
    return "\"".equals(tok) ||
        "`".equals(tok) ||
        "]".equals(tok) ||
        "[".equals(tok) ||
        "'".equals(tok);
  }

}
