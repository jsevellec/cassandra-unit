package org.cassandraunit.spring.boot;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import org.apache.cassandra.service.CassandraDaemon;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author pfrank
 */
@Configuration
@EnableConfigurationProperties(DataCassadraTestProperties.class)
public class EmbeddedCassandraAutoConfiguration {
  private final DataCassadraTestProperties dataCassadraTestProperties;

  public EmbeddedCassandraAutoConfiguration(
      final DataCassadraTestProperties dataCassadraTestProperties) {
    this.dataCassadraTestProperties = dataCassadraTestProperties;
  }

  @Bean
  CassandraDaemon cassandraDaemon(){
    final CassandraDaemon cassandraDaemon;
    try {
      cassandraDaemon =
          EmbeddedCassandraServerHelper
              .startEmbeddedCassandra(dataCassadraTestProperties.getConfiguration(),
                                      dataCassadraTestProperties.getTimeout());
    }catch(TTransportException | IOException x){
      throw new IllegalStateException("Unable to start Cassandra Daemon", x);
    }

    return cassandraDaemon;
  }

  @Bean
  public Session session(){
    cassandraDaemon();
    final Session session = EmbeddedCassandraServerHelper.getSession();
    final String[] dataSets = dataCassadraTestProperties.getDataSets();
    if(dataSets != null && dataSets.length > 0) {
      EmbeddedCassandraServerHelper.loadDataSets(session, dataCassadraTestProperties.getKeyspace(),
                                                 Arrays.asList(dataSets));
    }
    return session;
  }

  @Bean
  public Cluster cluster(){
    cassandraDaemon();
    return EmbeddedCassandraServerHelper.getCluster();
  }
}
