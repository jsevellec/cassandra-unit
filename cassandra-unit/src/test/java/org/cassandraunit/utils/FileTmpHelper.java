package org.cassandraunit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileTmpHelper {

    /**
     * Copies a classpath dataset out to a real file, so that the file-based dataset loaders can
     * be tested. The directory is created fresh per JVM and deleted on exit - the previous
     * implementation wrote to a fixed path under java.io.tmpdir and never cleaned up.
     */
    public static String copyClassPathDataSetToTmpDirectory(Class<?> c, String initialClasspathDataSetLocation)
            throws IOException {
        String dataSetFileName =
                initialClasspathDataSetLocation.substring(initialClasspathDataSetLocation.lastIndexOf('/') + 1);

        Path tmpDir = Files.createTempDirectory("cassandra-unit-dataset");
        tmpDir.toFile().deleteOnExit();
        Path target = tmpDir.resolve(dataSetFileName);
        target.toFile().deleteOnExit();

        try (InputStream dataSet = c.getResourceAsStream(initialClasspathDataSetLocation)) {
            if (dataSet == null) {
                throw new IOException("No such classpath dataset: " + initialClasspathDataSetLocation);
            }
            Files.copy(dataSet, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target.toString();
    }
}
