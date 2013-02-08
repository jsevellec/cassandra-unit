package org.cassandraunit.utils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import me.prettyprint.hector.api.ddl.ComparatorType;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.type.GenericTypeEnum;
import org.junit.Test;

/**
 * @author Jeremy Sevellec
 * @author Marc Carre (#27)
 */
public class ComparatorTypeHelperTest {

	@Test
	public void shouldPassTheVerificationWithACompositeType() {
		try {
			ComparatorTypeHelper.verifyAndExtract("CompositeType(UTF8Type)");
		} catch (ParseException e) {
			fail();
		}
	}

	@Test
	public void shouldPassTheVerificationWithASimpleType() {
		try {
			ComparatorTypeHelper.verifyAndExtract("UTF8Type");
		} catch (ParseException e) {
			fail();
		}
	}

	@Test(expected = ParseException.class)
	public void shouldGetAnExceptionBecauseOfUnknownSimpleType() throws Exception {
		ComparatorTypeHelper.verifyAndExtract("UNKOWN");
	}

	@Test(expected = ParseException.class)
	public void shouldGetAnExceptionBecauseOfUnknownCompositeType() throws Exception {
		ComparatorTypeHelper.verifyAndExtract("CompositeType(UNKNOWN)");
	}

	@Test(expected = ParseException.class)
	public void shouldGetAnExceptionBecauseOfUnknownCompositeTypeAndGoodType() throws Exception {
		ComparatorTypeHelper.verifyAndExtract("CompositeType(UTF8Type,UNKNOWN)");
	}

	@Test(expected = ParseException.class)
	public void shouldGetAnExceptionBecauseOfCompositeTypeWithoutStartingWithAParenthesis() throws Exception {
		ComparatorTypeHelper.verifyAndExtract("CompositeTypeUTF8Type,LongType)");
	}

	@Test(expected = ParseException.class)
	public void shouldGetAnExceptionBecauseOfCompositeTypeWithoutEndingWithAParenthesis() throws Exception {
		ComparatorTypeHelper.verifyAndExtract("CompositeTypeUTF8Type,LongType");
	}

	@Test
	public void shouldExtractGenericTypesFromTypeAlias() throws Exception {
		GenericTypeEnum[] genericTypesEnum = ComparatorTypeHelper
				.extractGenericTypesFromTypeAlias("(LongType,UTF8Type,IntegerType)");
		assertThat(genericTypesEnum, is(new GenericTypeEnum[] { GenericTypeEnum.LONG_TYPE, GenericTypeEnum.UTF_8_TYPE,
				GenericTypeEnum.INTEGER_TYPE }));
	}

    @Test
    public void shouldExtractReversedType() {
        ComparatorType typeWithReversedTrue = ComparatorTypeHelper.verifyAndExtract("UTF8Type(reversed=true)");
        assertThat(typeWithReversedTrue, is(ComparatorType.UTF8TYPE));

        ComparatorType typeWithReversedFalse = ComparatorTypeHelper.verifyAndExtract("UTF8Type(reversed=false)");
        assertThat(typeWithReversedFalse, is(ComparatorType.UTF8TYPE));
    }

    @Test
    public void shouldExtractReversedCompositeType() {
        ComparatorType typesWithReversedTrue = ComparatorTypeHelper.verifyAndExtract("CompositeType(LongType(reversed=true),UTF8Type)");
        assertThat(typesWithReversedTrue, is(ComparatorType.COMPOSITETYPE));

        ComparatorType typesWithReversedFalse = ComparatorTypeHelper.verifyAndExtract("CompositeType(LongType(reversed=false),UTF8Type)");
        assertThat(typesWithReversedFalse, is(ComparatorType.COMPOSITETYPE));
    }
}
