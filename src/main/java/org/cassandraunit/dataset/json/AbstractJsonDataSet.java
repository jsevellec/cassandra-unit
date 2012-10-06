package org.cassandraunit.dataset.json;

import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.commons.AbstractCommonsParserDataSet;
import org.cassandraunit.dataset.commons.ParsedKeyspace;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractJsonDataSet extends AbstractCommonsParserDataSet {

    protected String dataSetLocation = null;

    public AbstractJsonDataSet(String dataSetLocation) {
        this.dataSetLocation = dataSetLocation;
        if (getInputDataSetLocation(dataSetLocation) == null) {
            throw new ParseException("Dataset not found");
        }
    }

    protected ParsedKeyspace getParsedKeyspace() {
        InputStream inputDataSetLocation = getInputDataSetLocation(dataSetLocation);
        if (inputDataSetLocation == null) {
            throw new ParseException("Dataset not found");
        }

        ObjectMapper jsonMapper = new ObjectMapper();
        try {
            return jsonMapper.readValue(inputDataSetLocation, ParsedKeyspace.class);
        } catch (JsonParseException e) {
            throw new ParseException(e);
        } catch (JsonMappingException e) {
            throw new ParseException(e);
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    protected abstract InputStream getInputDataSetLocation(String dataSetLocation);

}
