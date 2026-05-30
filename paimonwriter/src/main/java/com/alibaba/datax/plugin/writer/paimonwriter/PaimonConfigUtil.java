package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.paimon.fs.Path;
import org.apache.paimon.options.CatalogOptions;
import org.apache.paimon.options.Options;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class PaimonConfigUtil {
    
    private static final String CONNECT_TYPE_S3 = "S3";
    
    private static final Set<String> FIXED_OPTION_KEYS = new HashSet<>();
    
    static {
        FIXED_OPTION_KEYS.add("warehouse");
        FIXED_OPTION_KEYS.add("catalogType");
        FIXED_OPTION_KEYS.add("database");
        FIXED_OPTION_KEYS.add("endpoint");
        FIXED_OPTION_KEYS.add("region");
        FIXED_OPTION_KEYS.add("accessKey");
        FIXED_OPTION_KEYS.add("accessKeyRef");
        FIXED_OPTION_KEYS.add("secretKey");
        FIXED_OPTION_KEYS.add("secretKeyRef");
        FIXED_OPTION_KEYS.add("pathStyleAccess");
        FIXED_OPTION_KEYS.add("sslEnabled");
    }
    
    private PaimonConfigUtil() {
    }
    
    static Options buildCatalogOptions(Configuration config) {
        validateConnectType(config);
        
        Configuration optionConfig = config.getConfiguration(ConfigKey.OPTIONS);
        if (optionConfig == null) {
            throw DataXException.asDataXException("Paimon options不能为空");
        }
        
        Options options = new Options();
        Map<String, Object> dynamicOptions = config.getMap(ConfigKey.OPTIONS);
        if (dynamicOptions != null) {
            for (Map.Entry<String, Object> entry : dynamicOptions.entrySet()) {
                if (!FIXED_OPTION_KEYS.contains(entry.getKey()) && entry.getValue() != null) {
                    options.setString(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        }
        
        setRequired(options, CatalogOptions.WAREHOUSE.key(), config.getString(ConfigKey.WAREHOUSE), ConfigKey.WAREHOUSE);
        options.set(CatalogOptions.METASTORE.key(), config.getString(ConfigKey.CATALOG_TYPE, "filesystem"));
        
        setIfNotBlank(options, "s3.endpoint", config.getString(ConfigKey.ENDPOINT));
        setIfNotBlank(options, "s3.region", config.getString(ConfigKey.REGION, "us-east-1"));
        setIfNotBlank(options, "s3.access-key", resolveSecret(config, ConfigKey.ACCESS_KEY, ConfigKey.ACCESS_KEY_REF));
        setIfNotBlank(options, "s3.secret-key", resolveSecret(config, ConfigKey.SECRET_KEY, ConfigKey.SECRET_KEY_REF));
        options.setString("s3.path-style-access", String.valueOf(config.getBool(ConfigKey.PATH_STYLE_ACCESS, true)));
        options.setString("s3.ssl.enabled", String.valueOf(config.getBool(ConfigKey.SSL_ENABLED, false)));
        
        setIfNotBlank(options, "hadoop.fs.s3a.endpoint", config.getString(ConfigKey.ENDPOINT));
        setIfNotBlank(options, "hadoop.fs.s3a.endpoint.region", config.getString(ConfigKey.REGION, "us-east-1"));
        setIfNotBlank(options, "hadoop.fs.s3a.access.key", resolveSecret(config, ConfigKey.ACCESS_KEY, ConfigKey.ACCESS_KEY_REF));
        setIfNotBlank(options, "hadoop.fs.s3a.secret.key", resolveSecret(config, ConfigKey.SECRET_KEY, ConfigKey.SECRET_KEY_REF));
        options.setString("hadoop.fs.s3a.path.style.access", String.valueOf(config.getBool(ConfigKey.PATH_STYLE_ACCESS, true)));
        options.setString("hadoop.fs.s3a.connection.ssl.enabled", String.valueOf(config.getBool(ConfigKey.SSL_ENABLED, false)));
        options.setString("hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        options.setString("hadoop.fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
        options.setString("hadoop.fs.s3a.change.detection.mode", "none");
        options.setString("hadoop.fs.s3a.change.detection.version.required", "false");
        
        return options;
    }
    
    static String database(Configuration config) {
        String database = config.getString(ConfigKey.DATABASE);
        if (StringUtils.isBlank(database)) {
            throw DataXException.asDataXException("options.database不能为空");
        }
        return database;
    }
    
    private static void validateConnectType(Configuration config) {
        String connectType = config.getString(ConfigKey.CONNECT_TYPE);
        if (StringUtils.isBlank(connectType)) {
            throw DataXException.asDataXException("connectType不能为空");
        }
        if (!CONNECT_TYPE_S3.equalsIgnoreCase(connectType)) {
            throw DataXException.asDataXException("connectType当前仅支持S3");
        }
        if (StringUtils.isBlank(config.getString(ConfigKey.ENDPOINT))) {
            throw DataXException.asDataXException("options.endpoint不能为空");
        }
    }
    
    private static void setRequired(Options options, String optionKey, String value, String configKey) {
        if (StringUtils.isBlank(value)) {
            throw DataXException.asDataXException(configKey + "不能为空");
        }
        options.setString(optionKey, new Path(value).toUri().toString());
    }
    
    private static void setIfNotBlank(Options options, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            options.setString(key, value);
        }
    }
    
    private static String resolveSecret(Configuration config, String valueKey, String refKey) {
        String value = config.getString(valueKey);
        String ref = config.getString(refKey);
        if (StringUtils.isNotBlank(ref)) {
            String envValue = System.getenv(ref);
            if (StringUtils.isBlank(envValue)) {
                throw DataXException.asDataXException(refKey + "指定的环境变量不存在或为空: " + ref);
            }
            return envValue;
        }
        return value;
    }
}
