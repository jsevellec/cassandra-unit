package org.cassandraunit.dataset;

import java.util.List;

/**
 * @author Jeremy Sevellec
 */
public interface CQLDataSet {

    List<String> getCQLStatements();

    String getKeyspaceName();

    boolean isKeyspaceCreation();

    boolean isKeyspaceDeletion();
}
