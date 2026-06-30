package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.bean.Config;

public final class ConfigCache {

  /** 配置缓存有效期：24 小时 */
  public static final long TTL_MS = 24 * 60 * 60 * 1000L;

  private ConfigCache() {
  }

  public static boolean isValid(Config config) {
    if (config == null || config.isEmpty()) return false;
    if (TextUtils.isEmpty(config.getJson())) return false;
    if (config.getTime() <= 0) return false;
    return System.currentTimeMillis() - config.getTime() < TTL_MS;
  }
}
