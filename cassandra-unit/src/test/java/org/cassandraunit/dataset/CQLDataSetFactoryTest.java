package org.cassandraunit.dataset;

import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.dataset.rows.RowsCQLDataSet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Jeremy Sevellec
 */
class CQLDataSetFactoryTest {

    @Test
    void aCqlLocationShouldStillGiveTheOriginalDataSet() {
        assertThat(CQLDataSetFactory.fromClassPath("cql/simple.cql"))
                .isInstanceOf(ClassPathCQLDataSet.class);
    }

    @Test
    void aRowFormatShouldGiveARowDataSet() {
        assertThat(CQLDataSetFactory.fromClassPath("rows/widget.yaml")).isInstanceOf(RowsCQLDataSet.class);
        assertThat(CQLDataSetFactory.fromClassPath("rows/widget.json")).isInstanceOf(RowsCQLDataSet.class);
        assertThat(CQLDataSetFactory.fromClassPath("rows/widget.xml")).isInstanceOf(RowsCQLDataSet.class);
        assertThat(CQLDataSetFactory.fromClassPath("rows/widget.csv")).isInstanceOf(RowsCQLDataSet.class);
    }

    @Test
    void aRowDataSetShouldEmitNoStatementsOfItsOwn() {
        // It binds prepared statements inside load(session) instead, which is the whole reason
        // SessionAwareDataSet exists.
        assertThat(CQLDataSetFactory.fromClassPath("rows/widget.yaml").getCQLStatements()).isEmpty();
    }

    @Test
    void theExtensionShouldBeCaseInsensitive() {
        // Same file, spelled .YAML on disk - an extension is not a case-sensitive token.
        assertThat(CQLDataSetFactory.fromClassPath("rows/upperCaseExtension.YAML"))
                .isInstanceOf(RowsCQLDataSet.class);
    }

    @Test
    void ymlShouldBeAnAliasForYaml() {
        assertThat(CQLDataSetFactory.fromClassPath("rows/shortExtension.yml"))
                .isInstanceOf(RowsCQLDataSet.class);
    }

    @Test
    void severalLocationsShouldLoadAsOneWithTheKeyspaceHandledOnce() {
        // The shape a row dataset needs: schema first, rows after, one drop-and-create for the
        // pair. Without it a row dataset cannot be used from the JUnit rule or extension at all,
        // since both take exactly one dataset.
        CQLDataSet dataSet = CQLDataSetFactory.fromClassPathAll(
                "mykeyspace", "cql/rowsSchema.cql", "rows/widget.yaml");

        assertThat(dataSet).isInstanceOf(CompositeCQLDataSet.class);
        assertThat(dataSet.isKeyspaceCreation()).isTrue();
        assertThat(dataSet.isKeyspaceDeletion()).isTrue();
        assertThat(dataSet.getKeyspaceName()).isEqualTo("mykeyspace");

        // The members must not drop it themselves - a member that did would destroy what the
        // member before it just loaded.
        assertThat(((CompositeCQLDataSet) dataSet).getDataSets())
                .allSatisfy(member -> {
                    assertThat(member.isKeyspaceCreation()).isFalse();
                    assertThat(member.isKeyspaceDeletion()).isFalse();
                });
    }

    @Test
    void aCompositeWithNoLocationsShouldBeRejected() {
        assertThatThrownBy(() -> CQLDataSetFactory.fromClassPathAll("mykeyspace"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("No dataset locations");
    }

    @Test
    void keyspaceFlagsAndNameShouldCarryThrough() {
        CQLDataSet dataSet = CQLDataSetFactory.fromClassPath("rows/widget.yaml", false, false, "MyKeyspace");

        assertThat(dataSet.isKeyspaceCreation()).isFalse();
        assertThat(dataSet.isKeyspaceDeletion()).isFalse();
        // Lowercased, or USE breaks - the same contract AbstractCQLDataSet has.
        assertThat(dataSet.getKeyspaceName()).isEqualTo("mykeyspace");
    }

    @Test
    void aMissingDataSetShouldFailWhenItIsBuilt() {
        // Not later at load time: a typo in the path should fail where you can see it.
        assertThatThrownBy(() -> CQLDataSetFactory.fromClassPath("rows/nosuch.yaml"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Dataset not found");
    }

    @Test
    void anUnknownExtensionShouldSayWhatIsSupported() {
        assertThatThrownBy(() -> CQLDataSetFactory.fromClassPath("rows/widget.properties"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Unsupported dataset extension")
                .hasMessageContaining("yaml");
    }

    @Test
    void aLocationWithNoExtensionShouldSayWhyItCannotBeRead() {
        assertThatThrownBy(() -> CQLDataSetFactory.fromClassPath("rows/widget"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("no file extension");
    }

    @Test
    void cqlShouldBeTriedFirstByConvention() {
        // The Spring listener walks this list looking for <TestClassName>-dataset.<ext>. cql has
        // to stay first so an existing project resolves to exactly the file it always did.
        assertThat(CQLDataSetFactory.SUPPORTED_EXTENSIONS).startsWith("cql");
    }
}
