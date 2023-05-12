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
import pl.project13.core.util.*;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.Charset;
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
    final File gitPropsFile = craftPropertiesOutputFile(projectDir, propsFile);
    boolean shouldGenerate = true;

    if (gitPropsFile.exists()) {
      final Properties persistedProperties;
      try {
        persistedProperties = GenericFileManager.readProperties(
            log, propertiesOutputFormat, gitPropsFile, sourceCharset, projectName);
        final Properties propertiesCopy = (Properties) localProperties.clone();

        final String buildTimeProperty = prefixDot + GitCommitPropertyConstant.BUILD_TIME;

        propertiesCopy.setProperty(buildTimeProperty, "");
        persistedProperties.setProperty(buildTimeProperty, "");

        shouldGenerate = !propertiesCopy.equals(persistedProperties);
      } catch (GitCommitIdExecutionException e) {
        log.info(e.getMessage());
        shouldGenerate = true;
      }
    }

    if (shouldGenerate) {
      GenericFileManager.dumpProperties(
          log, propertiesOutputFormat, gitPropsFile, sourceCharset, escapeUnicode, projectName, localProperties);

      if (buildFileChangeListener != null) {
        buildFileChangeListener.changed(gitPropsFile);
      }
    } else {
      log.info(String.format("Properties file [%s] is up-to-date (for module %s)...", gitPropsFile.getAbsolutePath(), projectName));
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
