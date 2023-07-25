// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.gitee.dbswitch.common.event;

public interface TaskEventListener extends java.util.EventListener {

  /**
   * The event callback
   *
   * @param event object
   * @return event result
   */
  Object event(ListenedEvent event);
}