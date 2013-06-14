package org.cassandraunit.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import me.prettyprint.hector.api.ddl.ComparatorType;

import org.cassandraunit.type.GenericType;
import org.cassandraunit.type.GenericTypeEnum;
import org.junit.Test;

/**
 * 
 * @author Jeremy Sevellec
 *
 */
public class TypeExtractorTest {

	@Test
	public void shouldExtractUTF8Type() {
		verifyExtration("utf8(data)", "data", GenericTypeEnum.UTF_8_TYPE);
	}

	@Test
	public void shouldExtractBytesType() {
		verifyExtration("bytes(data)", "data", GenericTypeEnum.BYTES_TYPE);
	}

	@Test
	public void shouldExtractIntegerType() {
		verifyExtration("integer(1)", "1", GenericTypeEnum.INTEGER_TYPE);
	}

	@Test
	public void shouldExtractLexicalUUIDType() {
		verifyExtration("lexicaluuid(13816710-1dd2-11b2-879a-782bcb80ff6a)", "13816710-1dd2-11b2-879a-782bcb80ff6a",
				GenericTypeEnum.LEXICAL_UUID_TYPE);
	}

	@Test
	public void shouldExtractLongType() {
		verifyExtration("long(12)", "12", GenericTypeEnum.LONG_TYPE);
	}

	@Test
	public void shouldExtractTimeUUIDType() {
		verifyExtration("timeuuid(13816710-1dd2-11b2-879a-782bcb80ff6a)", "13816710-1dd2-11b2-879a-782bcb80ff6a",
				GenericTypeEnum.TIME_UUID_TYPE);
	}

	@Test
	public void shouldExtractUUIDType() {
		verifyExtration("uuid(13816710-1dd2-11b2-879a-782bcb80ff6a)", "13816710-1dd2-11b2-879a-782bcb80ff6a",
				GenericTypeEnum.UUID_TYPE);
	}

	private void verifyExtration(String valueToExtract, String extractedValueToHave, GenericTypeEnum typeToHave) {
		GenericType extractedValue = TypeExtractor.extract(valueToExtract, null);
		assertThat(extractedValue, is(notNullValue()));
		assertThat(extractedValue.getType(), is(typeToHave));
		assertThat(extractedValue.getValue(), is(extractedValueToHave));
	}

	@Test
	public void shouldExtractDefaultUTF8Type() {
		ComparatorType defaultValueType = ComparatorType.UTF8TYPE;
		String valueToExtract = "myutf8value";
		GenericType extractedValue = TypeExtractor.extract(valueToExtract, defaultValueType);
		assertThat(extractedValue, is(notNullValue()));
		assertThat(extractedValue.getType(), is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(extractedValue.getValue(), is("myutf8value"));
	}

	@Test
	public void shouldExtractDefaultLongType() {
		ComparatorType defaultValueType = ComparatorType.LONGTYPE;
		String valueToExtract = "12";
		GenericType extractedValue = TypeExtractor.extract(valueToExtract, defaultValueType);
		assertThat(extractedValue, is(notNullValue()));
		assertThat(extractedValue.getType(), is(GenericTypeEnum.LONG_TYPE));
		assertThat(extractedValue.getValue(), is("12"));
	}

	@Test
	public void shouldExtractWithNoFunctionDefined() {
		verifyExtration("data", "data", GenericTypeEnum.BYTES_TYPE);
	}

	@Test
	public void shouldExtractWithInvalidFunctionDefined() {
		verifyExtration("aaa(data", "aaa(data", GenericTypeEnum.BYTES_TYPE);
	}

    @Test
    public void shouldExtractWithParenthesisInside() {
        verifyExtration("utf8(rgb(0,0,0))", "rgb(0,0,0)",
                GenericTypeEnum.UTF_8_TYPE);
    }

    @Test
    public void shouldExtractWithALotOfParenthesisInside() {
        verifyExtration("utf8((()))))", "(())))",
                GenericTypeEnum.UTF_8_TYPE);
    }
}
