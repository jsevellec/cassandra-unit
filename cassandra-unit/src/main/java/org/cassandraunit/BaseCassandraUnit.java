package org.cassandraunit;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.rules.ExternalResource;

import java.time.Duration;

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

		/*
		 * Apply the read timeout before anything can create the shared session. This used to be
		 * accepted by three constructors, stored, and then never used at all - the session was
		 * built with a hardcoded request timeout of zero. Because there is one session per JVM
		 * it can only be set once, so the first rule to run in a JVM wins; setRequestTimeout
		 * warns rather than failing if it is already too late.
		 */
		EmbeddedCassandraServerHelper.setRequestTimeout(Duration.ofMillis(readTimeoutMillis));

		/* create structure and load data */
		load();
	}

	protected abstract void load();

	/*
	 * Deliberately no after(): cleanup is driven by the dataset, not by the rule. A CQLDataSet
	 * declares isKeyspaceDeletion/isKeyspaceCreation, and CQLDataLoader drops and recreates the
	 * keyspace on load accordingly, which is what isolates one test from the next. Dropping
	 * keyspaces here instead would break datasets that deliberately ask for their keyspace to be
	 * kept (see CQLDataLoadTestWithNoKeyspaceDeletion), and closing the session would break every
	 * other test in the JVM, since the session is shared. Call
	 * EmbeddedCassandraServerHelper.cleanEmbeddedCassandra() explicitly if you want a full wipe.
	 */
}
