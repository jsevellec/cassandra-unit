package org.cassandraunit.dataset.yaml;

import java.io.InputStream;

public class ClassPathYamlDataSet extends AbstractYamlDataSet {

    public ClassPathYamlDataSet(String dataSetLocation) {
        super(dataSetLocation);
    }

    @Override
    protected InputStream getInputDataSetLocation(String dataSetLocation) {
        InputStream inputDataSetLocation = this.getClass().getResourceAsStream("/" + dataSetLocation);
        return inputDataSetLocation;
    }

}
