package org.cassandraunit.dataset;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * A dataset resolved as a filesystem path.
 *
 * @author Jeremy Sevellec
 */
public class FileDataSetSource implements DataSetSource {

    private final String location;

    public FileDataSetSource(String location) {
        this.location = location;
    }

    @Override
    public InputStream open() {
        if (location == null) {
            return null;
        }
        try {
            return new FileInputStream(location);
        } catch (FileNotFoundException e) {
            // Absence is reported as null, the contract open() shares with getResourceAsStream;
            // the caller turns it into a ParseException naming the location.
            return null;
        }
    }

    @Override
    public String describe() {
        return "file:" + location;
    }
}
