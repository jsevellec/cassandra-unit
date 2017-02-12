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
@EnableConfigurationProperties(EmbeddedCassadraTestProperties.class)
public class EmbeddedCassandraAutoConfiguration {
  private final EmbeddedCassadraTestProperties embeddedCassadraTestProperties;

  public EmbeddedCassandraAutoConfiguration(
      final EmbeddedCassadraTestProperties embeddedCassadraTestProperties) {
    this.embeddedCassadraTestProperties = embeddedCassadraTestProperties;
  }

  @Bean
  CassandraDaemon cassandraDaemon(){
    final CassandraDaemon cassandraDaemon;
    try {
      cassandraDaemon =
          EmbeddedCassandraServerHelper
              .startEmbeddedCassandra(embeddedCassadraTestProperties.getConfiguration(),
                                      embeddedCassadraTestProperties.getTimeout());
    }catch(TTransportException | IOException x){
      throw new IllegalStateException("Unable to start Cassandra Daemon", x);
    }

    return cassandraDaemon;
  }

  @Bean
  public Session session(){
    cassandraDaemon();
    final Session session = EmbeddedCassandraServerHelper.getSession();
    final String[] dataSets = embeddedCassadraTestProperties.getDataSets();
    if(dataSets != null && dataSets.length > 0) {
      EmbeddedCassandraServerHelper.loadDataSets(session, embeddedCassadraTestProperties.getKeyspace(),
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
