package com.alibaba.datax.plugin.reader.apireader;

import com.alibaba.datax.common.element.BoolColumn;
import com.alibaba.datax.common.element.DateColumn;
import com.alibaba.datax.common.element.DoubleColumn;
import com.alibaba.datax.common.element.LongColumn;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbus.apihub.call.ApiRequestExecutor;
import com.nimbus.apihub.dto.api.ApiRequestDto;
import com.nimbus.apihub.dto.chain.CallChainDto;
import com.nimbus.apihub.dto.parse.ChainsResultDto;
import com.nimbus.apihub.dto.schema.AbstractSchema;
import com.nimbus.apihub.dto.schema.ArraySchema;
import com.nimbus.apihub.dto.schema.ObjectSchema;
import com.nimbus.apihub.enums.DataType;
import com.nimbus.common.data.model.api.ApiResultDto;
import com.nimbus.common.utils.utils.JacksonUtils;
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
            String apiInfo = taskConfig.get("").toString();
            log.info("apiInfo>>>{}", apiInfo);
            CallChainDto callChainDto = JacksonUtils.tryStr2Bean(apiInfo, CallChainDto.class);
            ApiRequestDto apiRequestDto = new ApiRequestDto();
            apiRequestDto.setId(callChainDto.getChainId());
            ApiResultDto<ChainsResultDto> result = ApiRequestExecutor.invoke(apiRequestDto, callChainDto);
            AbstractSchema abstractSchema = result.getItems().getSchema();
            List<AbstractSchema> schemaList;
            try {
                schemaList = ((ObjectSchema) ((ArraySchema) abstractSchema).getItems()).getProperties();
            } catch (Exception e) {
                log.error("获取schema失败", e);
                throw new RuntimeException("获取schema失败");
            }
            ArrayNode resultArrayNode = JacksonUtils.tryObj2ArrayNode(result.getItems().getParserResult());
            for (JsonNode jsonNode : resultArrayNode) {
                ObjectNode objNode;
                Record record = recordSender.createRecord();
                try {
                    objNode = JacksonUtils.obj2ObjectNode(jsonNode);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                for (AbstractSchema schema : schemaList) {
                    String key = schema.getKey();
                    DataType dataType = schema.getDataType();
                    JsonNode value = objNode.get(key);
                    boolean isNullNode = value instanceof NullNode;
                    switch (dataType) {
                        case INTEGER:
                            record.addColumn(new LongColumn(isNullNode ? null : value.asLong()));
                            break;
                        case STRING:
                            record.addColumn(new StringColumn(isNullNode ? null : value.asText()));
                            break;
                        case NUMBER:
                            record.addColumn(new DoubleColumn(isNullNode ? null : value.asDouble()));
                            break;
                        case BOOLEAN:
                            record.addColumn(new BoolColumn(isNullNode ? null : value.asBoolean()));
                            break;
                        case DATE:
                            String valueStr = value.asText();
                            if (isNullNode) {
                                Date date = null;
                                record.addColumn(new DateColumn(date));
                            } else if (isValidTimestampString(valueStr)) {
                                OffsetDateTime offsetDateTime = OffsetDateTime.parse(valueStr, formatter);
                                record.addColumn(new DateColumn(Date.from(offsetDateTime.toInstant())));
                            } else if (isValidDateString(valueStr)) {
                                record.addColumn(new DateColumn(sdf.parse(valueStr)));
                            } else if (isValidLong(valueStr)) {
                                record.addColumn(new DateColumn(Long.valueOf(valueStr)));
                            } else {
                                throw new RuntimeException("不支持该日期类型：" + value);
                            }
                            break;
                        default:
                            log.warn("字段类型为其他，默认为String");
                            record.addColumn(new StringColumn(isNullNode ? null : value.asText()));
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
