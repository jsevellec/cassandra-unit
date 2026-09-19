package org.cassandraunit.dataset.rows;

import java.util.List;
import java.util.Map;

/**
 * Rows destined for one table, as parsed from a dataset and before any type conversion.
 * <p>
 * Values are whatever the parser produced: YAML and JSON give native types ({@code String},
 * {@code Long}, {@code Boolean}, {@code List}, {@code Map}, {@code null}), CSV and XML give
 * strings. {@link RowBinder} is what reconciles either shape with the real column types.
 * <p>
 * A key absent from a row's map means <em>unset</em> - the column is left out of that row's INSERT
 * entirely, so nothing is written. A key present with a {@code null} value means an explicit null,
 * which writes a tombstone. The two are deliberately different.
 *
 * @author Jeremy Sevellec
 */
public record TableRows(String table, List<Map<String, Object>> rows) {
}
