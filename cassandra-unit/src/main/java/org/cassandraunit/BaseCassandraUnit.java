package org.cassandraunit;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.rules.ExternalResource;

/**
 * @author Marcin Szymaniuk
 */
public abstract class BaseCassandraUnit extends ExternalResource {

	protected String configurationFileName;
	protected long startupTimeoutMillis;
	protected int readTimeoutMillis = 12000;

	public BaseCassandraUnit() {
		this(EmbeddedCassandraServerHelper.DEFAULT_STARTUP_TIMEOUT);
	}

	public BaseCassandraUnit(long startupTimeoutMillis) {
		this.startupTimeoutMillis = startupTimeoutMillis;
	}

	@Override
	protected void before() throws Exception {
		/* start an embedded Cassandra */
		if (configurationFileName != null) {
			EmbeddedCassandraServerHelper.startEmbeddedCassandra(configurationFileName, startupTimeoutMillis);
		} else {
			EmbeddedCassandraServerHelper.startEmbeddedCassandra(startupTimeoutMillis);
		}

		/* create structure and load data */
		load();
	}

	protected abstract void load();

	/**
	 * Gets a base SocketOptions with an overridden readTimeoutMillis.
	 */
}
