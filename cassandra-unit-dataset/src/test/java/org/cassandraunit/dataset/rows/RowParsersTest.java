package org.cassandraunit.dataset.rows;

import org.cassandraunit.dataset.ParseException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The parsers on their own, with no Cassandra: what did the file literally say?
 * <p>
 * Type conversion is deliberately not tested here - the parsers do none. That is
 * {@code RowsDataSetLoadTest}'s job, because it needs a real schema to convert against.
 *
 * @author Jeremy Sevellec
 */
class RowParsersTest {

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void yamlShouldReadTablesRowsAndNativeTypes() {
        List<TableRows> tables = new YamlRowParser().parse(stream("""
                widget:
                  - id: 1
                    label: "1"
                    tags: [a, b]
                  - id: 2
                    label: null
                """), null, "test");

        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).table()).isEqualTo("widget");
        assertThat(tables.get(0).rows()).hasSize(2);
        // Unquoted 1 is a number, quoted "1" is a string. The parser reports the difference; it
        // does not resolve it.
        assertThat(tables.get(0).rows().get(0)).containsEntry("id", 1).containsEntry("label", "1");
        assertThat(tables.get(0).rows().get(0).get("tags")).isEqualTo(List.of("a", "b"));
        // An explicit null is a present key with a null value, which is not the same as absent.
        assertThat(tables.get(0).rows().get(1)).containsKey("label");
        assertThat(tables.get(0).rows().get(1).get("label")).isNull();
    }

    @Test
    void yamlShouldAcceptSeveralTablesInOneFile() {
        List<TableRows> tables = new YamlRowParser().parse(stream("""
                widget:
                  - id: 1
                gadget:
                  - id: 2
                """), null, "test");

        assertThat(tables).extracting(TableRows::table).containsExactlyInAnyOrder("widget", "gadget");
    }

    @Test
    void yamlShouldAcceptABareListOfRowsWhenTheTableNameIsKnown() {
        List<TableRows> tables = new YamlRowParser().parse(stream("- id: 1\n"), "widget", "test");

        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).table()).isEqualTo("widget");
    }

    @Test
    void yamlShouldRejectABareListOfRowsWithNoTableName() {
        assertThatThrownBy(() -> new YamlRowParser().parse(stream("- id: 1\n"), null, "test"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("bare list of rows");
    }

    @Test
    void anEmptyDocumentShouldLoadNothing() {
        assertThat(new YamlRowParser().parse(stream(""), "widget", "test")).isEmpty();
    }

    @Test
    void aMalformedDocumentShouldSayWhichFileItWas() {
        assertThatThrownBy(() -> new YamlRowParser().parse(stream("widget: [oops\n"), null, "classpath:rows/x.yaml"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("classpath:rows/x.yaml");
    }

    @Test
    void aTableThatDoesNotHoldAListShouldBeRejected() {
        assertThatThrownBy(() -> new YamlRowParser().parse(stream("widget: nonsense\n"), null, "test"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("must hold a list of rows");
    }

    @Test
    void jsonShouldProduceTheSameShapeAsYaml() {
        List<TableRows> tables = new JsonRowParser().parse(
                stream("{\"widget\":[{\"id\":1,\"label\":\"1\",\"tags\":[\"a\",\"b\"]}]}"), null, "test");

        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).table()).isEqualTo("widget");
        assertThat(tables.get(0).rows().get(0)).containsEntry("id", 1).containsEntry("label", "1");
        assertThat(tables.get(0).rows().get(0).get("tags")).isEqualTo(List.of("a", "b"));
    }

    @Test
    void xmlShouldReadValuesAsStringsAndMarkNullsExplicitly() {
        List<TableRows> tables = new XmlRowParser().parse(stream("""
                <dataset>
                  <table name="widget">
                    <row>
                      <id>1</id>
                      <label>1</label>
                      <tags><value>a</value><value>b</value></tags>
                      <props><entry key="k">7</entry></props>
                    </row>
                    <row>
                      <id>2</id>
                      <label null="true"/>
                    </row>
                  </table>
                </dataset>
                """), null, "test");

        Map<String, Object> first = tables.get(0).rows().get(0);
        // Everything XML produces is a string; RowValueConverter is what turns "1" into whatever the
        // column actually is.
        assertThat(first).containsEntry("id", "1").containsEntry("label", "1");
        assertThat(first.get("tags")).isEqualTo(List.of("a", "b"));
        assertThat(first.get("props")).isEqualTo(Map.of("k", "7"));

        Map<String, Object> second = tables.get(0).rows().get(1);
        assertThat(second).containsKey("label");
        assertThat(second.get("label")).isNull();
    }

    @Test
    void xmlShouldNotResolveExternalEntities() {
        // A dataset is data. If it could pull in a DOCTYPE it could read the filesystem (XXE).
        assertThatThrownBy(() -> new XmlRowParser().parse(stream("""
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <dataset><table name="widget"><row><id>&xxe;</id></row></table></dataset>
                """), null, "test"))
                .isInstanceOf(ParseException.class);
    }

    @Test
    void csvShouldTakeItsTableNameFromOutsideTheFileAndSplitCollections() {
        List<TableRows> tables = new CsvRowParser().parse(
                stream("id,label,tags\n1,alpha,a|b\n"), "widget", "test");

        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).table()).isEqualTo("widget");
        assertThat(tables.get(0).rows().get(0)).containsEntry("id", "1").containsEntry("label", "alpha");
        assertThat(tables.get(0).rows().get(0).get("tags")).isEqualTo(List.of("a", "b"));
    }

    @Test
    void csvShouldTreatAnEmptyFieldAsUnsetRatherThanNull() {
        List<TableRows> tables = new CsvRowParser().parse(
                stream("id,label\n1,\n"), "widget", "test");

        // Absent, not present-and-null: the column is left out of the INSERT, so nothing is
        // written and no tombstone is created. CSV cannot express an explicit null.
        assertThat(tables.get(0).rows().get(0)).containsOnlyKeys("id");
    }

    @Test
    void csvShouldKeepAQuotedFieldContainingASeparatorIntact() {
        List<TableRows> tables = new CsvRowParser().parse(
                stream("id,label\n1,\"a,b\"\n"), "widget", "test");

        assertThat(tables.get(0).rows().get(0)).containsEntry("label", "a,b");
    }

    @Test
    void csvShouldRejectADatasetWithNoTableName() {
        assertThatThrownBy(() -> new CsvRowParser().parse(stream("id\n1\n"), null, "test"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("cannot name its own table");
    }
}
