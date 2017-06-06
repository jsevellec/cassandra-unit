package org.cassandraunit;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.PrintStream;
import org.apache.cassandra.cli.CliMain;
import org.cassandraunit.exception.CassandraUnitException;

public class CassandraCliUnit extends BaseCassandraUnit {

	protected final boolean turnOffCassandraCliLogging;

	protected String host = "localhost";

	protected int port = 9171;

	protected final String filePath;

	public CassandraCliUnit(boolean turnOffCassandraCliLogging, String host, int port, String filePath) {
		this(turnOffCassandraCliLogging, filePath);
		this.host = host;
		this.port = port;
	}

	public CassandraCliUnit(boolean turnOffCassandraCliLogging, String filePath) {
		super();
		this.turnOffCassandraCliLogging = turnOffCassandraCliLogging;
		this.filePath = filePath;
	}

	/**
	 * The filePath is the relative path from the root of the project folder.
	 * 
	 * @param filePath
	 */
	public CassandraCliUnit(String filePath) {
		this(true, filePath);
	}

	@Override
	protected void load() {
		String[] args = new String[]{"-h" + host, "-p" + port, "-f" + filePath};
		try {
			if (turnOffCassandraCliLogging) {
				CliMain.sessionState.setOut(new PrintStream(ByteStreams.nullOutputStream()));
			}
			CliMain.main(args);
		} catch (IOException e) {
			throw new CassandraUnitException("failed to execute cassandra command line file", e);
		}
	}
}
