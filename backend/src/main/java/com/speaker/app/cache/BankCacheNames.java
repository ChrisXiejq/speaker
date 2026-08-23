package com.speaker.app.cache;

/**
 * 题库公开接口使用的 Spring Cache 名称，需与 {@code RedisCacheConfiguration} 及失效注解一致。
 */
public final class BankCacheNames {

    public static final String BANK_SEASONS = "bankSeasons";
    public static final String BANK_TOPIC_GROUPS = "bankTopicGroups";

    private BankCacheNames() {}
}
