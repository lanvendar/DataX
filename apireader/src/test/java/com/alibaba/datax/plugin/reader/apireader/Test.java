package com.alibaba.datax.plugin.reader.apireader;

import com.alibaba.datax.common.element.BoolColumn;
import com.alibaba.datax.common.element.DateColumn;
import com.alibaba.datax.common.element.DoubleColumn;
import com.alibaba.datax.common.element.LongColumn;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.element.UuidColumn;
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


    @org.junit.Test
    public void test() {
        RequestExecutor requestExecutor = new BaseRequestExecutor();
//        String json = "{\"id\":\"\",\"name\":\"数据源查询\",\"method\":\"POST\",\"url\":\"http://172.21.234.21:32001/bdmm-manager/api/schemaInfo/list\",\"header\":{\"X-Authorization\":[\"Bearer ${token}\"]},\"param\":{},\"auth\":{\"type\":\"API_KEY\",\"param\":{\"location\":\"header\",\"key\":\"hello\",\"value\":\"world\"}},\"body\":{\"params\":{},\"page\":1,\"limit\":200},\"cacheParamList\":[],\"cacheConfig\":{\"type\":\"file\",\"param\":{\"cacheTimeout\":\"0\"}},\"timeout\":0,\"analyzeColumnList\":[{\"index\":1,\"name\":\"schemaId\",\"type\":\"STRING\",\"path\":\"items[].schemaId1\"},{\"index\":2,\"name\":\"databaseType\",\"type\":\"STRING\",\"path\":\"items[].schemaName\"},{\"index\":3,\"name\":\"schemaName\",\"type\":\"STRING\",\"path\":\"items[].driverClass\"},{\"index\":4,\"name\":\"erMessage\",\"type\":\"STRING\",\"path\":\"erMessage\"}],\"preApiInfoList\":[{\"id\":\"\",\"name\":\"\",\"method\":\"POST\",\"url\":\"http://172.16.5.235:32001/infra-auth/api/noauth/v2/login\",\"header\":{},\"param\":{},\"auth\":{\"type\":\"NO_AUTH\",\"param\":{}},\"body\":{\"device\":\"PC\",\"username\":\"Q0gKfYXKUPGKOmjLjLZUFVVveY7FRZ936yftf3bYsTd10oPq78YAvS2f9dei4cQ/DqxBJtWJl3k4rvHknLFXOv/9YzB2Qmov5DJS1VRWpQInZBRSXmzV8loB29pM0Kje4Bkw3SYPKCCrPBd8YilVhRQgJeEIrNOymvWP7+U9esI=\",\"password\":\"XsYYH7+rx7aAE8oXS3bJqsEZ6ilL0Cl1OXE7LSgteNT5tHqr4Z1RNnq49UWdsmyooEpG/9qXmC3Ysach6Y0NarQlLaTU4hVHFBDZTc8K2dcWP3SZGdvr9aReq1eF0lk26k8RfKNhDZt7z9NjksQUMucn/JgLDsFrqf6ag74eps4=\"},\"cacheParamList\":[{\"name\":\"token\",\"path\":\"items.token\"}],\"cacheConfig\":{\"type\":\"file\",\"param\":{\"cacheTimeout\":\"0\"}},\"timeout\":0,\"analyzeColumnList\":[],\"requestOrder\":0}]}";;
        String json = "{\"id\":\"\",\"name\":\"数据源查询\",\"method\":\"POST\",\"url\":\"http://172.21.234.21:32001/bdmm-manager/api/schemaInfo/list\",\"header\":{\"X-Authorization\":[\"Bearer ${token}\"]},\"param\":{},\"auth\":{\"type\":\"API_KEY\",\"param\":{\"location\":\"header\",\"key\":\"hello\",\"value\":\"world\"}},\"body\":{\"params\":{},\"page\":1,\"limit\":20},\"cacheParamList\":[],\"cacheConfig\":{\"type\":\"file\",\"param\":{\"cacheTimeout\":\"0\"}},\"timeout\":5,\"isPreApi\":false,\"analyzeColumnList\":[{\"index\":1,\"name\":\"schemaId\",\"type\":\"UUID\",\"path\":\"items[].schemaId\"},{\"index\":2,\"name\":\"schemaName\",\"type\":\"STRING\",\"path\":\"items[].schemaName\"}],\"preApiInfoList\":[{\"id\":\"\",\"name\":\"\",\"method\":\"POST\",\"url\":\"http://172.16.5.235:32001/infra-auth/api/noauth/v2/login\",\"header\":{},\"param\":{},\"auth\":{\"type\":\"NO_AUTH\",\"param\":{}},\"body\":{\"device\":\"PC\",\"username\":\"Q0gKfYXKUPGKOmjLjLZUFVVveY7FRZ936yftf3bYsTd10oPq78YAvS2f9dei4cQ/DqxBJtWJl3k4rvHknLFXOv/9YzB2Qmov5DJS1VRWpQInZBRSXmzV8loB29pM0Kje4Bkw3SYPKCCrPBd8YilVhRQgJeEIrNOymvWP7+U9esI=\",\"password\":\"XsYYH7+rx7aAE8oXS3bJqsEZ6ilL0Cl1OXE7LSgteNT5tHqr4Z1RNnq49UWdsmyooEpG/9qXmC3Ysach6Y0NarQlLaTU4hVHFBDZTc8K2dcWP3SZGdvr9aReq1eF0lk26k8RfKNhDZt7z9NjksQUMucn/JgLDsFrqf6ag74eps4=\"},\"cacheParamList\":[{\"name\":\"token\",\"path\":\"items.token\"}],\"cacheConfig\":{\"type\":\"file\",\"param\":{\"cacheTimeout\":\"0\"}},\"timeout\":0,\"analyzeColumnList\":[],\"requestOrder\":1}]}";
        ApiInfoDto apiInfoDto = JacksonUtils.string2bean(json, ApiInfoDto.class);
        RequestResultDto requestResultDto = requestExecutor.sendRequest(apiInfoDto);
        List<List<Object>> result = requestResultDto.getResult();
        System.out.println(result);
    }


    @org.junit.Test
    public void test2() throws ParseException {
        String json = "{\"id\":\"aadaef1b-0a78-491c-a18a-5e6b5d9b79c3\",\"name\":\"获取数据源信息\",\"method\":\"POST\",\"url\":\"http://172.21.234.21:32001/bdmm-manager/api/schemaInfo/list\",\"header\":{\"X-Authorization\":[\"Bearer ${token}\"]},\"body\":{\"params\":{},\"page\":1,\"limit\":20},\"param\":{},\"auth\":{\"type\":\"API_KEY\",\"param\":{\"location\":\"header\",\"value\":\"world\",\"key\":\"hello\"}},\"cacheParamList\":[],\"cacheConfig\":{\"type\":\"file\",\"param\":{\"cacheTimeout\":\"0\"}},\"timeout\":5,\"analyzeColumnList\":[{\"index\":1,\"name\":\"schemaId\",\"type\":\"UUID\",\"path\":\"items[].schemaId\"},{\"index\":2,\"name\":\"schemaName\",\"type\":\"STRING\",\"path\":\"items[].schemaName\"},{\"index\":3,\"name\":\"tableCount\",\"type\":\"STRING\",\"path\":\"items[].tableCount\"},{\"index\":4,\"name\":\"createTime\",\"type\":\"DATE\",\"path\":\"items[].createTime\"}],\"isPreApi\":false,\"preApiInfoList\":[{\"id\":\"\",\"name\":\"\",\"method\":\"POST\",\"url\":\"http://172.16.5.235:32001/infra-auth/api/noauth/v2/login\",\"header\":{},\"param\":{},\"auth\":{\"type\":\"NO_AUTH\",\"param\":{}},\"body\":{\"device\":\"PC\",\"username\":\"Q0gKfYXKUPGKOmjLjLZUFVVveY7FRZ936yftf3bYsTd10oPq78YAvS2f9dei4cQ/DqxBJtWJl3k4rvHknLFXOv/9YzB2Qmov5DJS1VRWpQInZBRSXmzV8loB29pM0Kje4Bkw3SYPKCCrPBd8YilVhRQgJeEIrNOymvWP7+U9esI=\",\"password\":\"XsYYH7+rx7aAE8oXS3bJqsEZ6ilL0Cl1OXE7LSgteNT5tHqr4Z1RNnq49UWdsmyooEpG/9qXmC3Ysach6Y0NarQlLaTU4hVHFBDZTc8K2dcWP3SZGdvr9aReq1eF0lk26k8RfKNhDZt7z9NjksQUMucn/JgLDsFrqf6ag74eps4=\"},\"cacheParamList\":[{\"name\":\"token\",\"path\":\"items.token\"}],\"cacheConfig\":{\"type\":\"file\",\"param\":{\"cacheTimeout\":\"0\"}},\"timeout\":0,\"analyzeColumnList\":[],\"requestOrder\":1}]}";
        ApiInfoDto apiInfoDto = JacksonUtils.string2bean(json, ApiInfoDto.class);
        List<AnalyzeColumnDto> analyzeColumnList = apiInfoDto.getAnalyzeColumnList();
        if (CollectionUtils.isEmpty(analyzeColumnList)) {
            throw new CustomException("字段解析列表为空");
        }
        RequestExecutor requestExecutor = new BaseRequestExecutor();
        RequestResultDto requestResult = requestExecutor.sendRequest(apiInfoDto);
        List<List<Object>> result = requestResult.getResult();
        for (List<Object> row : result) {
            for (int i = 0; i < row.size(); i++) {
                AnalyzeColumnDto analyzeColumnDto = analyzeColumnList.get(i);
                ColumnTypeEnum columnType = analyzeColumnDto.getType();
                String value = String.valueOf(row.get(i));
                Object rowValue = row.get(i);
                switch (columnType) {
                    case INT:
                    case LONG:
                        new LongColumn(Long.valueOf(value));
                        break;
                    case DOUBLE:
                        new DoubleColumn(Double.valueOf(value));
                        break;
                    case BOOLEAN:
                        new BoolColumn(Boolean.valueOf(value));
                        break;
                    case STRING:
                        new StringColumn(value);
                        break;
                    case DATE:
                        if (null == value || "null".equals(value)) {
                            Date date = null;
                            new DateColumn(date);
                        } else if (isValidDateString(value)) {
                            log.info("date>>");
                            new DateColumn(sdf.parse(value));
                        } else if (isValidLong(value)) {
                            log.info("long>>");
                            new DateColumn(Long.valueOf(value));
                        } else {
                            throw new CustomException("不支持该日期类型：" + value);
                        }
                        break;
                    case UUID:
                        new UuidColumn(value);
                        break;
                    default:
                        log.warn("字段类型为其他，默认为String");
                        new StringColumn(value);
                        break;
                }
            }
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
