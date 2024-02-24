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

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.junit.*;
import org.junit.runner.RunWith;
import pl.project13.core.git.GitDescribeConfig;
import pl.project13.core.util.GenericFileManager;
import pl.project13.core.util.JsonManager;
import pl.project13.core.util.XmlManager;
import pl.project13.core.util.YmlManager;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitParamsRunner.class)
public class GitCommitIdPluginIntegrationTest {
  private static final boolean UseJGit = false;
  private static final boolean UseNativeGit = true;

  public static Collection<?> useNativeGit() {
    return asList(UseJGit, UseNativeGit);
  }

  public static Collection<?> useDirty() {
    return asList(true, false);
  }

  private Path sandbox;

  @Before
  public void setUp() throws Exception {
    sandbox = Files.createTempDirectory("sandbox.git-commit-id-core");
  }

  @After
  public void tearDown() throws Exception {
    if (sandbox != null) {
      deleteDir(sandbox);
    }
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldIncludeExpectedProperties(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertThat(properties).containsKey("git.branch");
    assertThat(properties).containsKey("git.dirty");
    assertThat(properties).containsKey("git.commit.id");
    assertThat(properties).containsKey("git.commit.id.abbrev");
    assertThat(properties).containsKey("git.build.user.name");
    assertThat(properties).containsKey("git.build.user.email");
    assertThat(properties).containsKey("git.commit.user.name");
    assertThat(properties).containsKey("git.commit.user.email");
    assertThat(properties).containsKey("git.commit.message.full");
    assertThat(properties).containsKey("git.commit.message.short");
    assertThat(properties).containsKey("git.commit.time");
    assertThat(properties).containsKey("git.remote.origin.url");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldExcludeAsConfiguredProperties(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setExcludeProperties(Arrays.asList("git.remote.origin.url", ".*.user.*"))
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then

    // explicitly excluded
    assertThat(properties).doesNotContainKey("git.remote.origin.url");

    // glob excluded
    assertThat(properties).doesNotContainKey("git.build.user.name");
    assertThat(properties).doesNotContainKey("git.build.user.email");
    assertThat(properties).doesNotContainKey("git.commit.user.name");
    assertThat(properties).doesNotContainKey("git.commit.user.email");

    // these stay
    assertThat(properties).containsKey("git.branch");
    assertThat(properties).containsKey("git.commit.id");
    assertThat(properties).containsKey("git.commit.id.abbrev");
    assertThat(properties).containsKey("git.commit.message.full");
    assertThat(properties).containsKey("git.commit.message.short");
    assertThat(properties).containsKey("git.commit.time");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldIncludeOnlyAsConfiguredProperties(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);
    
    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setCommitIdGenerationMode(CommitIdGenerationMode.FULL)
                    .setIncludeOnlyProperties(Arrays.asList("git.remote.origin.url", ".*.user.*", "^git.commit.id.full$"))
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // explicitly included
    assertThat(properties).containsKey("git.remote.origin.url");

    // glob included
    assertThat(properties).containsKey("git.build.user.name");
    assertThat(properties).containsKey("git.build.user.email");
    assertThat(properties).containsKey("git.commit.id.full");
    assertThat(properties).containsKey("git.commit.user.name");
    assertThat(properties).containsKey("git.commit.user.email");

    // these excluded
    assertThat(properties).doesNotContainKey("git.branch");
    assertThat(properties).doesNotContainKey("git.commit.id");
    assertThat(properties).doesNotContainKey("git.commit.id.abbrev");
    assertThat(properties).doesNotContainKey("git.commit.message.full");
    assertThat(properties).doesNotContainKey("git.commit.message.short");
    assertThat(properties).doesNotContainKey("git.commit.time");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldExcludeAndIncludeAsConfiguredProperties(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);
    
    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setIncludeOnlyProperties(Arrays.asList("git.remote.origin.url", ".*.user.*"))
                    .setExcludeProperties(Collections.singletonList("git.build.user.email"))
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then

    // explicitly included
    assertThat(properties).containsKey("git.remote.origin.url");

    // explicitly excluded -> overrules include only properties
    assertThat(properties).doesNotContainKey("git.build.user.email");

    // glob included
    assertThat(properties).containsKey("git.build.user.name");
    assertThat(properties).containsKey("git.commit.user.name");
    assertThat(properties).containsKey("git.commit.user.email");

    // these excluded
    assertThat(properties).doesNotContainKey("git.branch");
    assertThat(properties).doesNotContainKey("git.commit.id");
    assertThat(properties).doesNotContainKey("git.commit.id.full");
    assertThat(properties).doesNotContainKey("git.commit.id.abbrev");
    assertThat(properties).doesNotContainKey("git.commit.message.full");
    assertThat(properties).doesNotContainKey("git.commit.message.short");
    assertThat(properties).doesNotContainKey("git.commit.time");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldHaveNoPrefixWhenConfiguredPrefixIsEmptyStringAsConfiguredProperties(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);
    
    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setPrefixDot("")
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    // explicitly excluded
    assertThat(properties).doesNotContainKey("git.remote.origin.url");
    assertThat(properties).doesNotContainKey(".remote.origin.url");
    assertThat(properties).containsKey("remote.origin.url");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldSkipDescribeWhenConfiguredToDoSo(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);
    
    GitDescribeConfig config = new GitDescribeConfig();
    config.setSkip(true);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(config)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertThat(properties).doesNotContainKey("git.commit.id.describe");
  }
  
  @Test
  @Parameters(method = "useNativeGit")
  public void shouldNotUseBuildEnvironmentBranchInfoWhenParameterSet(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setUseBranchNameFromBuildEnvironment(false)
                    .build();
    Properties properties = new Properties();

    Map<String, String> env = new HashMap<>();

    env.put("JENKINS_URL", "http://myciserver.com");
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_LOCAL_BRANCH", "localbranch");

    // reset repo and force detached HEAD
    try (final Git git = git(dotGitDirectory)) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("b6a73ed").call();
      git.checkout().setCreateBranch(true).setName("test_branch").setForceRefUpdate(true).call();
    }

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.branch", "test_branch");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldUseJenkinsBranchInfoWhenAvailable(boolean useNativeGit) throws Exception {
    // given
    Map<String, String> env = new HashMap<>();

    String detachedHeadSha1 = "b6a73ed747dd8dc98642d731ddbf09824efb9d48";
    String ciUrl = "http://myciserver.com";

    // when
    // in a detached head state, getBranch() will return the SHA1...standard behavior
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, detachedHeadSha1);

    // again, SHA1 will be returned if we're in jenkins, but GIT_BRANCH is not set
    env.put("JENKINS_URL", ciUrl);
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, detachedHeadSha1);

