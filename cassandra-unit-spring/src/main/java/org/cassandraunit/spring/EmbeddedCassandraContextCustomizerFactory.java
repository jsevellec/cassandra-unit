package org.cassandraunit.spring;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

import java.util.List;

/**
 * Publishes the embedded node's address into the {@code Environment} of any test class annotated
 * with {@link EmbeddedCassandra}, so a Spring-managed {@code CqlSession} connects to it without the
 * test wiring anything.
 * <p>
 * This is what makes {@code @SpringBootTest} work. Boot's {@code CassandraAutoConfiguration} builds
 * its session from {@code spring.cassandra.*}, which default to port 9042; the embedded node listens
 * on 9142, or on a port picked at startup with {@code cu-cassandra-rndport.yaml}. Without a bridge
 * the two never meet, and with the random-port configuration the test could not even name the port
 * in a properties file. See {@link EmbeddedCassandraContextCustomizer}.
 * <p>
 * <b>This class is loaded for every Spring test in every project that has this jar</b>, because it
 * is registered in {@code META-INF/spring.factories} and Spring builds its factory list once per
 * bootstrapper. So it does two things carefully: it returns {@code null} - contributing nothing to
 * the context, not even a cache-key entry - for any class without the annotation, and it names no
 * type from {@code cassandra-unit}. Everything that touches {@code EmbeddedCassandraServerHelper}
 * lives in the customizer, which is only instantiated once the annotation has been found. A project
 * that uses this module for one test class does not load the embedded server for the others.
 * <p>
 * {@code AnnotationUtils.findAnnotation} rather than a direct {@code getAnnotation}, because that is
 * what the listeners use and it is what makes the {@code @CassandraUnit} meta-annotation work.
 *
 * @author Jeremy Sevellec
 */
public class EmbeddedCassandraContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                     List<ContextConfigurationAttributes> configAttributes) {
        EmbeddedCassandra annotation = AnnotationUtils.findAnnotation(testClass, EmbeddedCassandra.class);
        if (annotation == null || !annotation.exposeProperties()) {
            return null;
        }
        return new EmbeddedCassandraContextCustomizer(
                annotation.configuration(), annotation.tmpDir(), annotation.timeout());
    }
}
