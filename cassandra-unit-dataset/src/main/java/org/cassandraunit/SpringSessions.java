package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.function.Function;

/**
 * Hands {@link CqlDataSetExtension} the {@link CqlSession} bean from a Spring test's
 * {@code ApplicationContext}, so fixtures load into the session Spring already owns.
 * <p>
 * This is the Spring Boot story. Boot builds a {@code CqlSession} from {@code spring.cassandra.*}
 * against whatever node you point it at - a Testcontainers container, a real cluster, Astra - and
 * this loads datasets through that same session rather than a second one of our own:
 *
 * <pre>
 * &#64;SpringBootTest
 * class WidgetTest {
 *
 *     &#64;RegisterExtension
 *     static final CqlDataSetExtension fixtures = CqlDataSetExtension
 *             .using(SpringSessions.fromApplicationContext())
 *             .schemaOnce(CQLDataSetFactory.fromClassPath("cql/schema.cql", "mykeyspace"))
 *             .rowsPerTest(CQLDataSetFactory.fromClassPath(
 *                     "data/widget.yaml", false, false, "mykeyspace"))
 *             .build();
 *
 *     &#64;Test
 *     void readsTheFixture(CqlSession session) {    // resolved by the extension
 *         ...
 *     }
 * }
 * </pre>
 *
 * <b>Why this is safe whichever extension starts first.</b> {@code CqlDataSetExtension} is
 * registered programmatically with {@code @RegisterExtension}, and Jupiter runs declaratively
 * registered {@code beforeAll} callbacks - which is how {@code @SpringBootTest} registers
 * {@code SpringExtension} - before programmatic ones. That ordering does not matter here:
 * {@link SpringExtension#getApplicationContext} creates the {@code TestContextManager} if it is
 * absent, and the context itself is loaded on demand from Spring's shared context cache. So this
 * resolves a live context whether Spring got there first or we did.
 *
 * <p><b>Never combine this with {@code closingSession()}.</b> The session belongs to the
 * {@code ApplicationContext}, and Spring closes it on its own schedule. Two things would go wrong:
 * the bean would be closed out from under a context that is still cached and will be handed to the
 * next test class, and it would happen <em>early</em> - Jupiter runs {@code afterAll} callbacks in
 * reverse registration order, so this extension's teardown precedes {@code SpringExtension}'s.
 * {@code closingSession()} is off by default; leave it off.
 *
 * <p><b>{@code @DirtiesContext} has one shape that does not work.</b> The session is resolved once
 * and cached for the class, so:
 * <ul>
 * <li>class-level {@code @DirtiesContext} with the default {@code AFTER_CLASS} - fine, the context
 *     is discarded after everything here has finished with it;</li>
 * <li>{@code AFTER_EACH_TEST_METHOD} or a method-level {@code @DirtiesContext} - <b>broken</b>. The
 *     context is rebuilt between methods and its {@code CqlSession} bean replaced, but this
 *     extension still holds the closed one and {@code beforeEach} loads into it.</li>
 * </ul>
 *
 * <p><b>Leave the test-method parameter bare.</b> Both this extension and {@code SpringExtension}
 * resolve parameters. A plain {@code CqlSession} parameter is claimed only by ours, because Spring
 * takes a parameter only when it carries {@code @Autowired}, {@code @Qualifier} or {@code @Value}.
 * Add {@code @Autowired} to it and both claim it, which Jupiter reports as
 * {@code ParameterResolutionException: Discovered multiple competing ParameterResolvers}. Use a
 * bare parameter, or an {@code @Autowired} field - not an {@code @Autowired} parameter.
 *
 * <p>Requires {@code spring-test} and {@code spring-context} on the test classpath. Both are
 * optional dependencies of this module, so they reach you only if you declare them - or, in
 * practice, because you are already using Spring.
 *
 * @author Jeremy Sevellec
 * @see CqlDataSetExtension#using(Function)
 */
public final class SpringSessions {

    private SpringSessions() {
    }

    /**
     * The one {@link CqlSession} bean in the test's {@code ApplicationContext}, by type.
     *
     * @return a session source for {@link CqlDataSetExtension#using(Function)}
     */
    public static Function<ExtensionContext, CqlSession> fromApplicationContext() {
        return context -> {
            ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
            try {
                return applicationContext.getBean(CqlSession.class);
            } catch (NoUniqueBeanDefinitionException e) {
                throw new IllegalStateException("the ApplicationContext defines "
                        + e.getNumberOfBeansFound() + " CqlSession beans, so there is no single one"
                        + " to load into - name the one you mean with"
                        + " SpringSessions.fromApplicationContext(String)", e);
            } catch (NoSuchBeanDefinitionException e) {
                throw new IllegalStateException("the ApplicationContext defines no CqlSession bean."
                        + " Declare one, or build the session yourself with"
                        + " CqlDataSetExtension.using(Supplier)", e);
            }
        };
    }

    /**
     * A {@link CqlSession} bean by name, for a context that defines more than one.
     *
     * @param beanName name of the bean to load through
     * @return a session source for {@link CqlDataSetExtension#using(Function)}
     */
    public static Function<ExtensionContext, CqlSession> fromApplicationContext(String beanName) {
        if (beanName == null || beanName.isBlank()) {
            throw new IllegalArgumentException("beanName must not be null or blank");
        }
        return context -> {
            ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
            try {
                return applicationContext.getBean(beanName, CqlSession.class);
            } catch (NoSuchBeanDefinitionException e) {
                throw new IllegalStateException("the ApplicationContext defines no CqlSession bean"
                        + " named '" + beanName + "'", e);
            }
        };
    }
}
