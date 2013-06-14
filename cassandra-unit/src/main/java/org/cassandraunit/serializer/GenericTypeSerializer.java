package org.cassandraunit.serializer;

import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.DateSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.FloatSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.beans.Composite;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.cassandraunit.exception.CassandraUnitException;
import org.cassandraunit.type.GenericType;
import org.cassandraunit.type.GenericTypeEnum;
import org.cassandraunit.utils.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author Jeremy Sevellec
 */
public class GenericTypeSerializer extends AbstractSerializer<GenericType> {

    private static final GenericTypeSerializer instance = new GenericTypeSerializer();
    private static final Pattern hexPattern = Pattern.compile("[0-9abcdefABCDEF]+");
    private static final Pattern base64Pattern = Pattern.compile("[0-9a-zA-Z/+]+={0,3}");

    public static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HHmmss");

    private Logger logger = LoggerFactory.getLogger(this.getClass());


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
                    byte[] decodedBytes;
                    try {
                        if (genericValue.isEmpty()) {
                            byteBuffer = ByteBufferSerializer.get().fromBytes(new byte[0]);
                        } else if (isHex(genericValue)) {
                            decodedBytes = Hex.decodeHex(genericValue.toCharArray());
                            byteBuffer = ByteBufferSerializer.get().fromBytes(decodedBytes);
                        } else if (isBase64(genericValue)) {
                            decodedBytes = Base64.decode(genericValue);
                            byteBuffer = ByteBufferSerializer.get().fromBytes(decodedBytes);
                        } else {
                            throw new CassandraUnitException("Failed to parse \"" + genericValue + "\" as bytes: unknown binary encoding.");
                        }
                    } catch (DecoderException e) {
                        throw new CassandraUnitException("Failed to parse \"" + genericValue + "\" as bytes", e);
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

    private boolean isHex(String genericValue) {
        return hexPattern.matcher(genericValue).matches();
    }

    private boolean isBase64(String genericValue) {
        if (!(genericValue.length() % 4 == 0))
            return false;
        return base64Pattern.matcher(genericValue).matches();
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