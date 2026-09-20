package org.cassandraunit.spring;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Where the published property source sits relative to a value the user set themselves.
 * <p>
 * It is added with {@code addFirst}, so the node that is actually running wins over a port written
 * down in advance. That is the behaviour worth having - with {@code cu-cassandra-rndport.yaml} the
 * real port cannot be written down at all - but it is a change for anyone who used to bridge the
 * gap by hand, so it is pinned here rather than left to be discovered.
 *
 * @author Jeremy Sevellec
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EmbeddedCassandraExposedPropertiesTest.Config.class)
@TestPropertySource(properties = {"spring.cassandra.port=9042",
        "spring.cassandra.contact-points=example.invalid"})
@EmbeddedCassandra
public class EmbeddedCassandraExposedPropertiesTest {

    @Autowired
    private ConfigurableEnvironment environment;

    /**
     * Ahead of the properties the test set for itself. Not necessarily at index 0 - Spring puts a
     * {@code configurationProperties} adapter there - so what is asserted is the relative order,
     * which is what decides the value.
     */
    @Test
    public void should_rank_ahead_of_the_inlined_test_properties() {
        List<String> names = new ArrayList<>();
        environment.getPropertySources().forEach(source -> names.add(source.getName()));

        assertThat(names).contains(EmbeddedCassandraContextCustomizer.PROPERTY_SOURCE_NAME);
        assertThat(names.indexOf(EmbeddedCassandraContextCustomizer.PROPERTY_SOURCE_NAME))
                .isLessThan(names.indexOf("Inlined Test Properties"));
    }

    @Test
    public void should_win_over_a_hand_written_address() {
        assertThat(environment.getProperty("spring.cassandra.port", Integer.class))
                .isEqualTo(EmbeddedCassandraServerHelper.getNativeTransportPort())
                .isNotEqualTo(9042);
        assertThat(environment.getProperty("spring.cassandra.contact-points"))
                .isEqualTo(EmbeddedCassandraServerHelper.getHost())
                .isNotEqualTo("example.invalid");
    }

    @Test
    public void should_publish_the_local_datacenter() {
        assertThat(environment.getProperty("spring.cassandra.local-datacenter"))
                .isEqualTo(EmbeddedCassandraContextCustomizer.LOCAL_DATACENTER);
    }

    @Configuration(proxyBeanMethods = false)
    static class Config {
    }
}
