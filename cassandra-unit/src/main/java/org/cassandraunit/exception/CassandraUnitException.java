package org.cassandraunit.exception;

public class CassandraUnitException extends RuntimeException {

	private static final long serialVersionUID = 3823886777280016967L;

	public CassandraUnitException(String message, Throwable cause) {
        super(message, cause);
    }

    public CassandraUnitException(String message) {
        super(message);
    }

}
