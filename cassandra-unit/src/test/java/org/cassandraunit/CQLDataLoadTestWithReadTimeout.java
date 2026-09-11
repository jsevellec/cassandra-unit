package org.cassandraunit;

import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 
 * @author Jeremy Sevellec
 *
 */
public class CQLDataLoadTestWithReadTimeout {

	private static final int READ_TIMEOUT_VALUE = 15000;

	@Rule
	public CassandraCQLUnit cassandraCQLUnit = new CassandraCQLUnit(new ClassPathCQLDataSet("cql/simple.cql",
			"mykeyspace"), READ_TIMEOUT_VALUE);

	@Test
	public void testCQLDataAreInPlace() throws Exception {
		test();
	}

	@Test
	public void sameTestToMakeSureMultipleTestsAreFine() throws Exception {
		test();
	}

	/**
	 * The constructor's readTimeoutMillis used to be stored and then ignored - the session was
	 * always built with a hardcoded request timeout of zero - so this test passed without
	 * testing anything about the timeout at all.
	 */
	@Test
	public void readTimeoutShouldReachTheSession() {
		Duration configured = cassandraCQLUnit.session.getContext().getConfig()
				.getDefaultProfile().getDuration(DefaultDriverOption.REQUEST_TIMEOUT);

		assertThat(configured).isEqualTo(Duration.ofMillis(READ_TIMEOUT_VALUE));
	}

	private void test() {
		ResultSet result = cassandraCQLUnit.session
				.execute("select * from testCQLTable WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570737");

		String val = result.iterator().next().getString("value");
		assertThat(val).isEqualTo("Cql loaded string");
	}
}
