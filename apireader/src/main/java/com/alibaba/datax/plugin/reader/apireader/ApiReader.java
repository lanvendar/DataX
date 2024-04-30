package com.alibaba.datax.plugin.reader.apireader;

import com.alibaba.datax.common.element.BoolColumn;
import com.alibaba.datax.common.element.DateColumn;
import com.alibaba.datax.common.element.DoubleColumn;
import com.alibaba.datax.common.element.LongColumn;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.element.UuidColumn;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.nimbus.bdmm.apihub.constant.ColumnTypeEnum;
import com.nimbus.bdmm.apihub.dto.AnalyzeColumnDto;
import com.nimbus.bdmm.apihub.dto.ApiInfoDto;
import com.nimbus.bdmm.apihub.dto.RequestResultDto;
import com.nimbus.bdmm.apihub.executor.BaseRequestExecutor;
import com.nimbus.bdmm.apihub.executor.RequestExecutor;
import com.nimbus.bdmm.common.exception.CustomException;
import com.nimbus.bdmm.common.jackson.JacksonUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ApiReader.
 *
 * @author sunyingjie
 * @version 3.5.1, 2024/4/8
 * @since 2024/4/8
 **/
public class ApiReader extends Reader {

    private static final Logger log = LoggerFactory.getLogger(ApiReader.class);

    public static class Job extends Reader.Job {
        private Configuration jobConfig = null;

        @Override
        public void init() {
            this.jobConfig = super.getPluginJobConf();
        }

        @Override
        public void destroy() {

        }

        @Override
        public List<Configuration> split(int adviceNumber) {
            List<Configuration> splittedConfigs = new ArrayList<Configuration>();
            splittedConfigs.add(jobConfig);
            return splittedConfigs;
        }
    }

    public static class Task extends Reader.Task {
        private Configuration taskConfig = null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // 定义日期时间格式器，匹配给定的字符串格式
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        @Override
        public void init() {
            this.taskConfig = super.getPluginJobConf();

        }

        @Override
        public void destroy() {

        }

        @Override
        public void startRead(RecordSender recordSender) throws ParseException {
            ApiInfoDto apiInfoDto = JacksonUtils.string2bean(taskConfig.get("").toString(), ApiInfoDto.class);
            List<AnalyzeColumnDto> analyzeColumnList = apiInfoDto.getAnalyzeColumnList();
            if (CollectionUtils.isEmpty(analyzeColumnList)) {
                throw new CustomException("字段解析列表为空");
            }
            RequestExecutor requestExecutor = new BaseRequestExecutor();
            RequestResultDto requestResult = requestExecutor.sendRequest(apiInfoDto);
            List<List<Object>> result = requestResult.getResult();
            for (List<Object> row : result) {
                Record record = recordSender.createRecord();
                for (int i = 0; i < row.size(); i++) {
                    AnalyzeColumnDto analyzeColumnDto = analyzeColumnList.get(i);
                    ColumnTypeEnum columnType = analyzeColumnDto.getType();
                    String value = String.valueOf(row.get(i));
                    switch (columnType) {
                        case INT:
                        case LONG:
                            record.addColumn(new LongColumn(Long.valueOf(value)));
                            break;
                        case DOUBLE:
                            record.addColumn(new DoubleColumn(Double.valueOf(value)));
                            break;
                        case BOOLEAN:
                            record.addColumn(new BoolColumn(Boolean.valueOf(value)));
                            break;
                        case STRING:
                            record.addColumn(new StringColumn(value));
                            break;
                        case DATE:
                            if (null == value || "null".equals(value)) {
                                Date date = null;
                                record.addColumn(new DateColumn(date));
                            } else if (isValidTimestampString(value)) {
                                OffsetDateTime offsetDateTime = OffsetDateTime.parse(value, formatter);
                                record.addColumn(new DateColumn(Date.from(offsetDateTime.toInstant())));
                            } else if (isValidDateString(value)) {
                                record.addColumn(new DateColumn(sdf.parse(value)));
                            } else if (isValidLong(value)) {
                                record.addColumn(new DateColumn(Long.valueOf(value)));
                            } else {
                                throw new CustomException("不支持该日期类型：" + value);
                            }
                            break;
                        case UUID:
                            record.addColumn(new UuidColumn(value));
                            break;
                        default:
                            log.warn("字段类型为其他，默认为String");
                            record.addColumn(new StringColumn(value));
                            break;
                    }
                }
                recordSender.sendToWriter(record);
            }
        }

        /**
         * 判断字符串是否为日期格式
         *
         * @param dateString 日期字符串
         * @return true:是日期格式，false:不是日期格式
         */
        private boolean isValidDateString(String dateString) {
            // 设置严格解析，避免自动修正不准确的日期
            sdf.setLenient(false);
            try {
                sdf.parse(dateString);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }


        /**
         * 判断字符串是否为timestamp格式
         *
         * @param dateString 日期字符串
         * @return true/false
         */
        private boolean isValidTimestampString(String dateString) {
            try {
                // 解析字符串为OffsetDateTime对象
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateString, formatter);
                return true;
            } catch (DateTimeParseException e) {
                return false;
            }

        }


        /**
         * 判断字符串是否为long型
         *
         * @param str 字符串
         * @return true:是Long类型，false:不是Long格式
         */
        private boolean isValidLong(String str) {
            if (str == null || str.isEmpty()) {
                return false;
            }
            try {
                Long.parseLong(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}
