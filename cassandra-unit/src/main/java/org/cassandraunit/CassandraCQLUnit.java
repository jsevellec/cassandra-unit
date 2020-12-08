package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

/**
 * @author Marcin Szymaniuk
 * @author Jeremy Sevellec
 * @author David Webb
 */
public class CassandraCQLUnit extends BaseCassandraUnit {

	private CQLDataSet dataSet;
	public CqlSession session;

	public CassandraCQLUnit(CQLDataSet dataSet) {
		this.dataSet = dataSet;
	}

	public CassandraCQLUnit(CQLDataSet dataSet, int readTimeoutMillis) {
		this.dataSet = dataSet;
		this.readTimeoutMillis = readTimeoutMillis;
	}

	public CassandraCQLUnit(CQLDataSet dataSet, String configurationFileName) {
		this(dataSet);
		this.configurationFileName = configurationFileName;
	}

	public CassandraCQLUnit(CQLDataSet dataSet, String configurationFileName, int readTimeoutMillis) {
		this(dataSet);
		this.configurationFileName = configurationFileName;
		this.readTimeoutMillis = readTimeoutMillis;
	}

	// The former constructors with hostip and port have been removed. Now host+port is directly read out of the provided
	// configurationFile(Name). You may also use EmbeddedCassandraServerHelper.CASSANDRA_RNDPORT_YML_FILE to select
	// random (free) ports for EmbeddedCassandra, such that you can start multiple embedded cassandras on the same host
	// (but not in the same JVM).

	public CassandraCQLUnit(CQLDataSet dataSet, String configurationFileName, long startUpTimeoutMillis) {
		super(startUpTimeoutMillis);
		this.dataSet = dataSet;
		this.configurationFileName = configurationFileName;
	}

	public CassandraCQLUnit(CQLDataSet dataSet, String configurationFileName, long startUpTimeoutMillis, int readTimeoutMillis) {
		super(startUpTimeoutMillis);
		this.dataSet = dataSet;
		this.configurationFileName = configurationFileName;
		this.readTimeoutMillis = readTimeoutMillis;
	}

	@Override
	protected void load() {
		session = EmbeddedCassandraServerHelper.getSession();
		CQLDataLoader dataLoader = new CQLDataLoader(session);
		dataLoader.load(dataSet);
		session = dataLoader.getSession();
	}

	@Override
	protected void after() {
		super.after();
	}

	// Getters for those who do not like to directly access fields

	public CqlSession getSession() {
		return session;
	}
}
