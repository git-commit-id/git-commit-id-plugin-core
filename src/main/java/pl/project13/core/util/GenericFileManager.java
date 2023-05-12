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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Properties;

public class GenericFileManager {
  public static Properties readProperties(
      @Nullable LogInterface log,
      @Nonnull CommitIdPropertiesOutputFormat propertiesOutputFormat,
      @Nonnull File gitPropsFile,
      @Nonnull Charset sourceCharset,
      @Nullable String projectName
  ) throws GitCommitIdExecutionException {
    final Properties persistedProperties;

    try {
      if (log != null) {
        log.info(String.format("Reading existing %s file [%s] (for project %s)...",
          propertiesOutputFormat.name().toLowerCase(), gitPropsFile.getAbsolutePath(), projectName));
      }
      switch (propertiesOutputFormat) {
        case JSON:
          persistedProperties = JsonManager.readJsonProperties(gitPropsFile, sourceCharset);
          break;
        case PROPERTIES:
          persistedProperties = PropertyManager.readProperties(gitPropsFile);
          break;
        case XML:
          persistedProperties = XmlManager.readXmlProperties(gitPropsFile, sourceCharset);
          break;
        case YML:
          persistedProperties = YmlManager.readYmlProperties(gitPropsFile, sourceCharset);
          break;
        default:
          throw new GitCommitIdExecutionException("Not implemented:" + propertiesOutputFormat);
      }
    } catch (final CannotReadFileException ex) {
      // Read has failed, regenerate file
      throw new GitCommitIdExecutionException(
        String.format("Cannot read file [%s] (for project %s)...", gitPropsFile.getAbsolutePath(), projectName));
    }
    return persistedProperties;
  }

  public static void dumpProperties(
      @Nullable LogInterface log,
      @Nonnull CommitIdPropertiesOutputFormat propertiesOutputFormat,
      @Nonnull File gitPropsFile,
      @Nonnull Charset sourceCharset,
      boolean escapeUnicode,
      @Nullable String projectName,
      @Nonnull Properties propertiesToDump
  ) throws GitCommitIdExecutionException {
    try {
      if (log != null) {
        log.info(String.format("Writing %s file [%s] (for project %s)...",
          propertiesOutputFormat.name().toLowerCase(), gitPropsFile.getAbsolutePath(), projectName));
      }

      Files.createDirectories(gitPropsFile.getParentFile().toPath());
      try (final OutputStream outputStream = new FileOutputStream(gitPropsFile)) {
        OrderedProperties sortedLocalProperties = PropertiesFileGenerator.createOrderedProperties();
        propertiesToDump.forEach((key, value) -> sortedLocalProperties.setProperty((String) key, (String) value));
        switch (propertiesOutputFormat) {
          case JSON:
            JsonManager.dumpJson(outputStream, sortedLocalProperties, sourceCharset);
            break;
          case PROPERTIES:
            // using outputStream directly instead of outputWriter this way the UTF-8 characters appears in unicode escaped form
            PropertyManager.dumpProperties(outputStream, sortedLocalProperties, escapeUnicode);
            break;
          case XML:
            // using outputStream directly instead of outputWriter this way the UTF-8 characters appears in unicode escaped form
            XmlManager.dumpXml(outputStream, sortedLocalProperties, sourceCharset);
            break;
          case YML:
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
