package org.cassandraunit.dataset;

import java.io.InputStream;

/**
 * A dataset resolved as a classpath resource.
 *
 * @author Jeremy Sevellec
 */
public class ClassPathDataSetSource implements DataSetSource {

    private final String location;
    private final Class<?> context;

    public ClassPathDataSetSource(String location) {
        this(location, ClassPathDataSetSource.class);
    }

    /**
     * @param context the class whose classloader resolves the resource. {@code ClassPathCQLDataSet}
     *                passes its own runtime class, which is what it used before this type existed -
     *                it matters only when a subclass is loaded by a different classloader.
     */
    public ClassPathDataSetSource(String location, Class<?> context) {
        this.location = location;
        this.context = context;
    }

    @Override
    public InputStream open() {
        if (location == null) {
            return null;
        }
        return context.getResourceAsStream("/" + location);
    }

    @Override
    public String describe() {
        return "classpath:" + location;
    }
}
