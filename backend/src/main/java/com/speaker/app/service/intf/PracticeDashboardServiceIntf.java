package com.speaker.app.service.intf;

import com.speaker.app.dto.PracticeDashboardDto;

/** 练习数据看板：与对练会话流程解耦，仅做统计聚合。 */
public interface PracticeDashboardServiceIntf {

    PracticeDashboardDto getDashboardStats();
}
