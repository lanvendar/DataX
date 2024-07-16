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
//        CallChainDto callChainDto = JacksonUtils.tryStr2Bean("{\"chainId\":\"78ccc5af-ad82-40c7-b4cd-514d205412d5\",\"chainIdentifier\":\"78ccc5af-ad82-40c7-b4cd-514d205412d5\",\"chainName\":\"查询产品属性\",\"params\":[],\"chains\":[{\"index\":1,\"chainType\":\"API\",\"api\":{\"id\":\"78ccc5af-ad82-40c7-b4cd-514d205412d5\",\"name\":\"查询产品属性\",\"method\":\"GET\",\"urlTemplate\":\"http://172.21.234.21:32580/api/product/getProductAndAttr/1/57c94c57-62a3-5e80-92a4-d5160e6e0924\",\"headerTemplate\":[{\"key\":\"X-Project\",\"value\":\"b0e528ef-e2f9-36cf-8509-5514c2aa39f6\"},{\"key\":\"X-Authorization\",\"value\":\"Bearer eyJhbGciOiJIUzUxMiJ9.eyJzY29wZXMiOlsiVEVOQU5UX0FETUlOIl0sInVzZXJJZCI6ImI3NDc3ZGE4LTI1MjMtNDhlYi1iYzdkLTRjZWIwYzM0OGIzNCIsImxvZ2luRGV2aWNlVHlwZSI6IlBDIiwiaXNQdWJsaWMiOmZhbHNlLCJsb2dpbklkIjoiNjQxNzQyNzMtMTIxZS00MTA4LWI2YzYtYjVhZTBkZDViMmU3IiwibmFtZSI6IuS8geS4mueuoeeQhuWRmCIsInBob25lIjoiMTg2MDAwMDAwMDEiLCJ0ZW5hbnRJZCI6IjRjYjJiNGM4LTJlOTEtMzcyZC04ZjExLWE5MDEzMDQzNWQzOCIsImlzcyI6InN6LW5pbWJ1cy5jb20iLCJpYXQiOjE3MTkyMTU4NDMsImV4cCI6MTcyMTgwNzg0M30.1Solws8oL5444pV67i-Frhxnsb0kIDlFPAlVY8dZksXuxaKI-gIZ_aSZcPw57N36YVHQJIl6svsv1PrYyDdyAQ\"},{\"key\":\"test\",\"value\":\"#{test}\"}],\"authTemplate\":{},\"cookieTemplate\":[],\"paramsTemplate\":[],\"timeout\":5,\"resultPredicate\":{\"httpStatusCode\":200},\"schema\":{\"dataType\":\"ARRAY\",\"items\":{\"dataType\":\"OBJECT\",\"properties\":[{\"key\":\"productIdentifier\",\"description\":\"productIdentifier\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"items[].productIdentifier\"},{\"key\":\"productName\",\"description\":\"productName\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"items[].productName\"},{\"key\":\"attrKey\",\"description\":\"attrKey\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"items[].attrKey\"},{\"key\":\"attrName\",\"description\":\"attrName\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"items[].attrName\"}]}}}},{\"index\":2,\"chainType\":\"RESULT_DEFINITION\",\"schema\":{\"dataType\":\"ARRAY\",\"items\":{\"dataType\":\"OBJECT\",\"properties\":[{\"key\":\"productIdentifier\",\"description\":\"productIdentifier\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"{{1.[].productIdentifier}}\"},{\"key\":\"productName\",\"description\":\"productName\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"{{1.[].productName}}\"},{\"key\":\"attrKey\",\"description\":\"attrKey\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"{{1.[].attrKey}}\"},{\"key\":\"attrName\",\"description\":\"attrName\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"{{1.[].attrName}}\"}]}}}]}", CallChainDto.class);
        CallChainDto callChainDto = JacksonUtils.tryStr2Bean("{\"chainId\":\"dd3413c8-3a12-4020-83b9-dbd840cf9db4\",\"chainIdentifier\":\"dd3413c8-3a12-4020-83b9-dbd840cf9db4\",\"chainName\":\"test2\",\"params\":[],\"chains\":[{\"index\":1,\"chainType\":\"API\",\"api\":{\"id\":\"dd3413c8-3a12-4020-83b9-dbd840cf9db4\",\"name\":\"test2\",\"method\":\"GET\",\"urlTemplate\":\"http://172.16.8.15:18082/test\",\"headerTemplate\":[],\"authTemplate\":{},\"cookieTemplate\":[],\"paramsTemplate\":[],\"timeout\":5,\"resultPredicate\":{\"httpStatusCode\":200},\"schema\":{\"dataType\":\"OBJECT\",\"properties\":[{\"key\":\"ydmj\",\"description\":\"ydmj\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"data.ydmj\"},{\"key\":\"projectNo\",\"description\":\"projectNo\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"data.projectNo\"}]}}},{\"index\":2,\"chainType\":\"RESULT_DEFINITION\",\"schema\":{\"dataType\":\"OBJECT\",\"properties\":[{\"key\":\"ydmj\",\"description\":\"ydmj\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"{{1.ydmj}}\"},{\"key\":\"projectNo\",\"description\":\"projectNo\",\"dataType\":\"STRING\",\"extractLocation\":\"RESPONSE_BODY\",\"refer\":\"{{1.projectNo}}\"}]}}]}", CallChainDto.class);
        ApiRequestDto apiRequestDto = new ApiRequestDto();
        apiRequestDto.setId(callChainDto.getChainId());
        ApiResultDto<ChainsResultDto> result = ApiRequestExecutor.invoke(apiRequestDto, callChainDto);
        AbstractSchema abstractSchema = result.getItems().getSchema();

        List<AbstractSchema> schemaList;
        if (abstractSchema instanceof ArraySchema) {
            schemaList = ((ObjectSchema) ((ArraySchema) abstractSchema).getItems()).getProperties();

        } else {//ObjectSchema
            schemaList = ((ObjectSchema) abstractSchema).getProperties();
        }

        Object parserResult = result.getItems().getParserResult();
        ArrayNode resultArrayNode = JacksonUtils.createArrayNode();
        if (parserResult instanceof ArrayNode) {
            resultArrayNode = JacksonUtils.tryObj2ArrayNode(parserResult);
        } else {//ObjectNode
            resultArrayNode.add(JacksonUtils.obj2ObjectNode(parserResult));
        }

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
