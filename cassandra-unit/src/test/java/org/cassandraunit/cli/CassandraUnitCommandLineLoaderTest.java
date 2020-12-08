package org.cassandraunit.cli;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.apache.commons.cli.CommandLine;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.cassandraunit.utils.FileTmpHelper;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CassandraUnitCommandLineLoaderTest {

    public void shouldPrintUsageWhenNoArgumentsSpecified() {
        String[] args = {};
        CassandraUnitCommandLineLoader.main(args);
        assertThat(CassandraUnitCommandLineLoader.isUsageBeenPrinted(), is(true));
    }

    @Test
    public void shouldLaunchCliAndGetFileAndGetHostAndPortOptions() throws Exception {
        String[] args = {"-f", "dataset.xsd", "-h", "myHost", "-p", "9160"};
        CassandraUnitCommandLineLoader.parseCommandLine(args);
        CommandLine commandLine = CassandraUnitCommandLineLoader.getCommandLine();
        assertThat(commandLine.getOptionValue("f"), is("dataset.xsd"));
        assertThat(commandLine.getOptionValue("file"), is("dataset.xsd"));
        assertThat(commandLine.getOptionValue("h"), is("myHost"));
        assertThat(commandLine.getOptionValue("host"), is("myHost"));
        assertThat(commandLine.getOptionValue("p"), is("9160"));
        assertThat(commandLine.getOptionValue("port"), is("9160"));
    }

    @Test
    public void shouldPrintUsageBecausePortOptionIsMissing() throws Exception {
        String[] args = {"-f", "dataset.xsd", "-h", "myHost", "-c", "TestCluster"};
        CassandraUnitCommandLineLoader.parseCommandLine(args);
        assertThat(CassandraUnitCommandLineLoader.isUsageBeenPrinted(), is(true));
    }

    @Test
    public void shouldPrintUsageBecauseHostOptionIsMissing() throws Exception {
        String[] args = {"-f", "dataset.xsd", "-p", "3160", "-c", "TestCluster"};
        CassandraUnitCommandLineLoader.parseCommandLine(args);
        assertThat(CassandraUnitCommandLineLoader.isUsageBeenPrinted(), is(true));
    }

    @Test
    public void shouldPrintUsageBecauseFileOptionIsMissing() throws Exception {
        String[] args = {"-h", "myHost", "-p", "9160", "-c", "TestCluster"};
        CassandraUnitCommandLineLoader.parseCommandLine(args);
        assertThat(CassandraUnitCommandLineLoader.isUsageBeenPrinted(), is(true));
    }

    @Test
    public void shouldPrintUsageBecauseHostArgumentIsMissing() throws Exception {
        String[] args = {"-h", "-p", "3160"};
        CassandraUnitCommandLineLoader.parseCommandLine(args);
        assertThat(CassandraUnitCommandLineLoader.isUsageBeenPrinted(), is(true));
    }

    @Test
    public void shouldPrintUsageBecausePortArgumentIsMissing() throws Exception {
        String[] args = {"-h", "myHost", "-p"};
        CassandraUnitCommandLineLoader.parseCommandLine(args);
        assertThat(CassandraUnitCommandLineLoader.isUsageBeenPrinted(), is(true));
    }

    @Test
    public void shouldLoadCQLDataSet() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();

        String targetFileDataSet = FileTmpHelper.copyClassPathDataSetToTmpDirectory(this.getClass(),
                "/cql/simpleWithKeyspaceCreation.cql");
        String host = "localhost";
        String port = "9142";
        String[] args = {"-f", targetFileDataSet, "-h", host, "-p", port};
        CassandraUnitCommandLineLoader.main(args);


        CqlSession session = EmbeddedCassandraServerHelper.getSession();

        ResultSet result = session.execute("select * from mykeyspace.testCQLTable WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570737");
        String val = result.iterator().next().getString("value");
        Assert.assertThat("Cql loaded string",is(val));

        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }
}
