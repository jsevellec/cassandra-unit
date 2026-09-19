package org.cassandraunit.dataset.rows;

import org.cassandraunit.dataset.ParseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared shape handling for the formats that parse to plain maps and lists - YAML and JSON, which
 * differ only in the parser that produced the structure, not in what the structure means.
 *
 * @author Jeremy Sevellec
 */
final class RowDocuments {

    private RowDocuments() {
    }

    /**
     * Accepts either shape:
     * <ul>
     *   <li>a map of table name to list of rows, which is the normal form and can carry several
     *       tables in one file;</li>
     *   <li>a bare list of rows, for a single-table file, which then needs {@code defaultTableName}
     *       - normally the filename stem.</li>
     * </ul>
     */
    static List<TableRows> toTables(Object root, String defaultTableName, String origin) {
        if (root == null) {
            // An empty document is a legitimate fixture: nothing to insert.
            return List.of();
        }
        if (root instanceof List<?> bareRows) {
            if (defaultTableName == null) {
                throw new ParseException(origin + ": the document is a bare list of rows, so there "
                        + "is no table name in it. Either key the rows by table name, or load this "
                        + "dataset with an explicit table name.");
            }
            return List.of(new TableRows(defaultTableName, toRows(bareRows, defaultTableName, origin)));
        }
        if (root instanceof Map<?, ?> byTable) {
            List<TableRows> tables = new ArrayList<>();
            for (Map.Entry<?, ?> entry : byTable.entrySet()) {
                String table = String.valueOf(entry.getKey());
                if (!(entry.getValue() instanceof List<?> rows)) {
                    throw new ParseException(origin + ": table '" + table + "' must hold a list of "
                            + "rows, got " + describe(entry.getValue()));
                }
                tables.add(new TableRows(table, toRows(rows, table, origin)));
            }
            return tables;
        }
        throw new ParseException(origin + ": expected a map of table name to rows, or a list of "
                + "rows, got " + describe(root));
    }

    private static List<Map<String, Object>> toRows(List<?> rows, String table, String origin) {
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> columns)) {
                throw new ParseException(origin + ": every row of '" + table + "' must be a map of "
                        + "column name to value, got " + describe(row));
            }
            // LinkedHashMap, not Map.copyOf: a row may legitimately hold a null value, meaning an
            // explicit null, and Map.copyOf rejects those.
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> column : columns.entrySet()) {
                converted.put(String.valueOf(column.getKey()), column.getValue());
            }
            result.add(converted);
        }
        return result;
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
