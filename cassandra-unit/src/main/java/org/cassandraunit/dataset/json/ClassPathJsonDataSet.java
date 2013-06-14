package org.cassandraunit.dataset.json;

import org.cassandraunit.dataset.DataSet;

import java.io.InputStream;

/**
 * @author Jeremy Sevellec
 */
public class ClassPathJsonDataSet extends AbstractJsonDataSet implements DataSet {

    public ClassPathJsonDataSet(String dataSetLocation) {
        super(dataSetLocation);
    }

    protected InputStream getInputDataSetLocation(String dataSetLocation) {
        InputStream inputDataSetLocation = this.getClass().getResourceAsStream("/" + dataSetLocation);
        return inputDataSetLocation;
    }

}
