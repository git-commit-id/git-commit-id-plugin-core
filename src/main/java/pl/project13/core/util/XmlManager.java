/*
 * This file is part of git-commit-id-plugin-core by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-plugin-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-plugin-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-plugin-core.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.core.util;

import nu.studer.java.util.OrderedProperties;
import pl.project13.core.CannotReadFileException;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.stream.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

public class XmlManager {
  protected static void dumpXml(OutputStream outputStream, OrderedProperties sortedLocalProperties, Charset sourceCharset) throws IOException {
    /*
    TODO get rid of custom indents and use
    https://ewernli.com/2009/06/18/stax-pretty-printer/
    https://stackoverflow.com/a/38371920
    */
    XMLOutputFactory fac = XMLOutputFactory.newInstance();

    try (Writer outputWriter = new OutputStreamWriter(outputStream, sourceCharset)) {
      XMLStreamWriter writer = fac.createXMLStreamWriter(outputWriter);
      // <?xml version="1.0" encoding="UTF-8"?>
      writer.writeStartDocument(StandardCharsets.UTF_8.toString(), "1.0");

      writer.writeStartElement("gitCommitIdPlugin");
      writer.writeCharacters("\n"); // indents

      for (Map.Entry e : sortedLocalProperties.entrySet()) {
        writer.writeCharacters("    "); // indents
        // <property key="git.branch" value="master"/>
        writer.writeEmptyElement("property");
        writer.writeAttribute("key", e.getKey().toString());
        writer.writeAttribute("value", e.getValue().toString());
        writer.writeCharacters("\n"); // indents
      }
      writer.writeCharacters("\n"); // indents
      writer.writeEndElement(); // </gitCommitIdPlugin>

      writer.writeEndDocument();
      writer.flush();
      writer.close();
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }

  }

  protected static Properties readXmlProperties(@Nonnull File xmlFile, Charset sourceCharset) throws CannotReadFileException {
    Properties retVal = new Properties();

    try (FileInputStream fis = new FileInputStream(xmlFile)) {
      try (InputStreamReader reader = new InputStreamReader(fis, sourceCharset)) {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#Java
        // factory.setProperty("http://apache.org/xml/features/disallow-doctype-decl", true); // not supported :/
        // https://docs.oracle.com/en/java/javase/11/security/java-api-xml-processing-jaxp-security-guide.html#JSSEC-GUID-88B04BE2-35EF-4F61-B4FA-57A0E9102342
        // Feature for Secure Processing (FSP)
        // factory.setProperty(XMLConstants.FEATURE_SECURE_PROCESSING, true);  // not supported :/
        // https://rules.sonarsource.com/java/RSPEC-2755
        // to be compliant, completely disable DOCTYPE declaration:
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        // or completely disable external entities declarations:
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        // or prohibit the use of all protocols by external entities:
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        // Other things we don't need
        factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);

        XMLStreamReader xmlReader = factory.createXMLStreamReader(reader);
        while (xmlReader.hasNext()) {
          if (xmlReader.next() == XMLStreamConstants.START_ELEMENT) {
            if (xmlReader.getLocalName().equals("property")) {
              String key = xmlReader.getAttributeValue(null, "key");
              String value = xmlReader.getAttributeValue(null, "value");
              retVal.setProperty(key, value);
            }
          }
        }
      } catch (XMLStreamException ex) {
        throw new CannotReadFileException(ex);
      }
    } catch (IOException e) {
      throw new CannotReadFileException(e);
    }
    return retVal;
  }
}
