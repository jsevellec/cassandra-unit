package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.dataset.CQLDataSet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What {@link CqlDataSetExtension} can say before it has a session.
 * <p>
 * The loading behaviour is covered by {@code CqlDataSetExtensionTest} over in cassandra-unit,
 * which has a real node to load into. This module has none by design, so what it can check here is
 * that the builder rejects unusable configurations up front, and that nothing touches the supplier
 * until the extension actually starts - which is what makes the Testcontainers ordering work.
 */
class CqlDataSetExtensionBuilderTest {

    /** A dataset that needs no file and no server; the builder never looks inside it. */
    private static final CQLDataSet SOME_DATASET = new CQLDataSet() {
        @Override
        public List<String> getCQLStatements() {
            return List.of("CREATE TABLE t (id int PRIMARY KEY)");
        }

        @Override
        public String getKeyspaceName() {
            return "someKeyspace";
        }

        @Override
        public boolean isKeyspaceCreation() {
            return true;
        }

        @Override
        public boolean isKeyspaceDeletion() {
            return true;
        }
    };

    @Test
    void shouldRefuseANullSupplier() {
        assertThatThrownBy(() -> CqlDataSetExtension.using((Supplier<CqlSession>) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void shouldRefuseToBuildWithNothingToLoad() {
        assertThatThrownBy(() -> CqlDataSetExtension.using(() -> null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nothing to load");
    }

    /**
     * The supplier must not run at build time: when the field initialiser runs, a Testcontainers
     * container has not been started yet.
     */
    @Test
    void shouldNotCallTheSupplierWhileBuilding() {
        boolean[] called = {false};

        CqlDataSetExtension.using(() -> {
            called[0] = true;
            return null;
        }).load(SOME_DATASET).build();

        assertThat(called[0]).isFalse();
    }

    @Test
    void shouldRefuseToHandOutASessionBeforeItHasOne() {
        CqlDataSetExtension extension = CqlDataSetExtension.using(() -> null)
                .load(SOME_DATASET)
                .build();

        assertThatThrownBy(extension::getSession)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no session yet");
    }
}
