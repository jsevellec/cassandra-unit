package org.cassandraunit.dataset.cql;

import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.ClassPathDataSetSource;

import java.io.InputStream;

/**
 * @author Jeremy Sevellec
 */
public class ClassPathCQLDataSet extends AbstractCQLDataSet implements CQLDataSet {

    public ClassPathCQLDataSet(String dataSetLocation) {
        super(dataSetLocation, true, true, null);
    }

    public ClassPathCQLDataSet(String dataSetLocation, boolean keyspaceCreation) {
      super(dataSetLocation, keyspaceCreation, true);
    }

    public ClassPathCQLDataSet(String dataSetLocation, boolean keyspaceCreation, boolean keyspaceDeletion) {
      super(dataSetLocation, keyspaceCreation, keyspaceDeletion);
    }

    public ClassPathCQLDataSet(String dataSetLocation, String keyspaceName) {
      super(dataSetLocation, true, true , keyspaceName);
    }

    public ClassPathCQLDataSet(String dataSetLocation, boolean keyspaceCreation, String keyspaceName) {
      super(dataSetLocation, keyspaceCreation, true, keyspaceName);
    }

    public ClassPathCQLDataSet(String dataSetLocation, boolean keyspaceCreation, boolean keyspaceDeletion, String keyspaceName) {
        super(dataSetLocation, keyspaceCreation, keyspaceDeletion, keyspaceName);
    }

    @Override
    protected InputStream getInputDataSetLocation(String dataSetLocation) {
        // getClass(), not ClassPathDataSetSource.class: a subclass loaded by a different
        // classloader resolved against its own before this delegation existed, and still should.
        return new ClassPathDataSetSource(dataSetLocation, getClass()).open();
    }
}
