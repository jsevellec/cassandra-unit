package org.cassandraunit.dataset.rows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cassandraunit.dataset.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JSON row datasets, via jackson-databind - which the driver already depends on, so this format
 * costs nothing to support either.
 *
 * @author Jeremy Sevellec
 */
public class JsonRowParser implements RowDataSetParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public List<TableRows> parse(InputStream in, String defaultTableName, String origin) {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            // Read as plain maps and lists, the same shape snakeyaml produces, so RowDocuments and
            // RowBinder do not need to know which format they came from.
            return RowDocuments.toTables(MAPPER.readValue(reader, Object.class), defaultTableName, origin);
        } catch (IOException e) {
            throw new ParseException(origin + ": " + e.getMessage(), e);
        }
    }
}
