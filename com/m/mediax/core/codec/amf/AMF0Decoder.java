package com.m.mediax.core.codec.amf;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.codec.Decoder;
import org.apache.commons.codec.DecoderException;

import com.m.mediax.core.utils.ByteUtils;

public class AMF0Decoder implements Decoder {
    @Override
    public Object[] decode(Object source) throws DecoderException {
        if (source instanceof ByteBuffer) {
            return decode((ByteBuffer) source);
        } else if (source instanceof byte[]) {
            return decode(ByteBuffer.wrap((byte[]) source));
        } else {
            throw new IllegalArgumentException("unsupport Object[" + source + "]");
        }
    }

    public Object[] decode(ByteBuffer buffer) throws DecoderException {
        int start = buffer.position();

        List<Object> items = new LinkedList<Object>();

        try {
            while (buffer.remaining() > 0) {
                items.add(readObject(buffer));
            }
        } catch (Exception e) {
        } finally {
            buffer.position(start);
        }

        return items.toArray();
    }

    private Object readObject(ByteBuffer data) throws DecoderException {
        int valueType = data.get();
        final Object value;
        switch (valueType) {
            case AMFType.DT_NUMBER:
                value = readNumber(data);
                break;
            case AMFType.DT_BOOLEAN:
                value = readBoolean(data);
                break;
            case AMFType.DT_STRING:
                value = readString(data);
                break;
            case AMFType.DT_OBJECT:
                value = readEcmaObject(data);
                break;
            case AMFType.DT_NULL:
                value = readNull(data);
                break;
            case AMFType.DT_ECMA_ARRAY:
                value = readEcmaArray(data);
                break;
            case AMFType.DT_ARRAY:
                value = readStrictArray(data);
                break;
            case AMFType.DT_DATETIME:
                value = readTimestamp(data);
                break;
            case AMFType.DT_LONGSTRIING:
                value = readLongString(data);
                break;
            case AMFType.DT_END:
                value = AMFType.END;
                break;
            case AMFType.DT_REFERENCE:
            case AMFType.DT_UNDEFINED:
            default:
                throw new UnsupportedAMFTypeException(valueType, data);
        }

        return value;
    }

    private AMFArray readEcmaArray(ByteBuffer data) {
        int size = data.getInt();
        
        AMFArray array = (AMFArray) setProperties(data, new AMFArray(size), Integer.MAX_VALUE);

        return array;
    }
    
    private AMFObject readEcmaObject(ByteBuffer data) {
        return (AMFObject) setProperties(data, new AMFObject(), Integer.MAX_VALUE);
    }
    
    private DynamicObject setProperties(ByteBuffer data, DynamicObject object, int maxElementNum) {
        String key = null;
        Object value = null;
        try {
            for (int i = 0; i < maxElementNum; i++) {
                if (data.remaining() <= 0)  {
                    break;
                }
                
                key = null;
                key = readString(data);
                value = readObject(data);

                // 009 结尾
                if (key.length() == 0 && AMFType.END == value) {
                    break;
                } else {
                    object.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return object;
    }

    private double readNumber(ByteBuffer data) {
        if (data.remaining() >= 8) {
            long number = ByteUtils.readUInt64(data);
            return Double.longBitsToDouble(number);
        } else {
            long v = 0;
            while(data.remaining() > 0) {
                v = v << 8 + ByteUtils.readUInt8(data);
            }
            return v;
        }
        
    }

    private boolean readBoolean(ByteBuffer data) {
        return 0 != data.get();
    }

    private String readString(ByteBuffer data) {
        int length = (int) ByteUtils.readUInt16(data);
        if (length <= 0) {
            return "";
        } else {
            byte[] buffer = new byte[Math.min(length, data.remaining())];
            data.get(buffer);
            return new String(buffer);
        }
    }


    private Object readNull(ByteBuffer data) {
        return null;
    }


    private Object[] readStrictArray(ByteBuffer data) {
        long arraySize = ByteUtils.readUInt32(data);

        Object[] array = new Object[(int) arraySize];
        
        try {
            for (int i = 0; i < arraySize; i++) {
                array[i] = readObject(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return array;
    }

    private String readLongString(ByteBuffer data) {
        int length = (int) data.getInt();
        if (length < 0) {
            return "";
        } else {
            byte[] buffer = new byte[length];
            data.get(buffer);
            return new String(buffer);
        }
    }

    private Timestamp readTimestamp(ByteBuffer data) {
        long datetime = (long) data.getDouble();

        // Local time offset in minutes from UTC. For
        // time zones located west of Greenwich, UK
        // this value is a negative number. Time zone
        // east of Greenwich, UK, are positive.
        long metaDataTimeOffset = data.getShort() * 60;
        long systemTimeOffset = TimeZone.getDefault().getRawOffset() / 1000;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(datetime);

        // 校正时间
        // 例如，把东三区的时间，改成东八区的时间
        calendar.add(Calendar.SECOND, (int) (systemTimeOffset - metaDataTimeOffset));
        return new java.sql.Timestamp(calendar.getTimeInMillis());
    }
}
