package org.cassandraunit.spring.boot.model;

import org.springframework.data.cassandra.mapping.Column;
import org.springframework.data.cassandra.mapping.PrimaryKey;
import org.springframework.data.cassandra.mapping.Table;

import java.util.UUID;

/**
 * @author pfrank
 */
@Table("testCQLTable1")
public class CqlTable1 {
  @PrimaryKey
  private UUID id;

  @Column
  private String value;

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public String getValue() {
    return value;
  }

  public void setValue(final String value) {
    this.value = value;
  }
}
