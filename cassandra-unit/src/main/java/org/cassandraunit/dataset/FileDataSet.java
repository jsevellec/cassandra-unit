package org.cassandraunit.dataset;

import org.apache.commons.lang.StringUtils;
import org.cassandraunit.dataset.json.FileJsonDataSet;
import org.cassandraunit.dataset.xml.FileXmlDataSet;
import org.cassandraunit.dataset.yaml.FileYamlDataSet;
import org.cassandraunit.model.ColumnFamilyModel;
import org.cassandraunit.model.KeyspaceModel;

import java.util.List;

public class FileDataSet implements DataSet {

    DataSet dataSet = null;

    public FileDataSet(String dataSetLocation) {
        DataSetFileExtensionEnum dataSetExtensionEnum = getDataSetExtension(dataSetLocation);
        switch (dataSetExtensionEnum) {
            case xml:
                dataSet = new FileXmlDataSet(dataSetLocation);
                break;
            case json:
                dataSet = new FileJsonDataSet(dataSetLocation);
                break;
            case yaml:
                dataSet = new FileYamlDataSet(dataSetLocation);
                break;
            default:
                throw new ParseException("dataSet file extension must be one of .xml, .json, .yaml");
        }
    }

    private DataSetFileExtensionEnum getDataSetExtension(String dataSetLocation) {
        String extension = StringUtils.substringAfterLast(dataSetLocation, ".");
        if (extension == null || extension.isEmpty()) {
            throw new ParseException("dataSet file extension must be one of .xml, .json, .yaml");
        }
        return DataSetFileExtensionEnum.valueOf(extension);
    }

    @Override
    public KeyspaceModel getKeyspace() {
        return dataSet.getKeyspace();
    }

    @Override
    public List<ColumnFamilyModel> getColumnFamilies() {

        return dataSet.getColumnFamilies();
    }

}
