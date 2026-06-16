package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

final class ProxyPrimaryKeyGenerator {
    
    private ProxyPrimaryKeyGenerator() {
    }
    
    static String generate(String joinedValues, ProxyPrimaryKeyAlgorithm algorithm) {
        switch (algorithm) {
            case UUID:
                return UUID.nameUUIDFromBytes(joinedValues.getBytes(StandardCharsets.UTF_8)).toString();
            case SHA_256:
            case SHA_512:
                return digest(joinedValues, algorithm.getConfigName());
            default:
                throw DataXException.asDataXException("不支持的代理主键算法: " + algorithm);
        }
    }
    
    private static String digest(String value, String algorithm) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            byte[] bytes = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } catch (Exception e) {
            throw DataXException.asDataXException("生成代理主键失败: " + e.getMessage());
        }
    }
}
