package org.cassandraunit.spring;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextCustomizer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The factory's gate, tested directly.
 * <p>
 * This matters more than its size suggests. The factory is listed in {@code META-INF/spring.factories},
 * so Spring loads it for <em>every</em> Spring test in every project that has this jar on the
 * classpath - including projects that use one annotated test class and a hundred plain ones.
 * Returning null for those is what keeps it invisible: a null contributes nothing to the context
 * and nothing to the context cache key.
 * <p>
 * The equality cases are the context cache. Spring folds customizers into
 * {@code MergedContextConfiguration}, so two classes annotated the same way must produce equal
 * customizers or each gets its own {@code ApplicationContext} - a slowdown that no assertion
 * anywhere else would catch. {@code EmbeddedCassandraContextReuseTest} checks the consequence;
 * this checks the cause.
 *
 * @author Jeremy Sevellec
 */
public class EmbeddedCassandraContextCustomizerFactoryTest {

    private final EmbeddedCassandraContextCustomizerFactory factory =
            new EmbeddedCassandraContextCustomizerFactory();

    private ContextCustomizer customizerFor(Class<?> testClass) {
        return factory.createContextCustomizer(testClass, List.of());
    }

    @Test
    public void should_contribute_nothing_to_a_class_without_the_annotation() {
        assertThat(customizerFor(NotAnnotated.class)).isNull();
    }

    @Test
    public void should_contribute_nothing_when_the_user_opted_out() {
        assertThat(customizerFor(OptedOut.class)).isNull();
    }

    @Test
    public void should_create_a_customizer_for_an_annotated_class() {
        assertThat(customizerFor(Annotated.class)).isNotNull();
    }

    /** Found through {@code @CassandraUnit} too, which is a meta-annotation of the two. */
    @Test
    public void should_find_the_annotation_through_the_meta_annotation() {
        assertThat(customizerFor(ViaCassandraUnit.class)).isNotNull();
    }

    @Test
    public void should_be_equal_for_equivalent_attributes() {
        assertThat(customizerFor(Annotated.class))
                .isEqualTo(customizerFor(AnnotatedIdentically.class))
                .hasSameHashCodeAs(customizerFor(AnnotatedIdentically.class));
    }

    @Test
    public void should_differ_when_the_configuration_differs() {
        assertThat(customizerFor(Annotated.class)).isNotEqualTo(customizerFor(OtherYaml.class));
    }

    @Test
    public void should_differ_when_the_timeout_differs() {
        assertThat(customizerFor(Annotated.class)).isNotEqualTo(customizerFor(OtherTimeout.class));
    }

    static class NotAnnotated {
    }

    @EmbeddedCassandra(exposeProperties = false)
    static class OptedOut {
    }

    @EmbeddedCassandra
    static class Annotated {
    }

    @EmbeddedCassandra
    static class AnnotatedIdentically {
    }

    @CassandraUnit
    static class ViaCassandraUnit {
    }

    @EmbeddedCassandra(configuration = "cu-cassandra-rndport.yaml")
    static class OtherYaml {
    }

    @EmbeddedCassandra(timeout = 12345L)
    static class OtherTimeout {
    }
}
