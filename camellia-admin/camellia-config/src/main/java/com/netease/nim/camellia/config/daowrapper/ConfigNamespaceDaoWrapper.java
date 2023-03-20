package com.netease.nim.camellia.config.daowrapper;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.config.conf.ConfigProperties;
import com.netease.nim.camellia.config.dao.ConfigNamespaceDao;
import com.netease.nim.camellia.config.model.ConfigNamespace;
import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by caojiajun on 2023/3/15
 */
@Service
public class ConfigNamespaceDaoWrapper {

    private static final String tag = "camellia_config_namespace";

    @Autowired
    private ConfigNamespaceDao dao;

    @Autowired
    private CamelliaRedisTemplate template;

    @Autowired
    private ConfigProperties configProperties;

    public int create(ConfigNamespace configNamespace) {
        try {
            return dao.create(configNamespace);
        } finally {
            clearCache(configNamespace.getNamespace());
        }
    }

    public int update(ConfigNamespace configNamespace) {
        try {
            return dao.update(configNamespace);
        } finally {
            clearCache(configNamespace.getNamespace());
        }
    }

    public int delete(ConfigNamespace configNamespace) {
        try {
            return dao.delete(configNamespace.getId());
        } finally {
            clearCache(configNamespace.getNamespace());
        }
    }

    public ConfigNamespace getById(long id) {
        return dao.getById(id);
    }

    public List<ConfigNamespace> getList(int offset, int limit, boolean onlyValid, String keyword) {
        if (keyword != null) {
            keyword = keyword.trim();
        }
        if (keyword == null || keyword.length() == 0) {
            if (onlyValid) {
                return dao.getValidList(offset, limit);
            } else {
                return dao.getList(offset, limit);
            }
        } else {
            if (onlyValid) {
                return dao.getValidListAndKeyword(offset, limit, keyword);
            } else {
                return dao.getListAndKeyword(offset, limit, keyword);
            }
        }
    }

    public ConfigNamespace getByNamespace(String namespace) {
        String cacheKey = CacheUtil.buildCacheKey(tag, namespace);
        String value = template.get(cacheKey);
        if (value != null) {
            if (value.equalsIgnoreCase("null")) {
                return null;
            }
            return JSONObject.parseObject(value, ConfigNamespace.class);
        }
        ConfigNamespace configNamespace = dao.getByNamespace(namespace);
        if (configNamespace != null) {
            template.setex(cacheKey, configProperties.getDaoCacheExpireSeconds(), JSONObject.toJSONString(configNamespace));
        } else {
            template.setex(cacheKey, configProperties.getDaoCacheExpireSeconds(), "null");
        }
        return configNamespace;
    }

    private void clearCache(String namespace) {
        String cacheKey = CacheUtil.buildCacheKey(tag, namespace);
        template.del(cacheKey);
    }
}