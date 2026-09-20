package org.cassandraunit.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A context shared by {@link EmbeddedCassandraContextReuseTest} and
 * {@link EmbeddedCassandraContextReuseSecondTest}, counting how many times it is actually built.
 * <p>
 * Both classes carry identical {@code @EmbeddedCassandra} attributes and name this same
 * configuration, so Spring should build one context and hand it to both. The counter is how that is
 * observed: nothing fails when context caching breaks, the suite merely gets slower, so it has to
 * be asserted deliberately.
 *
 * @author Jeremy Sevellec
 */
@Configuration(proxyBeanMethods = false)
public class SharedEmbeddedCassandraConfig {

    private static final AtomicInteger BUILDS = new AtomicInteger();

    static int builds() {
        return BUILDS.get();
    }

    /** Records the address this context saw, so the reuse test can check it is the real one. */
    @Bean
    ObservedAddress observedAddress(Environment environment) {
        BUILDS.incrementAndGet();
        return new ObservedAddress(
                environment.getProperty("spring.cassandra.contact-points"),
                environment.getProperty("spring.cassandra.port", Integer.class));
    }

    record ObservedAddress(String contactPoints, Integer port) {
    }
}
