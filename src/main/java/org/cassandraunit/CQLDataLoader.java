package org.cassandraunit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Marcin Szymaniuk
 */
public class CQLDataLoader {

    private static final Logger log = LoggerFactory.getLogger(CQLDataLoader.class);
    private Cluster cluster;
    private String keyspaceName;

    public CQLDataLoader(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    public void load(String cqlFile,Session session) {
        try {
            final InputStream inputStream = getClass().getResourceAsStream(cqlFile);
            final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(inputStreamReader);
            String line;
            while ((line = br.readLine()) != null) {
                session.execute(line);
            }
            br.close();

        } catch (IOException e) {
            log.error(e.getMessage(),e);
        }
    }


}
