package org.cassandraunit.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class FileTmpHelper {

	public static String copyClassPathDataSetToTmpDirectory(Class c, String initialClasspathDataSetLocation)
			throws IOException {
		InputStream dataSetInputStream = c.getResourceAsStream(initialClasspathDataSetLocation);
		String dataSetFileName = StringUtils.substringAfterLast(initialClasspathDataSetLocation, "/");
		String tmpPath = FileUtils.getTempDirectoryPath() + "/cassandra-unit/dataset/";
		String targetDataSetPathFileName = tmpPath + dataSetFileName;
		FileUtils.copyInputStreamToFile(dataSetInputStream, new File(targetDataSetPathFileName));
		return targetDataSetPathFileName;
	}

}
