package org.cassandraunit.dataset.xml;

import java.io.InputStream;

public class ClassPathXmlDataSet extends AbstractXmlDataSet {

    public ClassPathXmlDataSet(String dataSetLocation) {
        super(dataSetLocation);
    }

    @Override
    protected InputStream getInputDataSetLocation(String dataSetLocation) {
        InputStream inputDataSetLocation = this.getClass().getResourceAsStream("/" + dataSetLocation);
        return inputDataSetLocation;
    }

}
