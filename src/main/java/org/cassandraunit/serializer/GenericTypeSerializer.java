package org.cassandraunit.serializer;

import me.prettyprint.cassandra.serializers.*;
import me.prettyprint.hector.api.beans.Composite;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.cassandraunit.exception.CassandraUnitException;
import org.cassandraunit.type.GenericType;
import org.cassandraunit.type.GenericTypeEnum;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;

/**
 * @author Jeremy Sevellec
 */
public class GenericTypeSerializer extends AbstractSerializer<GenericType> {

    private static final GenericTypeSerializer instance = new GenericTypeSerializer();

    public static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HHmmss");

    public static GenericTypeSerializer get() {
        return instance;
    }

    @Override
    public GenericType fromByteBuffer(ByteBuffer byteBuffer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ByteBuffer toByteBuffer(GenericType genericType) {
        ByteBuffer byteBuffer = null;

        GenericTypeEnum currentType = genericType.getType();

        if (currentType == null) {
            currentType = GenericTypeEnum.BYTES_TYPE;
        }

        if (currentType == null) {

        } else {
            String genericValue = genericType.getValue();

            switch (genericType.getType()) {

                case BOOLEAN_TYPE:
                    byteBuffer = BooleanSerializer.get().toByteBuffer(Boolean.parseBoolean(genericValue));
                    break;
                case BYTES_TYPE:
                    byte[] hexDecodedBytes;
                    try {
                        if (genericValue.isEmpty()) {
                            byteBuffer = ByteBufferSerializer.get().fromBytes(new byte[0]);
                        } else {
                            hexDecodedBytes = Hex.decodeHex(genericValue.toCharArray());
                            byteBuffer = ByteBufferSerializer.get().fromBytes(hexDecodedBytes);
                        }
                    } catch (DecoderException e) {
                        throw new CassandraUnitException("cannot parse \"" + genericValue + "\" as hex bytes", e);
                    }
                    break;
                case DATE_TYPE:
                    try {
                        byteBuffer = DateSerializer.get().toByteBuffer(dateFormat.parse(genericValue));
                    } catch (ParseException e) {
                        throw new CassandraUnitException("cannot parse \"" + genericValue + "\" as date", e);
                    }
                    break;
                case DOUBLE_TYPE:
                    byteBuffer = DoubleSerializer.get().toByteBuffer(Double.parseDouble(genericValue));
                    break;
                case FLOAT_TYPE:
                    byteBuffer = FloatSerializer.get().toByteBuffer(Float.parseFloat(genericValue));
                    break;
                case INTEGER_TYPE:
                    int val = Integer.parseInt(genericValue);
                    byteBuffer = IntegerSerializer.get().toByteBuffer(val);
                    break;
                case LEXICAL_UUID_TYPE:
                    byteBuffer = UUIDSerializer.get().toByteBuffer(UUID.fromString(genericValue));
                    break;
                case LONG_TYPE:
                    byteBuffer = LongSerializer.get().toByteBuffer(Long.parseLong(genericValue));
                    break;
                case TIME_UUID_TYPE:
                    byteBuffer = UUIDSerializer.get().toByteBuffer(UUID.fromString(genericValue));
                    break;
                case ASCII_TYPE:
                case UTF_8_TYPE:
                    byteBuffer = StringSerializer.get().toByteBuffer(genericValue);
                    break;
                case UUID_TYPE:
                    byteBuffer = UUIDSerializer.get().toByteBuffer(UUID.fromString(genericValue));
                    break;
                case COUNTER_TYPE:
                    byteBuffer = LongSerializer.get().toByteBuffer(Long.parseLong(genericValue));
                    break;
                case COMPOSITE_TYPE:
                    byteBuffer = new CompositeSerializer().toByteBuffer(createComposite(genericType));
                    break;
                default:
                    byteBuffer = BytesArraySerializer.get().toByteBuffer(genericValue.getBytes());
                    break;
            }
        }
        return byteBuffer;
    }

    private Composite createComposite(GenericType genericType) {
        if (!GenericTypeEnum.COMPOSITE_TYPE.equals(genericType.getType())) {
            throw new IllegalArgumentException("the generricType must be a CompositeType");
        }

        Composite composite = new Composite();
        for (int i = 0; i < genericType.getCompositeValues().length; i++) {
            composite.addComponent(
                    new GenericType(genericType.getCompositeValues()[i],
                            genericType.getTypesBelongingCompositeType()[i]), GenericTypeSerializer.get());
        }
        return composite;

    }
}
