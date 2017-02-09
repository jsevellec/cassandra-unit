package org.cassandraunit.spring.boot;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
  public Session session(){
    return EmbeddedCassandraServerHelper.getSession();
  }

  @Bean
  public Cluster cluster(){
    return EmbeddedCassandraServerHelper.getCluster();
  }
}
