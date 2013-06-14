package org.cassandraunit.dataset.xml;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Test;
/**
 * 
 * @author Jeremy Sevellec
 *
 */
public class XmlDataSetExampleTest {

	@Test
	public void shouldGenerateAnXmlDataSetDocument() throws JAXBException {

		Keyspace keyspace = new Keyspace();
		keyspace.setName("beautifulKeyspaceName");
		JAXBContext jc = JAXBContext.newInstance(Keyspace.class);
		Marshaller marshaller = jc.createMarshaller();
		StringWriter stringWriter = new StringWriter();
		marshaller.marshal(keyspace, stringWriter);
		System.out.println(stringWriter.toString());

	}

}
