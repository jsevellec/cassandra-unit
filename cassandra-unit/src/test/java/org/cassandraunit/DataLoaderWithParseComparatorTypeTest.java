package org.cassandraunit;

import org.cassandraunit.dataset.json.ClassPathJsonDataSet;
import org.cassandraunit.dataset.xml.ClassPathXmlDataSet;
import org.junit.Test;

import static org.cassandraunit.utils.EmbeddedCassandraServerHelper.startEmbeddedCassandra;

public class DataLoaderWithParseComparatorTypeTest {

    @Test
    public void shouldParseJsonAndCreateKeyspaceWithComparatorTypeAndMetadatas() throws Exception {
        startEmbeddedCassandra();
        new DataLoader(CassandraUnit.clusterName, CassandraUnit.host)
                .load(new ClassPathJsonDataSet("json/dataSetWithComparatorType.json"));
    }

    @Test
    public void shouldParseXmlAndCreateKeyspaceWithComparatorTypeAndMetadatas() throws Exception {
        startEmbeddedCassandra();
        new DataLoader(CassandraUnit.clusterName, CassandraUnit.host)
                .load(new ClassPathXmlDataSet("xml/dataSetWithComparatorType.xml"));
    }

}

