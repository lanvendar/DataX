/*
 * Copyright © 2020-2024 Nimbus Corporation All rights reserved.
 *
 * 使本项目源码前请仔细阅读以下协议内容，如果你同意以下协议才能使用本项目所有的功能,
 * 否则如果你违反了以下协议，有可能陷入法律纠纷和赔偿，作者保留追究法律责任的权利.
 *
 * 1、本代码为商业源代码，只允许已授权内部人员查看使用
 * 2、任何人员无权将代码泄露或者授权给其他未被授权人员使用
 * 3、任何修改请保留原始作者信息，不得擅自删除及修改
 *
 * 请保留以上版权信息，否则作者将保留追究法律责任.
 */

package com.alibaba.datax.plugin.reader.paimonreader;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import org.apache.paimon.catalog.Catalog;
import org.apache.paimon.catalog.CatalogContext;
import org.apache.paimon.catalog.CatalogFactory;
import org.apache.paimon.catalog.Identifier;
import org.apache.paimon.options.Options;
import org.apache.paimon.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Li Zhengkai
 * @version 3.0, 2024/7/3
 * @since 2024/7/3
 */
public class PaimonHelper {
    
    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PaimonHelper.class);
    
    /**
     * 创建 paimon catalog.
     *
     * @return catalog对象.
     */
    static Catalog createPaimonCatalog(Configuration originalConfig) {
        Options options = PaimonConfigUtil.buildCatalogOptions(originalConfig);
        CatalogContext context = CatalogContext.create(options);
        return CatalogFactory.createCatalog(context);
    }
    
    /**
     * 获取 paimon table 对象.
     *
     * @return table对象.
     */
    static Table getPaimonTable(Configuration originalConfig) {
        Catalog paimonCatalog = createPaimonCatalog(originalConfig);
        String databaseName = PaimonConfigUtil.database(originalConfig);
        String tableName = originalConfig.getString(ConfigKey.TABLE);
        
        Identifier identifier = Identifier.create(databaseName, tableName);
        Table table = null;
        try {
            table = paimonCatalog.getTable(identifier);
        } catch (Catalog.TableNotExistException e) {
            LOG.error("table {} not exist, create it", tableName);
            throw DataXException.asDataXException(
                    String.format("表不存在: '%s'", tableName));
        }
        return table;
    }
}
