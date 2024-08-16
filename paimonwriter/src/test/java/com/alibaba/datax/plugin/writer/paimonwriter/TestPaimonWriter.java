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
import com.alibaba.datax.common.util.Configuration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * @author Li Zhengkai
 * @version 3.0, 2024/7/4
 * @since 2024/7/4
 */
public class TestPaimonWriter {
    
    private static final Logger log = LoggerFactory.getLogger(TestPaimonWriter.class);
    
    @Test
    public void testWrite() {
        log.info("开始");
        
        PaimonWriter.Task task = new PaimonWriter.Task();
        
        
        //构造测试
        Configuration conf = Configuration.from(TestPaimonWriter.class.getResourceAsStream("/plugin.json"));
        task.setPluginJobConf(conf);
        
        task.init();
        
        task.prepare();
        
        LinkedList<Record> list = new LinkedList<>();
        
        //DefaultRecord record1 = new DefaultRecord();
        //record1.setColumn(0, new StringColumn("张三a"));
        //record1.setColumn(1, new LongColumn(39));
        //
        //DefaultRecord record2 = new DefaultRecord();
        //record2.setColumn(0, new StringColumn("王五a"));
        //record2.setColumn(1, new LongColumn(49));
        //
        //list.add(record1);
        //list.add(record2);
        
        SimpleRecordReceiver recordReceiver = new SimpleRecordReceiver(list);
        
        task.startWrite(recordReceiver);
        
        log.info("结束");
    }
    
    @Test
    public void my() {
        // 当前日期
        LocalDate today = LocalDate.now();
        
        // 过去一年的日期
        LocalDate oneYearAgo = today.minusYears(1);
        
        // 创建日期格式器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        
        // 生成日期列表
        ArrayList<String> dateList = new ArrayList<>();
        for (LocalDate date = oneYearAgo; !date.isAfter(today); date = date.plusDays(1)) {
            dateList.add(date.format(formatter));
        }
        
        // 倒序排序
        Collections.reverse(dateList);
        
        // 输出日期列表
        for (String date : dateList) {
            System.out.println(date);
        }
    }
}
