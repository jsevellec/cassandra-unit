package org.cassandraunit.dataset.cql;

import org.apache.commons.lang.StringUtils;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeremy Sevellec
 */
public abstract class AbstractCQLDataSet implements CQLDataSet {

    private String dataSetLocation = null;
    private String keyspaceName = null;

    public AbstractCQLDataSet(String dataSetLocation) {
        this(dataSetLocation,"cassandraunittestkeyspace");
    }

    public AbstractCQLDataSet(String dataSetLocation, String keyspaceName) {
        if (keyspaceName == null || keyspaceName.isEmpty()) {
            throw new IllegalArgumentException("keyspaceName can't be null or empty");
        }
        if (getInputDataSetLocation(dataSetLocation) == null) {
            throw new ParseException("Dataset not found");
        }
        this.keyspaceName = keyspaceName.toLowerCase();
        this.dataSetLocation = dataSetLocation;
    }


    protected abstract InputStream getInputDataSetLocation(String dataSetLocation);

    @Override
    public List<String> getCQLQueries() {
        InputStream inputStream = getInputDataSetLocation(dataSetLocation);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(inputStreamReader);
        String line;
        List<String> cqlQueries = new ArrayList();
        try {
            while ((line = br.readLine()) != null) {
              if (StringUtils.isNotBlank(line)) {
                cqlQueries.add(line);
              }
            }
            br.close();
            return cqlQueries;
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    @Override
    public String getKeyspaceName() {
        return keyspaceName;
    }
}
