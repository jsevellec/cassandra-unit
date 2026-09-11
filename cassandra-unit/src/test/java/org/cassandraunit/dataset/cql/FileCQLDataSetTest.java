package org.cassandraunit.dataset.cql;

import org.cassandraunit.dataset.AbstractFileDataSetTest;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.ParseException;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class FileCQLDataSetTest extends AbstractFileDataSetTest {

	@Override
	public String getDataSetClasspathRessource() {
		return "/cql/simple.cql";
	}

	@Test
	public void shouldGetACQLDataSet() {

		CQLDataSet dataSet = new FileCQLDataSet(super.targetDataSetPathFileName);
		assertThat(dataSet).isNotNull();
	}

	@Test
	public void shouldNotGetACQLDataSetBecauseNull() {
		assertThatThrownBy(() -> new FileCQLDataSet(null))
				.isInstanceOf(ParseException.class);
	}

	@Test
	public void shouldNotGetACQLDataSetBecauseOfFileNotFound() {
		assertThatThrownBy(() -> new FileCQLDataSet("/notfound.cql"))
				.isInstanceOf(ParseException.class);
	}

}
