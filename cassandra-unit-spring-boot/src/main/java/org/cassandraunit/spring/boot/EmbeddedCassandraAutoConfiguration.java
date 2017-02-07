package org.cassandraunit.spring.boot;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author pfrank
 */
@Configuration
public class EmbeddedCassandraAutoConfiguration {
  @Bean
  public Session session(){
    return EmbeddedCassandraServerHelper.getSession();
  }

  @Bean
  public Cluster cluster(){
    return EmbeddedCassandraServerHelper.getCluster();
  }
}
