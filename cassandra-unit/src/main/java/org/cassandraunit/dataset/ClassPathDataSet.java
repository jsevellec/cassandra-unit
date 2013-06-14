package org.cassandraunit.dataset;

import org.apache.commons.lang.StringUtils;
import org.cassandraunit.dataset.json.ClassPathJsonDataSet;
import org.cassandraunit.dataset.xml.ClassPathXmlDataSet;
import org.cassandraunit.dataset.yaml.ClassPathYamlDataSet;
import org.cassandraunit.model.ColumnFamilyModel;
import org.cassandraunit.model.KeyspaceModel;

import java.util.List;

public class ClassPathDataSet implements DataSet {

    DataSet dataSet = null;

    public ClassPathDataSet(String dataSetLocation) {
        DataSetFileExtensionEnum dataSetExtensionEnum = getDataSetExtension(dataSetLocation);
        switch (dataSetExtensionEnum) {
            case xml:
                dataSet = new ClassPathXmlDataSet(dataSetLocation);
                break;
            case json:
                dataSet = new ClassPathJsonDataSet(dataSetLocation);
                break;
            case yaml:
                dataSet = new ClassPathYamlDataSet(dataSetLocation);
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
