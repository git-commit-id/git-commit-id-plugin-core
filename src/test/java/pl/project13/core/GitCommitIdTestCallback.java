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

import pl.project13.core.git.GitDescribeConfig;
import pl.project13.core.log.LogInterface;
import pl.project13.core.util.BuildFileChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;

public class GitCommitIdTestCallback {
  private Map<String, String> systemEnv = System.getenv();
  private String projectVersion = "dummy-version";
  private LogInterface logInterface = createDummyLogInterface();
  private String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
  private String dateFormatTimeZone = TimeZone.getDefault().getID();
  private String prefixDot = "git.";
  private List<String> excludeProperties = new ArrayList<>();
  private List<String> includeOnlyProperties = new ArrayList<>();
  private Date reproducibleBuildOutputTimestamp = new Date();
  private boolean useNativeGit = false;
  private long nativeGitTimeoutInMs = (30 * 1000);
  private int abbrevLength = 7;
  private GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, abbrevLength);
  private CommitIdGenerationMode commitIdGenerationMode = CommitIdGenerationMode.FLAT;
  private boolean useBranchNameFromBuildEnvironment = false;
  private boolean isOffline = true;
  private String evaluateOnCommit = "HEAD";
  private File dotGitDirectory;
  private boolean shouldGenerateGitPropertiesFile = false;
  private CommitIdPropertiesOutputFormat propertiesOutputFormat = CommitIdPropertiesOutputFormat.PROPERTIES;
  private String projectName = "dummy-project";
  private File projectBaseDir;
  private File generateGitPropertiesFilename;
  private Charset propertiesSourceCharset = StandardCharsets.UTF_8;
  private boolean shouldPropertiesEscapeUnicode = false;

  public GitCommitIdTestCallback() {
    try {
      this.projectBaseDir = Files.createTempDirectory("dummy-project-dir").toFile();
      this.generateGitPropertiesFilename = projectBaseDir.toPath().resolve("src/main/resources/git.properties").toFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public GitCommitIdTestCallback setSystemEnv(Map<String, String> systemEnv) {
    this.systemEnv = systemEnv;
    return this;
  }

  public GitCommitIdTestCallback setProjectVersion(String projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  public GitCommitIdTestCallback setLogInterface(LogInterface logInterface) {
    this.logInterface = logInterface;
    return this;
  }

  public GitCommitIdTestCallback setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
    return this;
  }

  public GitCommitIdTestCallback setDateFormatTimeZone(String dateFormatTimeZone) {
    this.dateFormatTimeZone = dateFormatTimeZone;
    return this;
  }

  public GitCommitIdTestCallback setPrefixDot(String prefixDot) {
    this.prefixDot = prefixDot;
    return this;
  }

  public GitCommitIdTestCallback setExcludeProperties(List<String> excludeProperties) {
    this.excludeProperties = excludeProperties;
    return this;
  }

  public GitCommitIdTestCallback setIncludeOnlyProperties(List<String> includeOnlyProperties) {
    this.includeOnlyProperties = includeOnlyProperties;
    return this;
  }

  public GitCommitIdTestCallback setReproducibleBuildOutputTimestamp(Date reproducibleBuildOutputTimestamp) {
    this.reproducibleBuildOutputTimestamp = reproducibleBuildOutputTimestamp;
    return this;
  }

  public GitCommitIdTestCallback setUseNativeGit(boolean useNativeGit) {
    this.useNativeGit = useNativeGit;
    return this;
  }

  public GitCommitIdTestCallback setNativeGitTimeoutInMs(long nativeGitTimeoutInMs) {
    this.nativeGitTimeoutInMs = nativeGitTimeoutInMs;
    return this;
  }

  public GitCommitIdTestCallback setAbbrevLength(int abbrevLength) {
    this.abbrevLength = abbrevLength;
    return this;
  }

  public GitCommitIdTestCallback setGitDescribeConfig(GitDescribeConfig gitDescribeConfig) {
    this.gitDescribeConfig = gitDescribeConfig;
    return this;
  }

  public GitCommitIdTestCallback setCommitIdGenerationMode(CommitIdGenerationMode commitIdGenerationMode) {
    this.commitIdGenerationMode = commitIdGenerationMode;
    return this;
  }

  public GitCommitIdTestCallback setUseBranchNameFromBuildEnvironment(boolean useBranchNameFromBuildEnvironment) {
    this.useBranchNameFromBuildEnvironment = useBranchNameFromBuildEnvironment;
    return this;
  }

  public GitCommitIdTestCallback setOffline(boolean offline) {
    this.isOffline = offline;
    return this;
  }

  public GitCommitIdTestCallback setEvaluateOnCommit(String evaluateOnCommit) {
    this.evaluateOnCommit = evaluateOnCommit;
    return this;
  }

  public GitCommitIdTestCallback setDotGitDirectory(File dotGitDirectory) {
    this.dotGitDirectory = dotGitDirectory;
    return this;
  }

  public GitCommitIdTestCallback setShouldGenerateGitPropertiesFile(boolean shouldGenerateGitPropertiesFile) {
    this.shouldGenerateGitPropertiesFile = shouldGenerateGitPropertiesFile;
    return this;
  }

  public GitCommitIdTestCallback setPropertiesOutputFormat(CommitIdPropertiesOutputFormat propertiesOutputFormat) {
    this.propertiesOutputFormat = propertiesOutputFormat;
    return this;
  }

  public GitCommitIdTestCallback setProjectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

  public GitCommitIdTestCallback setProjectBaseDir(File projectBaseDir) {
    this.projectBaseDir = projectBaseDir;
    return this;
  }

  public GitCommitIdTestCallback setGenerateGitPropertiesFilename(File generateGitPropertiesFilename) {
    this.generateGitPropertiesFilename = generateGitPropertiesFilename;
    return this;
  }

  public GitCommitIdTestCallback setPropertiesSourceCharset(Charset propertiesSourceCharset) {
    this.propertiesSourceCharset = propertiesSourceCharset;
    return this;
  }

  public GitCommitIdTestCallback setShouldPropertiesEscapeUnicode(boolean shouldPropertiesEscapeUnicode) {
    this.shouldPropertiesEscapeUnicode = shouldPropertiesEscapeUnicode;
    return this;
  }

  public GitCommitIdPlugin.Callback build() {
    return new GitCommitIdPlugin.Callback() {
      @Override
      public Map<String, String> getSystemEnv() {
        return systemEnv;
      }

      @Override
      public Supplier<String> supplyProjectVersion() {
        return () -> projectVersion;
      }

      @Nonnull
      @Override
      public LogInterface getLogInterface() {
        return logInterface;
      }

      @Nonnull
      @Override
      public String getDateFormat() {
        return dateFormat;
      }

      @Nonnull
      @Override
      public String getDateFormatTimeZone() {
        return dateFormatTimeZone;
      }

      @Nonnull
      @Override
      public String getPrefixDot() {
        return prefixDot;
      }

      @Override
      public List<String> getExcludeProperties() {
        return excludeProperties;
      }

      @Override
      public List<String> getIncludeOnlyProperties() {
        return includeOnlyProperties;
      }

      @Nullable
      @Override
      public Date getReproducibleBuildOutputTimestamp() throws GitCommitIdExecutionException {
        return reproducibleBuildOutputTimestamp;
      }

      @Override
      public boolean useNativeGit() {
        return useNativeGit;
      }

      @Override
      public long getNativeGitTimeoutInMs() {
        return nativeGitTimeoutInMs;
      }

      @Override
      public int getAbbrevLength() {
        return abbrevLength;
      }

      @Override
      public GitDescribeConfig getGitDescribe() {
        return gitDescribeConfig;
      }

      @Override
      public CommitIdGenerationMode getCommitIdGenerationMode() {
        return commitIdGenerationMode;
      }

      @Override
      public boolean getUseBranchNameFromBuildEnvironment() {
        return useBranchNameFromBuildEnvironment;
      }

      @Override
      public boolean isOffline() {
        return isOffline;
      }

      @Override
      public String getEvaluateOnCommit() {
        return evaluateOnCommit;
      }

      @Override
      public File getDotGitDirectory() {
        return dotGitDirectory;
      }

      @Override
      public boolean shouldGenerateGitPropertiesFile() {
        return shouldGenerateGitPropertiesFile;
      }

      @Override
      public void performPublishToAllSystemEnvironments(Properties properties) {
        // not implemented
      }

      @Override
      public void performPropertiesReplacement(Properties properties) {
        // not implemented
      }

      @Override
      public CommitIdPropertiesOutputFormat getPropertiesOutputFormat() {
        return propertiesOutputFormat;
      }

      @Override
      public BuildFileChangeListener getBuildFileChangeListener() {
        return file -> {
          // do nothing
        };
      }

      @Override
      public String getProjectName() {
        return projectName;
      }

      @Override
      public File getProjectBaseDir() {
        return projectBaseDir;
      }

      @Override
      public File getGenerateGitPropertiesFilename() {
        return generateGitPropertiesFilename;
      }

      @Override
      public Charset getPropertiesSourceCharset() {
        return propertiesSourceCharset;
      }

      @Override
      public boolean shouldPropertiesEscapeUnicode() {
        return shouldPropertiesEscapeUnicode;
      }
    };
  }

  private GitDescribeConfig createGitDescribeConfig(boolean forceLongFormat, int abbrev) {
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setTags(true);
    gitDescribeConfig.setForceLongFormat(forceLongFormat);
    gitDescribeConfig.setAbbrev(abbrev);
    return gitDescribeConfig;
  }

  private LogInterface createDummyLogInterface() {
    return new LogInterface() {
      @Override
      public void debug(String msg) {
        // ignore
      }

      @Override
      public void info(String msg) {
        // ignore
      }

      @Override
      public void warn(String msg) {
        // ignore
      }

      @Override
      public void error(String msg) {
        // ignore
      }

      @Override
      public void error(String msg, Throwable t) {
        // ignore
      }
    };
  }
}
