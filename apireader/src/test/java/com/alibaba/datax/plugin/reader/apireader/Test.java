package com.alibaba.datax.plugin.reader.apireader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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


    @org.junit.Test
    public void test() throws JsonProcessingException {
        CallChainDto callChainDto = JacksonUtils.tryStr2Bean("{\"chainName\":\"查询产品属性\",\"chainId\":\"78ccc5af-ad82-40c7-b4cd-514d205412d5\",\"chains\":[{\"index\":1,\"chainType\":\"API\",\"api\":{\"id\":\"78ccc5af-ad82-40c7-b4cd-514d205412d5\",\"name\":\"查询产品属性\",\"method\":\"GET\",\"urlTemplate\":\"http://172.21.234.21:32580/api/product/getProductAndAttr/1/57c94c57-62a3-5e80-92a4-d5160e6e0924\",\"headerTemplate\":[{\"key\":\"X-Project\",\"value\":\"b0e528ef-e2f9-36cf-8509-5514c2aa39f6\"},{\"key\":\"X-Authorization\",\"value\":\"Bearer eyJhbGciOiJIUzUxMiJ9.eyJzY29wZXMiOlsiVEVOQU5UX0FETUlOIl0sInVzZXJJZCI6ImI3NDc3ZGE4LTI1MjMtNDhlYi1iYzdkLTRjZWIwYzM0OGIzNCIsImxvZ2luRGV2aWNlVHlwZSI6IlBDIiwiaXNQdWJsaWMiOmZhbHNlLCJsb2dpbklkIjoiNjQxNzQyNzMtMTIxZS00MTA4LWI2YzYtYjVhZTBkZDViMmU3IiwibmFtZSI6IuS8geS4mueuoeeQhuWRmCIsInBob25lIjoiMTg2MDAwMDAwMDEiLCJ0ZW5hbnRJZCI6IjRjYjJiNGM4LTJlOTEtMzcyZC04ZjExLWE5MDEzMDQzNWQzOCIsImlzcyI6InN6LW5pbWJ1cy5jb20iLCJpYXQiOjE3MTkyMTU4NDMsImV4cCI6MTcyMTgwNzg0M30.1Solws8oL5444pV67i-Frhxnsb0kIDlFPAlVY8dZksXuxaKI-gIZ_aSZcPw57N36YVHQJIl6svsv1PrYyDdyAQ\"}],\"authTemplate\":{},\"cookieTemplate\":[],\"paramsTemplate\":[],\"timeout\":5,\"resultPredicate\":{\"httpStatusCode\":200},\"schema\":{\"dataType\":\"ARRAY\",\"items\":{\"dataType\":\"OBJECT\",\"properties\":[{\"key\":\"productIdentifier\",\"description\":\"productIdentifier\",\"dataType\":\"STRING\",\"refer\":\"items[].productIdentifier\"},{\"key\":\"productName\",\"description\":\"productName\",\"dataType\":\"STRING\",\"refer\":\"items[].productName\"},{\"key\":\"attrKey\",\"description\":\"attrKey\",\"dataType\":\"STRING\",\"refer\":\"items[].attrKey\"},{\"key\":\"attrName\",\"description\":\"attrName\",\"dataType\":\"STRING\",\"refer\":\"items[].attrName\"}]}}}},{\"index\":2,\"chainType\":\"RESULT_DEFINITION\",\"schema\":{\"dataType\":\"ARRAY\",\"items\":{\"dataType\":\"OBJECT\",\"properties\":[{\"key\":\"productIdentifier\",\"description\":\"productIdentifier\",\"dataType\":\"STRING\",\"refer\":\"{{1.[].productIdentifier}}\"},{\"key\":\"productName\",\"description\":\"productName\",\"dataType\":\"STRING\",\"refer\":\"{{1.[].productName}}\"},{\"key\":\"attrKey\",\"description\":\"attrKey\",\"dataType\":\"STRING\",\"refer\":\"{{1.[].attrKey}}\"},{\"key\":\"attrName\",\"description\":\"attrName\",\"dataType\":\"STRING\",\"refer\":\"{{1.[].attrName}}\"}]}}}],\"label\":\"查询产品属性\",\"params\":[],\"value\":\"78ccc5af-ad82-40c7-b4cd-514d205412d5\",\"chainIdentifier\":\"78ccc5af-ad82-40c7-b4cd-514d205412d5\"}", CallChainDto.class);
        ApiRequestDto apiRequestDto = new ApiRequestDto();
        apiRequestDto.setId(callChainDto.getChainId());
        ApiResultDto<ChainsResultDto> result = ApiRequestExecutor.invoke(apiRequestDto, callChainDto);
        AbstractSchema abstractSchema = result.getItems().getSchema();

        List<AbstractSchema> schemaList = ((ObjectSchema) ((ArraySchema) abstractSchema).getItems()).getProperties();

        ArrayNode resultArrayNode = JacksonUtils.tryObj2ArrayNode(result.getItems().getParserResult());

        for (JsonNode jsonNode : resultArrayNode) {
            ObjectNode objNode =  JacksonUtils.obj2ObjectNode(jsonNode);
            for (AbstractSchema schema : schemaList) {
                String key = schema.getKey();
                DataType dataType = schema.getDataType();
                System.out.print(objNode.get(key).asText());
            }
            System.out.println("=======");
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
