package org.cassandraunit.spring.boot.repository;

import org.cassandraunit.spring.boot.model.CqlTable1;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

/**
 * @author pfrank
 */
public interface CqlTable1Repository extends CrudRepository<CqlTable1, UUID>{

}
