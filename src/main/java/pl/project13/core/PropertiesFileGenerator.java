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

package pl.project13.core;

import nu.studer.java.util.OrderedProperties;
import pl.project13.core.log.LogInterface;
import pl.project13.core.util.BuildFileChangeListener;
import pl.project13.core.util.JsonManager;
import pl.project13.core.util.PropertyManager;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Properties;

public class PropertiesFileGenerator {

  private LogInterface log;
  private BuildFileChangeListener buildFileChangeListener;
  private CommitIdPropertiesOutputFormat propertiesOutputFormat;
  private String prefixDot;
  private String projectName;

  public PropertiesFileGenerator(LogInterface log, BuildFileChangeListener buildFileChangeListener, CommitIdPropertiesOutputFormat propertiesOutputFormat, String prefixDot, String projectName) {
    this.log = log;
    this.buildFileChangeListener = buildFileChangeListener;
    this.propertiesOutputFormat = propertiesOutputFormat;
    this.prefixDot = prefixDot;
    this.projectName = projectName;
  }

  public void maybeGeneratePropertiesFile(
          @Nonnull Properties localProperties,
          File projectDir,
          File propsFile,
          Charset sourceCharset,
          boolean escapeUnicode
  ) throws GitCommitIdExecutionException {
    try {
      final File gitPropsFile = craftPropertiesOutputFile(projectDir, propsFile);
      final boolean isJsonFormat = CommitIdPropertiesOutputFormat.JSON.equals(propertiesOutputFormat);

      boolean shouldGenerate = true;

      if (gitPropsFile.exists()) {
        final Properties persistedProperties;

        try {
          if (isJsonFormat) {
            log.info(String.format("Reading existing json file [%s] (for module %s)...", gitPropsFile.getAbsolutePath(), projectName));
            persistedProperties = JsonManager.readJsonProperties(gitPropsFile, sourceCharset);
          } else {
            log.info(String.format("Reading existing properties file [%s] (for module %s)...", gitPropsFile.getAbsolutePath(), projectName));
            persistedProperties = PropertyManager.readProperties(gitPropsFile);
          }

          final Properties propertiesCopy = (Properties) localProperties.clone();

          final String buildTimeProperty = prefixDot + GitCommitPropertyConstant.BUILD_TIME;

          propertiesCopy.setProperty(buildTimeProperty, "");
          persistedProperties.setProperty(buildTimeProperty, "");

          shouldGenerate = !propertiesCopy.equals(persistedProperties);
        } catch (CannotReadFileException ex) {
          // Read has failed, regenerate file
          log.info(String.format("Cannot read properties file [%s] (for module %s)...", gitPropsFile.getAbsolutePath(), projectName));
          shouldGenerate = true;
        }
      }

      if (shouldGenerate) {
        Files.createDirectories(gitPropsFile.getParentFile().toPath());
        try (OutputStream outputStream = new FileOutputStream(gitPropsFile)) {
          OrderedProperties sortedLocalProperties = PropertiesFileGenerator.createOrderedProperties();
          localProperties.forEach((key, value) -> sortedLocalProperties.setProperty((String) key, (String) value));
          if (isJsonFormat) {
            log.info(String.format("Writing json file to [%s] (for module %s)...", gitPropsFile.getAbsolutePath(), projectName));
            JsonManager.dumpJson(outputStream, sortedLocalProperties, sourceCharset);
          } else {
            log.info(String.format("Writing properties file to [%s] (for module %s)...", gitPropsFile.getAbsolutePath(), projectName));
            // using outputStream directly instead of outputWriter this way the UTF-8 characters appears in unicode escaped form
            PropertyManager.dumpProperties(outputStream, sortedLocalProperties, escapeUnicode);
          }
        } catch (final IOException ex) {
          throw new RuntimeException("Cannot create custom git properties file: " + gitPropsFile, ex);
        }

        if (buildFileChangeListener != null) {
          buildFileChangeListener.changed(gitPropsFile);
        }

      } else {
        log.info(String.format("Properties file [%s] is up-to-date (for module %s)...", gitPropsFile.getAbsolutePath(), projectName));
      }
    } catch (IOException e) {
      throw new GitCommitIdExecutionException(e);
    }
  }

  public static OrderedProperties createOrderedProperties() {
    return new OrderedProperties.OrderedPropertiesBuilder()
            .withSuppressDateInComment(true)
            .withOrdering(Comparator.nullsLast(Comparator.naturalOrder()))
            .build();
  }

  /**
   * Used for up-to-date checks in maven plugin
   */
  public static File craftPropertiesOutputFile(File projectDir, File propsFile) {
    File returnPath;
    if (propsFile.isAbsolute()) {
      returnPath = propsFile;
    } else {
      returnPath = projectDir.toPath().resolve(propsFile.toPath()).toFile();
    }

    return returnPath;
  }
}
