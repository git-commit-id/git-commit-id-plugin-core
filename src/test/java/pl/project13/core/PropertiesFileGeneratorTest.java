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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import pl.project13.core.log.LogInterface;
import pl.project13.core.util.BuildFileChangeListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class PropertiesFileGeneratorTest {
  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private PropertiesFileGenerator propertiesFileGenerator;
  
  @Before
  public void setUp() {
    LogInterface logInterface = mock(LogInterface.class);
    BuildFileChangeListener buildFileChangeListener = file -> {
      // Ignore
    };

    propertiesFileGenerator = new PropertiesFileGenerator(logInterface, buildFileChangeListener, CommitIdPropertiesOutputFormat.PROPERTIES, "", "test");
  }

  /**
   * Replaces {@code \n} with the OS-specific line break characters.
   */
  private static String convertLineBreaks(String s) {
    return s.replace("\n", System.lineSeparator());
  }

  @Test
  public void generatedPropertiesFileDoesNotEscapeUnicode() throws GitCommitIdExecutionException, IOException {
    Properties properties = new Properties();
    properties.put(GitCommitPropertyConstant.COMMIT_ID_FULL, "b5993378ffadd1f84dc8da220b9204d157ec0f29");
    properties.put(GitCommitPropertyConstant.BRANCH, "develop");
    properties.put(GitCommitPropertyConstant.COMMIT_MESSAGE_SHORT, "測試中文");

    Path propertiesPath = temporaryFolder.getRoot().toPath().resolve("git.properties");
    propertiesFileGenerator.maybeGeneratePropertiesFile(
            properties, temporaryFolder.getRoot(), propertiesPath.toFile(), UTF_8, false);

    String actualContent = Files.readString(propertiesPath, UTF_8);
    String expectedContent = convertLineBreaks("#Generated by Git-Commit-Id-Plugin\n"
            + "branch=develop\n"
            + "commit.id.full=b5993378ffadd1f84dc8da220b9204d157ec0f29\n"
            + "commit.message.short=測試中文\n");
    assertEquals(expectedContent, actualContent);
  }

  @Test
  public void generatedPropertiesFileEscapeUnicode() throws GitCommitIdExecutionException, IOException {
    Properties properties = new Properties();
    properties.put(GitCommitPropertyConstant.COMMIT_ID_FULL, "b5993378ffadd1f84dc8da220b9204d157ec0f29");
    properties.put(GitCommitPropertyConstant.BRANCH, "develop");
    properties.put(GitCommitPropertyConstant.COMMIT_MESSAGE_SHORT, "測試中文");

    Path propertiesPath = temporaryFolder.getRoot().toPath().resolve("git.properties");
    propertiesFileGenerator.maybeGeneratePropertiesFile(
            properties, temporaryFolder.getRoot(), propertiesPath.toFile(), UTF_8, true);

    String actualContent = Files.readString(propertiesPath, UTF_8);
    String expectedContent = convertLineBreaks("#Generated by Git-Commit-Id-Plugin\n"
            + "branch=develop\n"
            + "commit.id.full=b5993378ffadd1f84dc8da220b9204d157ec0f29\n"
            + "commit.message.short=\\u6E2C\\u8A66\\u4E2D\\u6587\n");
    assertEquals(expectedContent, actualContent);
  }
  
  @Test
  public void generatedPropertiesFileDoesNotContainDateComment() throws GitCommitIdExecutionException, IOException {
    Properties properties = new Properties();
    properties.put(GitCommitPropertyConstant.COMMIT_ID_FULL, "b5993378ffadd1f84dc8da220b9204d157ec0f29");
    properties.put(GitCommitPropertyConstant.BRANCH, "develop");
  
    Path propertiesPath = temporaryFolder.getRoot().toPath().resolve("git.properties");
    propertiesFileGenerator.maybeGeneratePropertiesFile(
            properties, temporaryFolder.getRoot(), propertiesPath.toFile(), UTF_8, true);
  
    String actualContent = Files.readString(propertiesPath, UTF_8);
    String expectedContent = convertLineBreaks("#Generated by Git-Commit-Id-Plugin\n"
        + "branch=develop\n"
        + "commit.id.full=b5993378ffadd1f84dc8da220b9204d157ec0f29\n");
    assertEquals(expectedContent, actualContent);
  }

  @Test
  public void rereadGeneratedPropertiesFile() throws GitCommitIdExecutionException, IOException {
    Properties properties = new Properties();
    properties.put(GitCommitPropertyConstant.COMMIT_ID_FULL, "b5993378ffadd1f84dc8da220b9204d157ec0f29");
    properties.put(GitCommitPropertyConstant.BRANCH, "develop");
  
    Path propertiesPath = temporaryFolder.getRoot().toPath().resolve("git.properties");
    propertiesFileGenerator.maybeGeneratePropertiesFile(
            properties, temporaryFolder.getRoot(), propertiesPath.toFile(), UTF_8, true);

    // Re-read the generated properties file.
    propertiesFileGenerator.maybeGeneratePropertiesFile(
            properties, temporaryFolder.getRoot(), propertiesPath.toFile(), UTF_8, true);

    String actualContent = Files.readString(propertiesPath, UTF_8);
    String expectedContent = convertLineBreaks("#Generated by Git-Commit-Id-Plugin\n"
        + "branch=develop\n"
        + "commit.id.full=b5993378ffadd1f84dc8da220b9204d157ec0f29\n");
    assertEquals(expectedContent, actualContent);
  }

  @Test
  public void worksWithRelativeFileLocation() throws GitCommitIdExecutionException, IOException {
    Properties properties = new Properties();
    properties.put(GitCommitPropertyConstant.COMMIT_ID_FULL, "b5993378ffadd1f84dc8da220b9204d157ec0f29");

    Path relativePath = new File("src/blah/blub/git.properties").toPath();
    propertiesFileGenerator.maybeGeneratePropertiesFile(
            properties, temporaryFolder.getRoot(), relativePath.toFile(), UTF_8, false);


    Path absolutePath = temporaryFolder.getRoot().toPath().resolve("src/blah/blub/git.properties");
    assertTrue(absolutePath.toFile().exists());
    String actualContent = Files.readString(absolutePath, UTF_8);
    String expectedContent = convertLineBreaks("#Generated by Git-Commit-Id-Plugin\n"
            + "commit.id.full=b5993378ffadd1f84dc8da220b9204d157ec0f29\n");
    assertEquals(expectedContent, actualContent);
  }
}
