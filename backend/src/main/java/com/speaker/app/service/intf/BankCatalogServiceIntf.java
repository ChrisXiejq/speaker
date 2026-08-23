package com.speaker.app.service.intf;

import com.speaker.app.dto.BankTopicGroupResponse;

import java.util.List;

public interface BankCatalogServiceIntf {

    /**
     * 与 GET /api/bank/seasons 一致：未删除季节标签，首项为当前默认季。
     */
    List<String> listSeasonsForApi();

    /** 未删除的 distinct 季节标签（不做排序），供解析默认季等逻辑使用。 */
    List<String> getDistinctSeasonLabels();

    /**
     * 按季节 + Part 聚合题目（PART1 或 PART2_AND_3），结果由 Redis 缓存。
     */
    List<BankTopicGroupResponse> listTopicGroups(String season, String partKey);
}
