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
        if (getInputDataSetLocation(dataSetLocation) == null) {
            throw new ParseException("Dataset not found");
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
        List<String> lines = getLines();
        return linesToCQLStatements(lines);
    }

    private List<String> linesToCQLStatements(List<String> lines) {
        List<String> statements = new ArrayList<String>();
        StringBuffer statementUnderConstruction = new StringBuffer();
        for (String line : lines) {
            statementUnderConstruction.append(line.trim());
            if (endOfStatementLine(line)) {
                statements.add(statementUnderConstruction.toString());
                statementUnderConstruction.setLength(0);
            } else {
                statementUnderConstruction.append(" ");
            }
        }
        return statements;
    }

//    private boolean spaceNeededAfter(String line) {
//        boolean spaceNeeded = true;
//        String[] characterWithoutSpaceNeededAfter = {"<", ">", ":", "=", "|", "("};
//        if (StringUtils.endsWithAny(line, characterWithoutSpaceNeededAfter)) {
//            spaceNeeded = false;
//        }
//        return spaceNeeded;
//    }

    private boolean endOfStatementLine(String line) {
        return line.endsWith(END_OF_STATEMENT_DELIMITER);
    }

    public List<String> getLines() {
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

    public boolean isKeyspaceCreation() {
        return keyspaceCreation;
    }

    public boolean isKeyspaceDeletion() {
      return keyspaceDeletion;
    }
}
