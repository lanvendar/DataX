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

package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.plugin.RecordReceiver;

import java.util.LinkedList;

/**
 * @author Li Zhengkai
 * @version 3.0, 2024/7/4
 * @since 2024/7/4
 */
public class SimpleRecordReceiver implements RecordReceiver {
    
    private LinkedList<Record> list;
    
    public SimpleRecordReceiver(LinkedList<Record> list) {
        this.list = list;
    }
    
    @Override
    public Record getFromReader() {
        return list.poll();
    }
    
    @Override
    public void shutdown() {
    
    }
}
