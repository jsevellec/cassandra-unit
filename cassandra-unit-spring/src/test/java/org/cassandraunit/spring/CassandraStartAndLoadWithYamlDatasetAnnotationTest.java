package org.cassandraunit.spring;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema as CQL, rows as YAML, through {@code @CassandraDataSet}.
 * <p>
 * No new annotation attribute is involved: the format comes from each location's extension, and
 * the listener's existing rule - only the first location drops and creates the keyspace - is
 * already exactly what a schema-then-rows pair needs.
 *
 * @author Jeremy Sevellec
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(value = {"classpath:/default-context.xml"})
@TestExecutionListeners({CassandraUnitTestExecutionListener.class})
@CassandraDataSet(value = {"cql/widgetSchema.cql", "rows/widgets.yaml"})
@EmbeddedCassandra
public class CassandraStartAndLoadWithYamlDatasetAnnotationTest {

    @Test
    public void should_load_rows_from_yaml() {
        test();
    }

    @Test
    public void should_load_rows_from_yaml_twice() {
        test();
    }

    private void test() {
        CqlSession session = EmbeddedCassandraServerHelper.getSession();

        Row row = session.execute(
                "select * from widget where id=1690e8da-5bf8-49e8-9583-4dff8a570c01").one();

        assertThat(row.getString("label")).isEqualTo("1");
        assertThat(row.getSet("tags", String.class)).containsExactlyInAnyOrder("alpha", "beta");
    }
}
