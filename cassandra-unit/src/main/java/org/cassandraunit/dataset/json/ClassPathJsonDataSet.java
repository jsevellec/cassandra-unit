package org.cassandraunit.dataset.json;

import java.io.InputStream;

/**
 * @author Jeremy Sevellec
 */
public class ClassPathJsonDataSet extends AbstractJsonDataSet {

    public ClassPathJsonDataSet(String dataSetLocation) {
        super(dataSetLocation);
    }

    protected InputStream getInputDataSetLocation(String dataSetLocation) {
        InputStream inputDataSetLocation = this.getClass().getResourceAsStream("/" + dataSetLocation);
        return inputDataSetLocation;
    }

}
