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
import pl.project13.core.CommitIdPropertiesOutputFormat;
import pl.project13.core.GitCommitIdExecutionException;
import pl.project13.core.PropertiesFileGenerator;
import pl.project13.core.log.LogInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Properties;

public class GenericFileManager {
  public static Properties readProperties(
      LogInterface log,
      CommitIdPropertiesOutputFormat propertiesOutputFormat,
      File gitPropsFile,
      Charset sourceCharset,
      String projectName
  ) throws GitCommitIdExecutionException {
    final Properties persistedProperties;

    try {
      switch (propertiesOutputFormat) {
        case JSON:
          log.info(String.format("Reading existing json file [%s] (for project %s)...", gitPropsFile.getAbsolutePath(), projectName));
          persistedProperties = JsonManager.readJsonProperties(gitPropsFile, sourceCharset);
          break;
        case PROPERTIES:
          log.info(String.format("Reading existing properties file [%s] (for project %s)...", gitPropsFile.getAbsolutePath(), projectName));
          persistedProperties = PropertyManager.readProperties(gitPropsFile);
          break;
        case XML:
          log.info(String.format("Reading existing xml file [%s] (for project %s)...", gitPropsFile.getAbsolutePath(), projectName));
          persistedProperties = XmlManager.readXmlProperties(gitPropsFile, sourceCharset);
          break;
        case YML:
          log.info(String.format("Reading existing yml file [%s] (for project %s)...", gitPropsFile.getAbsolutePath(), projectName));
          persistedProperties = YmlManager.readYmlProperties(gitPropsFile, sourceCharset);
          break;
        default:
          throw new GitCommitIdExecutionException("Not implemented:" + propertiesOutputFormat);
      }
    } catch (final CannotReadFileException ex) {
      // Read has failed, regenerate file
      throw new GitCommitIdExecutionException(
        String.format("Cannot read properties file [%s] (for project %s)...", gitPropsFile.getAbsolutePath(), projectName));
    }
    return persistedProperties;
  }

  public static void dumpProperties(
      LogInterface log,
      CommitIdPropertiesOutputFormat propertiesOutputFormat,
      File gitPropsFile,
      Charset sourceCharset,
      boolean escapeUnicode,
      String projectName,
      Properties propertiesToDump
  ) throws GitCommitIdExecutionException {
    try {
      Files.createDirectories(gitPropsFile.getParentFile().toPath());
      try (final OutputStream outputStream = new FileOutputStream(gitPropsFile)) {
        OrderedProperties sortedLocalProperties = PropertiesFileGenerator.createOrderedProperties();
        propertiesToDump.forEach((key, value) -> sortedLocalProperties.setProperty((String) key, (String) value));
        switch (propertiesOutputFormat) {
          case JSON:
            log.info(String.format("Writing json file to [%s] (for project %s)...", gitPropsFile.getAbsolutePath(), projectName));
            JsonManager.dumpJson(outputStream, sortedLocalProperties, sourceCharset);
            break;
          case PROPERTIES:
            log.info(String.format("Writing properties file to [%s] (for project %s)...", gitPropsFile.getAbsolutePath(), projectName));
            // using outputStream directly instead of outputWriter this way the UTF-8 characters appears in unicode escaped form
            PropertyManager.dumpProperties(outputStream, sortedLocalProperties, escapeUnicode);
            break;
          case XML:
            log.info(String.format("Writing xml file to [%s] (for project %s)...", gitPropsFile.getAbsolutePath(), projectName));
            // using outputStream directly instead of outputWriter this way the UTF-8 characters appears in unicode escaped form
            XmlManager.dumpXml(outputStream, sortedLocalProperties, sourceCharset);
            break;
          case YML:
            log.info(String.format("Writing yml file to [%s] (for project %s)...", gitPropsFile.getAbsolutePath(), projectName));
            // using outputStream directly instead of outputWriter this way the UTF-8 characters appears in unicode escaped form
            YmlManager.dumpYml(outputStream, sortedLocalProperties, sourceCharset);
            break;
          default:
            throw new GitCommitIdExecutionException("Not implemented:" + propertiesOutputFormat);
        }
      }
    } catch (final IOException ex) {
      throw new GitCommitIdExecutionException("Cannot create custom git properties file: " + gitPropsFile, ex);
    }
  }
}