    // now set GIT_BRANCH too and see that the branch name from env var is returned
    env.clear();
    env.put("JENKINS_URL", ciUrl);
    env.put("GIT_BRANCH", "mybranch");
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, "mybranch");

    // same, but for hudson
    env.clear();
    env.put("HUDSON_URL", ciUrl);
    env.put("GIT_BRANCH", "mybranch");
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, "mybranch");

    // now set GIT_LOCAL_BRANCH too and see that the branch name from env var is returned
    env.clear();
    env.put("JENKINS_URL", ciUrl);
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, "mylocalbranch");

    // same, but for hudson
    env.clear();
    env.put("HUDSON_URL", ciUrl);
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, "mylocalbranch");

    // GIT_BRANCH but no HUDSON_URL or JENKINS_URL
    env.clear();
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, detachedHeadSha1);
  }

  private void shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(boolean useNativeGit, Map<String, String> env, String expectedBranchName) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setSystemEnv(env)
                    .setUseBranchNameFromBuildEnvironment(true)
                    .build();
    Properties properties = new Properties();

    // given

    // reset repo and force detached HEAD
    try (final Git git = git(dotGitDirectory)) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("b6a73ed").call();
      git.checkout().setName("b6a73ed").setForceRefUpdate(true).call();
    }

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.branch", expectedBranchName);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldResolvePropertiesOnDefaultSettingsForNonPomProject(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);
    
    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertGitPropertiesPresentInProject(properties);
  }

  @Test(expected = GitCommitIdExecutionException.class)
  @Parameters(method = "useNativeGit")
  public void shouldFailWithExceptionWhenNoGitRepoFound(boolean useNativeGit) throws Exception {
    // given
    File emptyGitDir = sandbox.resolve("empty_git_dir").toFile();
    emptyGitDir.deleteOnExit();

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(emptyGitDir)
                    .setUseNativeGit(useNativeGit)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCustomPropertiesFileProperties(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS);

    File targetFilePath = sandbox.resolve("custom-git.properties").toFile();
    targetFilePath.delete();

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setShouldGenerateGitPropertiesFile(true)
                    .setGenerateGitPropertiesFilename(targetFilePath)
                    .build();
    Properties properties = new Properties();

    // when
    try {
      GitCommitIdPlugin.runPlugin(cb, properties);
      // then
      assertThat(targetFilePath).exists();
    } finally {
      targetFilePath.delete();
    }
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCustomPropertiesFileJson(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS);
    CommitIdPropertiesOutputFormat commitIdPropertiesOutputFormat = CommitIdPropertiesOutputFormat.JSON;
    
    File targetFilePath = sandbox.resolve("custom-git.json").toFile();
    targetFilePath.delete();

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setShouldGenerateGitPropertiesFile(true)
                    .setGenerateGitPropertiesFilename(targetFilePath)
                    .setPropertiesOutputFormat(commitIdPropertiesOutputFormat)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);
    // then
    assertThat(targetFilePath).exists();
    Properties p = GenericFileManager.readPropertiesAsUtf8(commitIdPropertiesOutputFormat, targetFilePath);
    assertThat(p).hasSizeGreaterThan(10);
    Assert.assertEquals(p, properties);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCustomPropertiesFileXml(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS);
    CommitIdPropertiesOutputFormat commitIdPropertiesOutputFormat = CommitIdPropertiesOutputFormat.XML;

    File targetFilePath = sandbox.resolve("custom-git.xml").toFile();
    targetFilePath.delete();

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setShouldGenerateGitPropertiesFile(true)
                    .setGenerateGitPropertiesFilename(targetFilePath)
                    .setPropertiesOutputFormat(commitIdPropertiesOutputFormat)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);
    // then
    assertThat(targetFilePath).exists();
    Properties p = GenericFileManager.readPropertiesAsUtf8(commitIdPropertiesOutputFormat, targetFilePath);
    assertThat(p).hasSizeGreaterThan(10);
    Assert.assertEquals(p, properties);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCustomPropertiesFileYml(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS);
    CommitIdPropertiesOutputFormat commitIdPropertiesOutputFormat = CommitIdPropertiesOutputFormat.YML;

    File targetFilePath = sandbox.resolve("custom-git.yml").toFile();
    targetFilePath.delete();

    GitCommitIdPlugin.Callback cb =
        new GitCommitIdTestCallback()
            .setDotGitDirectory(dotGitDirectory)
            .setUseNativeGit(useNativeGit)
            .setShouldGenerateGitPropertiesFile(true)
            .setGenerateGitPropertiesFilename(targetFilePath)
            .setPropertiesOutputFormat(commitIdPropertiesOutputFormat)
            .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);
    // then
    assertThat(targetFilePath).exists();
    Properties p = GenericFileManager.readPropertiesAsUtf8(commitIdPropertiesOutputFormat, targetFilePath);
    assertThat(p).hasSizeGreaterThan(10);
    Assert.assertEquals(p, properties);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateDescribeWithTagOnlyWhenForceLongFormatIsFalse(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
    gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "v1.0.0-dirty");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateDescribeWithTagOnlyWhenForceLongFormatIsFalseAndAbbrevLengthIsNonDefault(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 10);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "v1.0.0");

    assertPropertyPresentAndEqual(properties, "git.commit.id.describe-short", "v1.0.0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateDescribeWithTagAndZeroAndCommitIdWhenForceLongFormatIsTrue(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "v1.0.0-0-gde4db35");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateDescribeWithTagAndZeroAndCommitIdWhenForceLongFormatIsTrueAndAbbrevLengthIsNonDefault(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 10);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "v1.0.0-0-gde4db35917");

    assertPropertyPresentAndEqual(properties, "git.commit.id.describe-short", "v1.0.0-0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCommitIdAbbrevWithDefaultLength(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);
    
    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setAbbrevLength(7)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "de4db35");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCommitIdAbbrevWithNonDefaultLength(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);
    
    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setAbbrevLength(10)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertThat(properties).contains(entry("git.commit.id.abbrev", "de4db35917"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldFormatDate(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);
    
    String dateFormat = "MM/dd/yyyy";

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setDateFormat(dateFormat)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertGitPropertiesPresentInProject(properties);

    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
    String expectedDate = smf.format(new Date());
    assertThat(properties).contains(entry("git.build.time", expectedDate));
    assertThat(properties).contains(entry("git.commit.time", "08/19/2012"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldSkipGitDescribe(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    gitDescribeConfig.setSkip(true);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertThat(properties).doesNotContainKey("git.commit.id.describe");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldMarkGitDescribeAsDirty(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG_DIRTY);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertThat(properties).contains(entry("git.commit.id.describe", "v1.0.0-0-gde4db35" + dirtySuffix));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldAlwaysPrintGitDescribe(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    gitDescribeConfig.setAlways(true);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "0b0181b");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldWorkWithEmptyGitDescribe(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);
    
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertGitPropertiesPresentInProject(properties);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldWorkWithNullGitDescribe(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);

    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertGitPropertiesPresentInProject(properties);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldExtractTagsOnGivenCommit(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS);

    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    try (final Git git = git(dotGitDirectory)) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("d37a598").call();
    }

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertGitPropertiesPresentInProject(properties);

    assertThat(properties).containsKey("git.tags");
    assertThat(properties.get("git.tags").toString()).doesNotContain("refs/tags/");

    assertThat(Arrays.asList(properties.get("git.tags").toString().split(",")))
      .containsOnly("lightweight-tag", "newest-tag");
    assertPropertyPresentAndEqual(properties, "git.total.commit.count", "2");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldExtractTagsOnGivenCommitWithOldestCommit(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS);

    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();


    try (final Git git = git(dotGitDirectory)) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("9597545").call();
    }

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertGitPropertiesPresentInProject(properties);

    assertThat(properties).containsKey("git.tags");
    assertThat(properties.get("git.tags").toString()).doesNotContain("refs/tags/");

    assertThat(Arrays.asList(properties.get("git.tags").toString().split(",")))
      .containsOnly("annotated-tag", "lightweight-tag", "newest-tag");
    assertPropertyPresentAndEqual(properties, "git.total.commit.count", "1");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldExtractTagsOnHead(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);

    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertGitPropertiesPresentInProject(properties);

    assertThat(properties).containsKey("git.tags");
    assertThat(properties.get("git.tags").toString()).doesNotContain("refs/tags/");

    assertThat(Arrays.asList(properties.get("git.tags").toString().split(",")))
      .containsOnly("v1.0.0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void runGitDescribeWithMatchOption(boolean useNativeGit) throws Exception {
    // given
    String headCommitId = "b0c6d28b3b83bf7b905321bae67d9ca4c75a203f";
    Map<String,String> gitTagMap = new HashMap<>();
    gitTagMap.put("v1.0", "f830b5f85cad3d33ba50d04c3d1454e1ae469057");
    gitTagMap.put("v2.0", "0e3495783c56589213ee5f2ae8900e2dc1b776c4");

    for (Map.Entry<String,String> entry : gitTagMap.entrySet()) {
      String gitDescribeMatchNeedle = entry.getKey();
      String commitIdOfMatchNeedle = entry.getValue();

      GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
      gitDescribeConfig.setMatch(gitDescribeMatchNeedle);
      gitDescribeConfig.setAlways(false);

      File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_THREE_COMMITS_AND_TWO_TAGS_CURRENTLY_ON_COMMIT_WITHOUT_TAG);
      
      GitCommitIdPlugin.Callback cb =
              new GitCommitIdTestCallback()
                      .setDotGitDirectory(dotGitDirectory)
                      .setUseNativeGit(useNativeGit)
                      .setGitDescribeConfig(gitDescribeConfig)
                      .setCommitIdGenerationMode(CommitIdGenerationMode.FULL)
                      .build();
      Properties properties = new Properties();

      // when
      GitCommitIdPlugin.runPlugin(cb, properties);

      // then
      assertThat(properties).containsKey("git.commit.id.describe");
      assertThat(properties.getProperty("git.commit.id.describe")).startsWith(gitDescribeMatchNeedle);

      assertThat(headCommitId).isNotEqualTo(commitIdOfMatchNeedle);
      assertPropertyPresentAndEqual(properties, "git.commit.id.full", headCommitId);
      assertPropertyPresentAndEqual(properties, "git.total.commit.count", "3");
    }
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWhenOnATag(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
    gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.closest.tag.name", "v1.0.0");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.commit.count", "0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateTagInformationWhenOnATag(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
    gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.tag", "v1.0.0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWhenOnATagAndDirty(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG_DIRTY);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.closest.tag.name", "v1.0.0");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.commit.count", "0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWhenCommitHasTwoTags(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS);

    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    try (final Git git = git(dotGitDirectory)) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("d37a598").call();
    }

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    // AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS ==> Where the newest-tag was created latest
    assertPropertyPresentAndEqual(properties, "git.closest.tag.name", "newest-tag");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.commit.count", "0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldUseDateFormatTimeZone(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG_DIRTY);
    
    // RFC 822 time zone: Sign TwoDigitHours Minutes
    String dateFormat = "Z"; // we want only the timezone (formated in RFC 822) out of the dateformat (easier for asserts)
    String expectedTimeZoneOffset = "+0200";
    String executionTimeZoneOffset = "-0800";
    TimeZone expectedTimeZone = TimeZone.getTimeZone("GMT" + expectedTimeZoneOffset);
    TimeZone executionTimeZone = TimeZone.getTimeZone("GMT" + executionTimeZoneOffset);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .setDateFormat(dateFormat)
                    .setDateFormatTimeZone(expectedTimeZone.getID())
                    .build();
    Properties properties = new Properties();

    // override the default timezone for execution and testing
    TimeZone currentDefaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(executionTimeZone);

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.time", expectedTimeZoneOffset);

    assertPropertyPresentAndEqual(properties, "git.build.time", expectedTimeZoneOffset);

    // set the timezone back
    TimeZone.setDefault(currentDefaultTimeZone);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCommitIdOldFashioned(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG_DIRTY);
    
    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setCommitIdGenerationMode(CommitIdGenerationMode.FLAT)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertThat(properties).containsKey("git.commit.id");
    assertThat(properties).doesNotContainKey("git.commit.id.full");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void testDetectCleanWorkingDirectory(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.GIT_WITH_NO_CHANGES);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setCommitIdGenerationMode(CommitIdGenerationMode.FLAT)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertThat(properties.get("git.dirty")).isEqualTo("false");
    assertThat(properties).contains(entry("git.commit.id.describe", "85c2888")); // assert no dirtySuffix at the end!
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void testDetectDirtyWorkingDirectory(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_ONE_COMMIT);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setCommitIdGenerationMode(CommitIdGenerationMode.FLAT)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertThat(properties.get("git.dirty")).isEqualTo("true");
    assertThat(properties).contains(entry("git.commit.id.describe", "0b0181b" + dirtySuffix)); // assert dirtySuffix at the end!
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWithExcludeLightweightTagsForClosestTag(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 9);
    gitDescribeConfig.setDirty("-customDirtyMark");
    gitDescribeConfig.setTags(false); // exclude lightweight tags

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "b6a73ed");

    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "annotated-tag-2-gb6a73ed74-customDirtyMark");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.name", "annotated-tag");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.commit.count", "2");

    assertPropertyPresentAndEqual(properties, "git.total.commit.count", "3");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWithIncludeLightweightTagsForClosestTag(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 9);
    gitDescribeConfig.setDirty("-customDirtyMark");
    gitDescribeConfig.setTags(true); // include lightweight tags

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "b6a73ed");

    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "lightweight-tag-1-gb6a73ed74-customDirtyMark");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.name", "lightweight-tag");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.commit.count", "1");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWithIncludeLightweightTagsForClosestTagAndPreferAnnotatedTags(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 9);
    gitDescribeConfig.setDirty("-customDirtyMark");
    gitDescribeConfig.setTags(true); // include lightweight tags

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "b6a73ed");

    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "newest-tag-1-gb6a73ed74-customDirtyMark");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.name", "newest-tag");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.commit.count", "1");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWithIncludeLightweightTagsForClosestTagAndFilter(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 9);
    gitDescribeConfig.setDirty("-customDirtyMark");
    gitDescribeConfig.setTags(true); // include lightweight tags
    gitDescribeConfig.setMatch("light*");

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "b6a73ed");

    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "lightweight-tag-1-gb6a73ed74-customDirtyMark");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.name", "lightweight-tag");

    assertPropertyPresentAndEqual(properties, "git.closest.tag.commit.count", "1");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyEvalOnDifferentCommitWithParentOfHead(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 9);
    gitDescribeConfig.setDirty(null);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .setEvaluateOnCommit("HEAD^1")
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "e3d159d");

    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "e3d159dd7");

    assertPropertyPresentAndEqual(properties, "git.tags", "test_tag");

    assertPropertyPresentAndEqual(properties, "git.dirty", "true");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyEvalOnDifferentCommitWithBranchName(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 9);
    gitDescribeConfig.setDirty(null);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .setEvaluateOnCommit("test")
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "9cb810e");

    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "test_tag-0-g9cb810e57");

    assertPropertyPresentAndEqual(properties, "git.branch", "test");

    assertPropertyPresentAndEqual(properties, "git.tags", "test_tag");

    assertPropertyPresentAndEqual(properties, "git.dirty", "true");

    assertPropertyPresentAndEqual(properties, "git.total.commit.count", "2");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyEvalOnDifferentCommitWithTagName(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 9);
    gitDescribeConfig.setDirty(null);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .setEvaluateOnCommit("test_tag")
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "9cb810e");

    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "test_tag-0-g9cb810e57");

    assertPropertyPresentAndEqual(properties, "git.branch", "9cb810e57e2994f38c7ec6a698a31de66fdd9e24");

    assertPropertyPresentAndEqual(properties, "git.tags", "test_tag");

    assertPropertyPresentAndEqual(properties, "git.dirty", "true");

    assertPropertyPresentAndEqual(properties, "git.total.commit.count", "2");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyEvalOnDifferentCommitWithCommitHash(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 9);
    gitDescribeConfig.setDirty(null);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setGitDescribeConfig(gitDescribeConfig)
                    .setEvaluateOnCommit("9cb810e")
                    .build();
    Properties properties = new Properties();

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "9cb810e");

    assertPropertyPresentAndEqual(properties, "git.commit.id.describe", "test_tag-0-g9cb810e57");

    assertPropertyPresentAndEqual(properties, "git.branch", "test");

    assertPropertyPresentAndEqual(properties, "git.tags", "test_tag");

    assertPropertyPresentAndEqual(properties, "git.dirty", "true");

    assertPropertyPresentAndEqual(properties, "git.total.commit.count", "2");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyEvalOnCommitWithTwoBranches(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    .setEvaluateOnCommit("2343428")
                    .build();
    Properties properties = new Properties();

    // create a new branch on the current HEAD commit:
    //    2343428 - Moved master - Fri, 29 Nov 2013 10:38:34 +0100 (HEAD, branch: master)
    try (final Git git = git(dotGitDirectory)) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("2343428").call();
      git.checkout().setCreateBranch(true).setName("another_branch").setForceRefUpdate(true).call();
      git.checkout().setCreateBranch(true).setName("z_branch").setForceRefUpdate(true).call();
    }

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "2343428");
    assertPropertyPresentAndEqual(properties, "git.branch", "another_branch,master,z_branch");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyDetachedHeadIsNotReportedAsBranch(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH);

    GitCommitIdPlugin.Callback cb =
            new GitCommitIdTestCallback()
                    .setDotGitDirectory(dotGitDirectory)
                    .setUseNativeGit(useNativeGit)
                    // .setEvaluateOnCommit("HEAD") // do not change this
                    .build();
    Properties properties = new Properties();

    // detach head
    try (final Git git = git(dotGitDirectory)) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("2343428").call();
    }

    // when
    GitCommitIdPlugin.runPlugin(cb, properties);

    // then
    assertPropertyPresentAndEqual(properties, "git.commit.id.abbrev", "2343428");
    assertPropertyPresentAndEqual(properties, "git.branch", "master");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGeneratePropertiesWithMultiplePrefixesAndReactorProject(boolean useNativeGit) throws Exception {
    // given
    File dotGitDirectory = createTmpDotGitDirectory(AvailableGitTestRepo.ON_A_TAG);
    
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
    gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected

    Properties properties = new Properties();

    List<String> prefixes = Arrays.asList("prefix-one", "prefix-two");
    // when
    // simulate plugin execution with multiple prefixes
    // see https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/137#issuecomment-418144756
    for (String prefix: prefixes) {
      GitCommitIdPlugin.Callback cb =
              new GitCommitIdTestCallback()
                      .setDotGitDirectory(dotGitDirectory)
                      .setUseNativeGit(useNativeGit)
                      .setGitDescribeConfig(gitDescribeConfig)
                      .setPrefixDot(prefix + ".")
                      .build();

      GitCommitIdPlugin.runPlugin(cb, properties);
    }

    // then
    // since we inject into all reactors both projects should have both properties
    for (String prefix: prefixes) {
      assertPropertyPresentAndEqual(properties, prefix + ".commit.id.abbrev", "de4db35");

      assertPropertyPresentAndEqual(properties, prefix + ".closest.tag.name", "v1.0.0");

      assertPropertyPresentAndEqual(properties, prefix + ".closest.tag.commit.count", "0");
    }
  }

  @Test
  public void verifyAllowedCharactersForEvaluateOnCommit() {
    Pattern p = GitCommitIdPlugin.allowedCharactersForEvaluateOnCommit;
    assertTrue(p.matcher("5957e419d").matches());
    assertTrue(p.matcher("my_tag").matches());
    assertTrue(p.matcher("my-tag").matches());
    assertTrue(p.matcher("my.tag").matches());
    assertTrue(p.matcher("HEAD^1").matches());
    assertTrue(p.matcher("feature/branch").matches());

    assertFalse(p.matcher("; CODE INJECTION").matches());
    assertFalse(p.matcher("|exit").matches());
    assertFalse(p.matcher("&&cat /etc/passwd").matches());
  }

  private GitDescribeConfig createGitDescribeConfig(boolean forceLongFormat, int abbrev) {
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setTags(true);
    gitDescribeConfig.setForceLongFormat(forceLongFormat);
    gitDescribeConfig.setAbbrev(abbrev);
    gitDescribeConfig.setDirty("");
    return gitDescribeConfig;
  }

  private static void assertPropertyPresentAndEqual(Properties properties, String key, String expected) {
    assertThat(properties).containsEntry(key, expected);
  }

  private static void assertGitPropertiesPresentInProject(Properties properties) {
    assertThat(properties).containsKey("git.build.time");
    assertThat(properties).containsKey("git.build.host");
    assertThat(properties).containsKey("git.branch");
    assertThat(properties).containsKey("git.commit.id");
    assertThat(properties).containsKey("git.commit.id.abbrev");
    assertThat(properties).containsKey("git.commit.id.describe");
    assertThat(properties).containsKey("git.build.user.name");
    assertThat(properties).containsKey("git.build.user.email");
    assertThat(properties).containsKey("git.commit.user.name");
    assertThat(properties).containsKey("git.commit.user.email");
    assertThat(properties).containsKey("git.commit.message.full");
    assertThat(properties).containsKey("git.commit.message.short");
    assertThat(properties).containsKey("git.commit.time");
    assertThat(properties).containsKey("git.remote.origin.url");
    assertThat(properties).containsKey("git.closest.tag.name");
    assertThat(properties).containsKey("git.closest.tag.commit.count");
  }

  private File createTmpDotGitDirectory(@Nonnull AvailableGitTestRepo availableGitTestRepo) throws IOException {
    Path dotGitDirectory = sandbox.resolve(".git");
    deleteDir(dotGitDirectory);

    FileUtils.copyDirectory(availableGitTestRepo.getDir().getAbsoluteFile(), dotGitDirectory.toFile());

    return dotGitDirectory.toFile();
  }

  private void deleteDir(@Nonnull Path toBeDeleted) throws IOException {
    if (toBeDeleted.toFile().exists()) {
      FileUtils.forceDelete(toBeDeleted.toFile());
    }
  }

  private Git git(@Nonnull File dotGitDirectory) throws IOException {
    return Git.open(dotGitDirectory);
  }
}
