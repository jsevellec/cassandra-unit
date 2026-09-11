package org.cassandraunit.dataset.cql;

import org.junit.Test;

import java.util.ArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class SimpleCQLLexerTest {

    @Test
    public void canRecognizeEscapedSingleQuotes() {
        ArrayList<String> inputText = new ArrayList<>();
        inputText.add("INSERT INTO table ('some''thing'); INSERT INTO table ('somethingElse');");

        final SimpleCQLLexer lexer = new SimpleCQLLexer(inputText);

        assertThat(lexer.getStatements()).hasSize(2);
    }

    @Test
    public void canRecognizeEscapedDoubleQuotes() {
        ArrayList<String> inputText = new ArrayList<>();
        inputText.add("INSERT INTO \"table \"\"A\" ('some''thing'); INSERT INTO \"table \"\"B\" ('somethingElse');");

        final SimpleCQLLexer lexer = new SimpleCQLLexer(inputText);

        assertThat(lexer.getStatements()).hasSize(2);
    }
}
