package org.cassandraunit.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Half of the context-caching check; see {@link EmbeddedCassandraContextReuseTest} for the
 * other half and {@link SharedEmbeddedCassandraConfig} for how it is counted.
 *
 * @author Jeremy Sevellec
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SharedEmbeddedCassandraConfig.class)
@EmbeddedCassandra
public class EmbeddedCassandraContextReuseSecondTest {

    @Autowired
    private SharedEmbeddedCassandraConfig.ObservedAddress address;

    @Test
    public void should_see_the_embedded_address_and_build_one_context() {
        assertThat(address.contactPoints()).isEqualTo(EmbeddedCassandraServerHelper.getHost());
        assertThat(address.port()).isEqualTo(EmbeddedCassandraServerHelper.getNativeTransportPort());
        assertThat(SharedEmbeddedCassandraConfig.builds())
                .as("this context must be shared with EmbeddedCassandraContextReuseSecondTest - "
                        + "a count above 1 means the customizer's equals/hashCode stopped matching "
                        + "and every annotated class is now paying for its own ApplicationContext")
                .isEqualTo(1);
    }
}
