package org.cassandraunit.dataset.rows;

import org.cassandraunit.dataset.ParseException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * YAML row datasets, via snakeyaml - which is already a dependency, so this format costs nothing
 * to support.
 *
 * @author Jeremy Sevellec
 */
public class YamlRowParser implements RowDataSetParser {

    @Override
    public List<TableRows> parse(InputStream in, String defaultTableName, String origin) {
        // SafeConstructor: a dataset is a data file, and should never be able to instantiate
        // arbitrary classes through a YAML tag.
        LoaderOptions options = new LoaderOptions();
        // A column written twice in one row is a mistake, not a last-one-wins instruction.
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        // UTF-8 explicitly, like the CQL loader - the platform default made the same dataset parse
        // differently on different machines (#144).
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return RowDocuments.toTables(yaml.load(reader), defaultTableName, origin);
        } catch (YAMLException | java.io.IOException e) {
            throw new ParseException(origin + ": " + e.getMessage(), e);
        }
    }
}
