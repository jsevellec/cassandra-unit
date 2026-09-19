package org.cassandraunit.dataset.rows;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.cassandraunit.dataset.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV row datasets, via jackson-dataformat-csv.
 * <p>
 * CSV is flat, so one file is one table and the table name comes from outside the file - the
 * filename stem by default. The header row supplies the column names.
 * <p>
 * Two consequences of the format worth knowing:
 * <ul>
 *   <li><b>An empty field means unset</b>, not null. The column is left out of that row's INSERT,
 *       so nothing is written and no tombstone is created. CSV has no way to say "explicitly
 *       null"; use YAML or JSON when a tombstone is what you want. Inventing a {@code NULL}
 *       sentinel was considered and rejected - a text column can legitimately contain the string
 *       {@code NULL}, and a sentinel would corrupt it silently.</li>
 *   <li><b>Collections are split on a separator</b>, {@code |} by default, so a {@code set<text>}
 *       is written {@code alpha|beta}.</li>
 * </ul>
 * <p>
 * This is the only format needing a dependency that is not already present.
 * {@code jackson-dataformat-csv} is declared optional; {@code CQLDataSetFactory} checks for it and
 * explains what to add if it is missing. Hand-rolling a splitter was rejected: quoted fields,
 * embedded commas and embedded newlines are exactly what a hand-rolled one gets wrong.
 *
 * @author Jeremy Sevellec
 */
public class CsvRowParser implements RowDataSetParser {

    public static final String DEFAULT_COLLECTION_SEPARATOR = "|";

    private static final CsvMapper MAPPER = CsvMapper.builder()
            .enable(CsvParser.Feature.TRIM_SPACES)
            .build();

    private final String collectionSeparator;

    public CsvRowParser() {
        this(DEFAULT_COLLECTION_SEPARATOR);
    }

    public CsvRowParser(String collectionSeparator) {
        this.collectionSeparator = collectionSeparator;
    }

    @Override
    public List<TableRows> parse(InputStream in, String defaultTableName, String origin) {
        if (defaultTableName == null) {
            throw new ParseException(origin + ": a CSV dataset cannot name its own table. Name the "
                    + "file after the table, or load it with an explicit table name.");
        }
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             MappingIterator<Map<String, String>> it =
                     MAPPER.readerFor(Map.class).with(schema).readValues(reader)) {
            while (it.hasNext()) {
                rows.add(toRow(it.next()));
            }
        } catch (IOException e) {
            throw new ParseException(origin + ": " + e.getMessage(), e);
        }
        return List.of(new TableRows(defaultTableName, rows));
    }

    private Map<String, Object> toRow(Map<String, String> raw) {
        Map<String, Object> row = new LinkedHashMap<>();
        raw.forEach((column, value) -> {
            if (value == null || value.isEmpty()) {
                // Unset: leave the column out of the INSERT entirely.
                return;
            }
            row.put(column, value.contains(collectionSeparator)
                    ? List.of(value.split(java.util.regex.Pattern.quote(collectionSeparator), -1))
                    : value);
        });
        return row;
    }
}
