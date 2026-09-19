package org.cassandraunit.spring;

import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

import java.util.*;

/**
 * The goal of this abstract listener is to provide utility methods for its subclasses to be able to :
 * - start an embedded Cassandra
 * - load dataset into Cassandra keyspace
 *
 * @author Gaëtan Le Brun
 */
public abstract class AbstractCassandraUnitTestExecutionListener extends AbstractTestExecutionListener implements Ordered {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraUnitTestExecutionListener.class);
    private static boolean initialized = false;

    protected void startServer(TestContext testContext) throws Exception {
        EmbeddedCassandra embeddedCassandra = Objects.requireNonNull(AnnotationUtils.findAnnotation(testContext.getTestClass(), EmbeddedCassandra.class),
                "CassandraUnitTestExecutionListener must be used with @EmbeddedCassandra on " + testContext.getTestClass());
        if (!initialized) {
            String yamlFile = embeddedCassandra.configuration();
            // An empty tmpDir means "use the library default"; see @EmbeddedCassandra#tmpDir.
            String tmpDir = embeddedCassandra.tmpDir().isEmpty()
                    ? EmbeddedCassandraServerHelper.DEFAULT_TMP_DIR
                    : embeddedCassandra.tmpDir();
            long timeout = embeddedCassandra.timeout();
            EmbeddedCassandraServerHelper.startEmbeddedCassandra(yamlFile, tmpDir, timeout);
            initialized = true;
        }

        CassandraDataSet cassandraDataSet = AnnotationUtils.findAnnotation(testContext.getTestClass(), CassandraDataSet.class);
        if (cassandraDataSet != null) {
            String keyspace = cassandraDataSet.keyspace();
            List<String> dataset = dataSetLocations(testContext, cassandraDataSet);
            ListIterator<String> datasetIterator = dataset.listIterator();

            CQLDataLoader cqlDataLoader = new CQLDataLoader(EmbeddedCassandraServerHelper.getSession());
            while (datasetIterator.hasNext()) {
                String next = datasetIterator.next();
                boolean dropAndCreateKeyspace = datasetIterator.previousIndex() == 0;
                cqlDataLoader.load(CQLDataSetFactory.fromClassPath(next, dropAndCreateKeyspace, dropAndCreateKeyspace, keyspace));
            }
        }
    }

    /**
     * Find the dataset(s) to load. An explicit {@code @CassandraDataSet("...")} wins; otherwise the
     * convention is {@code <TestClassName>-dataset.<ext>}, tried first against the fully qualified
     * class name and then the simple name.
     * <p>
     * The format comes from the extension, so the convention now has several candidates rather
     * than one. They are tried in {@link CQLDataSetFactory#SUPPORTED_EXTENSIONS} order - {@code cql}
     * first, so a project that already has a {@code -dataset.cql} keeps resolving to exactly the
     * file it always did. Within a layout the first hit wins; the fully qualified layout is
     * exhausted before the simple one, which is the precedence that was already there.
     */
    private List<String> dataSetLocations(TestContext testContext, CassandraDataSet cassandraDataSet) {
        String[] dataset = cassandraDataSet.value();
        if (dataset.length == 0) {
            String found = findByConvention(testContext, true);
            if (found == null) {
                found = findByConvention(testContext, false);
            }
            if (found == null) {
                LOGGER.info("No dataset will be loaded");
            } else {
                dataset = new String[]{found};
            }
        }
        return Arrays.asList(dataset);
    }

    private String findByConvention(TestContext testContext, boolean includedPackageName) {
        for (String extension : CQLDataSetFactory.SUPPORTED_EXTENSIONS) {
            String alternativePath = alternativePath(testContext.getTestClass(), includedPackageName, extension);
            if (testContext.getApplicationContext().getResource(alternativePath).exists()) {
                return alternativePath.replace(ResourceUtils.CLASSPATH_URL_PREFIX + "/", "");
            }
        }
        return null;
    }

    protected void cleanServer() {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    protected String alternativePath(Class<?> clazz, boolean includedPackageName, String extension) {
        if (includedPackageName) {
            return ResourceUtils.CLASSPATH_URL_PREFIX + "/" + ClassUtils.convertClassNameToResourcePath(clazz.getName()) + "-dataset" + "." + extension;
        } else {
            return ResourceUtils.CLASSPATH_URL_PREFIX + "/" + clazz.getSimpleName() + "-dataset" + "." + extension;
        }
    }

    @Override
    public int getOrder() {
        // since spring 4.1 the default-order is LOWEST_PRECEDENCE. But we want to start EmbeddedCassandra even before
        // the springcontext such that beans can connect to the started cassandra
        return 0;
    }
}
