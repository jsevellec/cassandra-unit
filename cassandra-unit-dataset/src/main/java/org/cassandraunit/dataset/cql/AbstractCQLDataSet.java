package org.cassandraunit.dataset.cql;

import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jeremy Sevellec
 */
public abstract class AbstractCQLDataSet implements CQLDataSet {

    public static final String END_OF_STATEMENT_DELIMITER = ";";
    private String dataSetLocation = null;
    private String keyspaceName = null;
    private boolean keyspaceCreation = true;
    private boolean keyspaceDeletion = true;

    public AbstractCQLDataSet(String dataSetLocation) {
        this.dataSetLocation = dataSetLocation;
    }

    public AbstractCQLDataSet(String dataSetLocation, boolean keyspaceCreation, boolean keyspaceDeletion) {
        this(dataSetLocation, keyspaceCreation, keyspaceDeletion, null);
    }

    public AbstractCQLDataSet(String dataSetLocation, String keyspaceName) {
        this(dataSetLocation, true, true, keyspaceName);
    }

    public AbstractCQLDataSet(String dataSetLocation, boolean keyspaceCreation, boolean keyspaceDeletion, String keyspaceName) {
        // Only opened to check the dataset exists, so close it again - for FileCQLDataSet this
        // is a real FileInputStream, and it used to be discarded still open.
        try (InputStream probe = getInputDataSetLocation(dataSetLocation)) {
            if (probe == null) {
                throw new ParseException("Dataset not found: " + dataSetLocation);
            }
        } catch (IOException e) {
            throw new ParseException(e);
        }
        this.dataSetLocation = dataSetLocation;
        this.keyspaceCreation = keyspaceCreation;
        this.keyspaceDeletion = keyspaceDeletion;
        if (keyspaceName != null) {
            this.keyspaceName = keyspaceName.toLowerCase();
        }
    }

    protected abstract InputStream getInputDataSetLocation(String dataSetLocation);

    @Override
    public List<String> getCQLStatements() {
        return linesToCQLStatements(getLines());
    }

    private List<String> linesToCQLStatements(List<String> lines) {
    	SimpleCQLLexer lexer = new SimpleCQLLexer(lines);
    	return lexer.getStatements();
    }

    public List<String> getLines() {
        InputStream inputStream = getInputDataSetLocation(dataSetLocation);
        if (inputStream == null) {
            throw new ParseException("Dataset not found: " + dataSetLocation);
        }
        // UTF-8 explicitly: the platform default made the same dataset parse differently on
        // different machines, which is issue #144.
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return buffer.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    @Override
    public String getKeyspaceName() {
        return keyspaceName;
    }

    public boolean isKeyspaceCreation() {
        return keyspaceCreation;
    }

    public boolean isKeyspaceDeletion() {
      return keyspaceDeletion;
    }
}
