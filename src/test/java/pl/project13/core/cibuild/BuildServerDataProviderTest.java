package pl.project13.core.cibuild;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.project13.core.GitCommitPropertyConstant;
import pl.project13.core.log.DummyLogInterface;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class BuildServerDataProviderTest {
  @Test
  void shouldSelectGithubAsDataProvider() {
    Map<String, String> environment = Map.of("GITHUB_ACTIONS", "true");

    BuildServerDataProvider provider = BuildServerDataProvider.getBuildServerProvider(environment, new DummyLogInterface());

    assertThat(provider).isInstanceOf(GitHubBuildServerData.class);
  }

  @Nested
  class GithubProviderTests {
    @Test
    void shouldVerifyOnGithubEnvironment() {
      Map<String, String> environment = Map.of("GITHUB_ACTIONS", "true");

      assertThat(GitHubBuildServerData.isActiveServer(environment)).isTrue();
    }

    @Test
    void shouldLoadBuildNumber() {
      Properties properties = new Properties();
      Map<String, String> environment = Map.of("GITHUB_RUN_NUMBER", "123");
      GitHubBuildServerData provider = new GitHubBuildServerData(new DummyLogInterface(), environment);

      provider.loadBuildNumber(properties);

      assertThat(properties).containsEntry(GitCommitPropertyConstant.BUILD_NUMBER, "123");
    }

    @Test
    void shouldLoadBuildNumberAsEmptyIfNotAvailable() {
      Properties properties = new Properties();
      Map<String, String> environment = Map.of();
      GitHubBuildServerData provider = new GitHubBuildServerData(new DummyLogInterface(), environment);

      provider.loadBuildNumber(properties);

      assertThat(properties).containsEntry(GitCommitPropertyConstant.BUILD_NUMBER, "");
    }

    @Test
    void shouldLoadBranchNameForPullRequestBuild() {
      Map<String, String> environment = Map.of("GITHUB_REF", "refs/pull/feature_branch",
          "GITHUB_HEAD_REF", "feature_branch");
      GitHubBuildServerData provider = new GitHubBuildServerData(new DummyLogInterface(), environment);

      assertThat(provider.getBuildBranch()).isEqualTo("feature_branch");
    }

    @Test
    void shouldLoadBranchNameForBranchBuild() {
      Map<String, String> environment = Map.of("GITHUB_REF", "refs/heads/feature_branch");
      GitHubBuildServerData provider = new GitHubBuildServerData(new DummyLogInterface(), environment);

      assertThat(provider.getBuildBranch()).isEqualTo("feature_branch");
    }

    @Test
    void shouldLoadBranchNameAsEmptyIfNotAvailable() {
      Map<String, String> environment = Map.of();
      GitHubBuildServerData provider = new GitHubBuildServerData(new DummyLogInterface(), environment);

      assertThat(provider.getBuildBranch()).isEmpty();
    }
  }
}