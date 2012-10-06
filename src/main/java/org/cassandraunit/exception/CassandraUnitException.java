package org.cassandraunit.exception;

public class CassandraUnitException extends RuntimeException {

    public CassandraUnitException(String message, Throwable cause) {
        super(message, cause);
    }

}
