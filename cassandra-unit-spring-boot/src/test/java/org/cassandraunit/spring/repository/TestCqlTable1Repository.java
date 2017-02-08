package org.cassandraunit.spring.repository;

import org.cassandraunit.spring.data.TestCqlTable1;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

/**
 * @author pfrank
 */
public interface TestCqlTable1Repository extends CrudRepository<TestCqlTable1, UUID>{

}
