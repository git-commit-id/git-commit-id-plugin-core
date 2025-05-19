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
      Map<String, String> environment = Map.of(
          "GITHUB_RUN_ID", "1658821493",
          "GITHUB_RUN_NUMBER", "123",
          "GITHUB_RUN_ATTEMPT", "1");
      GitHubBuildServerData provider = new GitHubBuildServerData(new DummyLogInterface(), environment);

      provider.loadBuildNumber(properties);

      assertThat(properties).containsEntry(GitCommitPropertyConstant.BUILD_NUMBER, "123.1");
      assertThat(properties).containsEntry(GitCommitPropertyConstant.BUILD_NUMBER_UNIQUE, "1658821493.123.1");
    }

    @Test
    void shouldLoadBuildNumberAsZerosIfNotAvailable() {
      Properties properties = new Properties();
      Map<String, String> environment = Map.of();
      GitHubBuildServerData provider = new GitHubBuildServerData(new DummyLogInterface(), environment);

      provider.loadBuildNumber(properties);

      assertThat(properties).containsEntry(GitCommitPropertyConstant.BUILD_NUMBER, "0.0");
      assertThat(properties).containsEntry(GitCommitPropertyConstant.BUILD_NUMBER_UNIQUE, "0.0.0");
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