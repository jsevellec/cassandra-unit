package org.cassandraunit.spring.boot;

import org.cassandraunit.dataset.DataSetFileExtensionEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author pfrank
 */
@ConfigurationProperties(prefix = "cassandraunit")
public class DataCassadraTestProperties {
  private String keyspace;
  private String configuration;
  private String[] dataSets;
  private DataSetFileExtensionEnum type;
  private long timeout;

  public String getKeyspace() {
    return keyspace;
  }

  public void setKeyspace(String keyspace) {
    this.keyspace = keyspace;
  }

  public String getConfiguration() {
    return configuration;
  }

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  public String[] getDataSets() {
    return dataSets;
  }

  public void setDataSets(String[] dataSets) {
    this.dataSets = dataSets;
  }

  public DataSetFileExtensionEnum getType() {
    return type;
  }

  public void setType(DataSetFileExtensionEnum type) {
    this.type = type;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
}
