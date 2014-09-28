package org.cassandraunit.dataset;

/**
 * @author Jeremy Sevellec
 */
public class ParseException extends RuntimeException {

	private static final long serialVersionUID = -8528027860660267501L;

	public ParseException(Throwable e) {
        super(e);
    }

    public ParseException(String message) {
        super(message);
    }

}
