package com.alibaba.datax.common.element;

import com.alibaba.datax.common.exception.CommonErrorCode;
import com.alibaba.datax.common.exception.DataXException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * [dataX源码修改]: UUID Column 扩展.
 *
 * @author lanvendar
 * @version V1.0.0
 * @since 2024/03/28
 */
public class UuidColumn extends Column {
    
    public UuidColumn() {
        this(null);
    }
    
    public UuidColumn(final Object rawData) {
        super(rawData, Type.UUID, (null == rawData ? 0 : rawData.toString().length()));
    }
    
    @Override
    public String asString() {
        if (null == this.getRawData()) {
            return null;
        }
        
        return this.getRawData().toString();
    }
    
    @Override
    public BigInteger asBigInteger() {
        throw DataXException.asDataXException(CommonErrorCode.CONVERT_NOT_SUPPORT, String.format("String[\"%s\"]不能转为BigInteger .", this.asString()));
    }
    
    @Override
    public Long asLong() {
        throw DataXException.asDataXException(CommonErrorCode.CONVERT_NOT_SUPPORT, String.format("String[\"%s\"]不能转为Long .", this.asString()));
    }
    
    @Override
    public BigDecimal asBigDecimal() {
        throw DataXException.asDataXException(CommonErrorCode.CONVERT_NOT_SUPPORT,
                String.format("String [\"%s\"] 不能转为BigDecimal .", this.asString()));
    }
    
    @Override
    public Double asDouble() {
        throw DataXException.asDataXException(CommonErrorCode.CONVERT_NOT_SUPPORT, String.format("String [\"%s\"] 不能转为Double .", this.asString()));
    }
    
    @Override
    public Boolean asBoolean() {
        throw DataXException.asDataXException(CommonErrorCode.CONVERT_NOT_SUPPORT, String.format("String[\"%s\"]不能转为Bool .", this.asString()));
    }
    
    @Override
    public Date asDate() {
        throw DataXException.asDataXException(CommonErrorCode.CONVERT_NOT_SUPPORT, String.format("String[\"%s\"]不能转为Date .", this.asString()));
    }
    
    @Override
    public Date asDate(String dateFormat) {
        throw DataXException.asDataXException(CommonErrorCode.CONVERT_NOT_SUPPORT, String.format("String[\"%s\"]不能转为Date .", this.asString()));
    }
    
    @Override
    public byte[] asBytes() {
        try {
            return ColumnCast.uuid2Bytes(this);
        } catch (Exception e) {
            throw DataXException.asDataXException(CommonErrorCode.CONVERT_NOT_SUPPORT, String.format("String[\"%s\"]不能转为Bytes .", this.asString()));
        }
    }
}
