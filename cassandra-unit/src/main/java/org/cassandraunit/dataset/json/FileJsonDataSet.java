package org.cassandraunit.dataset.json;

import org.cassandraunit.dataset.DataSet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileJsonDataSet extends AbstractJsonDataSet implements DataSet {

    public FileJsonDataSet(String dataSetLocation) {
        super(dataSetLocation);
    }

    @Override
    protected InputStream getInputDataSetLocation(String dataSetLocation) {
        if (dataSetLocation == null) {
            return null;
        }
        try {
            return new FileInputStream(dataSetLocation);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

}
