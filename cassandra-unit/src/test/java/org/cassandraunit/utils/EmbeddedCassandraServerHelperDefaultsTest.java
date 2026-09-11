package org.cassandraunit.utils;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertions that must hold without an embedded Cassandra running, so this class deliberately
 * never starts one. Surefire uses one fork per class, so nothing here has a daemon.
 */
public class EmbeddedCassandraServerHelperDefaultsTest {

    /**
     * DEFAULT_TMP_DIR used to be the literal "target/embeddedCassandra", which baked Maven's
     * layout into the library: wrong under Gradle, which uses build/, and wrong whenever tests
     * do not run from the module directory.
     */
    @Test
    public void defaultTmpDirShouldNotAssumeAMavenLayout() {
        assertThat(EmbeddedCassandraServerHelper.DEFAULT_TMP_DIR)
                .doesNotStartWith("target")
                .startsWith(System.getProperty("java.io.tmpdir"));
    }

    /**
     * stopEmbeddedCassandra() used to dereference the daemon field unconditionally, so calling
     * it without having started anything threw NullPointerException.
     */
    @Test
    public void stopShouldBeSafeWhenNothingWasStarted() {
        EmbeddedCassandraServerHelper.stopEmbeddedCassandra();
    }
}
