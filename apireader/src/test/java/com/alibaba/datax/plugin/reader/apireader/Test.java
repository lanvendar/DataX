package com.alibaba.datax.plugin.reader.apireader;

import com.alibaba.datax.common.element.BoolColumn;
import com.alibaba.datax.common.element.DateColumn;
import com.alibaba.datax.common.element.DoubleColumn;
import com.alibaba.datax.common.element.LongColumn;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.core.transport.record.DefaultRecord;
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

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

/**
 * TODO
 *
 * @author sunyingjie
 * @version 3.5.1, 2024/4/8
 * @since 2024/4/8
 **/
public class Test {

    private static final Logger log = LoggerFactory.getLogger(Test.class);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    // 定义日期时间格式器，匹配给定的字符串格式
    DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    Record record = new DefaultRecord();


    @org.junit.Test
    public void test() throws JsonProcessingException, ParseException {
        System.out.println((BigInteger) null);
        CallChainDto callChainDto = JacksonUtils.tryStr2Bean("{\"chainId\":\"9aacada2-a82c-475f-803a-d5b9a7b0219a\",\"chainIdentifier\":\"9aacada2-a82c-475f-803a-d5b9a7b0219a\",\"chainName\":\"【应用】动态感知 - 报名列表 - MD5加密\",\"params\":[{\"key\":\"timestamp\",\"name\":\"timestamp\",\"required\":true,\"dataType\":\"DATE\",\"defaultValue\":\"20240627181910\",\"description\":\"必填，时间戳yyyyMMddHHmmss\"},{\"key\":\"activityId\",\"name\":\"活动id\",\"required\":true,\"dataType\":\"STRING\",\"defaultValue\":\"07e71d599d734d3f844c5014a991462b\",\"description\":null},{\"key\":\"entName\",\"name\":\"企业名称\",\"required\":true,\"dataType\":\"STRING\",\"defaultValue\":\"苏州芯慧和创科技发展有限公司\",\"description\":null},{\"key\":\"clientSecret\",\"name\":\"clientSecret\",\"required\":true,\"dataType\":\"STRING\",\"defaultValue\":\"d489cea560bb4a49a5a66d602556245c\",\"description\":\"必填，md5加密需要\"},{\"key\":\"clientId\",\"name\":\"clientId\",\"required\":true,\"dataType\":\"STRING\",\"defaultValue\":\"dtgz\",\"description\":\"必填，应用clientId\"}],\"chains\":[{\"index\":2,\"chainType\":\"VARIABLE_ENCRYPTION\",\"cacheType\":null,\"expiration\":null,\"key\":\"sign\",\"dataType\":\"STRING\",\"encryption\":{\"type\":\"MD5\",\"variables\":{\"text\":\"{{clientId}}{{clientSecret}}{{timestamp}}\"}}},{\"index\":1,\"chainType\":\"API\",\"cacheType\":null,\"expiration\":null,\"api\":{\"id\":\"9aacada2-a82c-475f-803a-d5b9a7b0219a\",\"name\":\"【应用】动态感知 - 报名列表 - MD5加密\",\"description\":\"✔\",\"method\":\"GET\",\"urlTemplate\":\"http://172.16.8.15:18082/policy/api/ua/openapi/dtgz/enroll/list\",\"headerTemplate\":[{\"key\":\"clientId\",\"value\":\"{{clientId}}\",\"extraExpression\":null},{\"key\":\"timestamp\",\"value\":\"{{timestamp}}\",\"extraExpression\":null},{\"key\":\"sign\",\"value\":\"{{2.sign}}\",\"extraExpression\":null}],\"authTemplate\":{\"type\":null,\"params\":null},\"cookieTemplate\":[],\"paramsTemplate\":[{\"key\":\"activityId\",\"value\":\"{{activityId}}\",\"extraExpression\":null},{\"key\":\"entId\",\"value\":\"\",\"extraExpression\":null},{\"key\":\"entName\",\"value\":\"{{entName}}\",\"extraExpression\":null}],\"requestBodyTemplate\":null,\"timeout\":5,\"resultPredicate\":{\"httpStatusCode\":200,\"resultCode\":null,\"targetValue\":null},\"schema\":{\"key\":null,\"description\":null,\"dataType\":\"ARRAY\",\"refer\":null,\"format\":null,\"flag\":null,\"items\":{\"key\":null,\"description\":null,\"dataType\":\"OBJECT\",\"refer\":null,\"format\":null,\"flag\":null,\"properties\":[{\"key\":\"activityId\",\"description\":\"activityId\",\"dataType\":\"STRING\",\"refer\":\"data[].activityId\",\"format\":null,\"flag\":null,\"defaultValue\":null},{\"key\":\"entName\",\"description\":\"entName\",\"dataType\":\"STRING\",\"refer\":\"data[].entName\",\"format\":null,\"flag\":null,\"defaultValue\":null},{\"key\":\"entId\",\"description\":\"entId\",\"dataType\":\"STRING\",\"refer\":\"data[].entId\",\"format\":null,\"flag\":null,\"defaultValue\":null}]}}}},{\"index\":3,\"chainType\":\"RESULT_DEFINITION\",\"schema\":{\"key\":null,\"description\":null,\"dataType\":\"ARRAY\",\"refer\":null,\"format\":null,\"flag\":null,\"items\":{\"key\":null,\"description\":null,\"dataType\":\"OBJECT\",\"refer\":null,\"format\":null,\"flag\":null,\"properties\":[{\"key\":\"activityId\",\"description\":\"activityId\",\"dataType\":\"STRING\",\"refer\":\"{{1.[].activityId}}\",\"format\":null,\"flag\":null,\"defaultValue\":null},{\"key\":\"entName\",\"description\":\"entName\",\"dataType\":\"STRING\",\"refer\":\"{{1.[].entName}}\",\"format\":null,\"flag\":null,\"defaultValue\":null},{\"key\":\"entId\",\"description\":\"entId\",\"dataType\":\"STRING\",\"refer\":\"{{1.[].entId}}\",\"format\":null,\"flag\":null,\"defaultValue\":null}]}}}]}", CallChainDto.class);
        ApiRequestDto apiRequestDto = new ApiRequestDto();
        apiRequestDto.setId(callChainDto.getChainId());
        ApiResultDto<ChainsResultDto> result = ApiRequestExecutor.invoke(apiRequestDto, callChainDto);
        AbstractSchema abstractSchema = result.getItems().getSchema();

        List<AbstractSchema> schemaList = ((ObjectSchema) ((ArraySchema) abstractSchema).getItems()).getProperties();

        ArrayNode resultArrayNode = JacksonUtils.tryObj2ArrayNode(result.getItems().getParserResult());

        for (JsonNode jsonNode : resultArrayNode) {
            ObjectNode objNode;
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
                        if (isNullNode || null == valueStr || "null".equals(valueStr)) {
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
//            recordSender.sendToWriter(record);
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
