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

import pl.project13.core.GitCommitPropertyConstant;
import pl.project13.core.log.LogInterface;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Properties;

public class GitHubBuildServerData extends BuildServerDataProvider {
  GitHubBuildServerData(LogInterface log, @Nonnull Map<String, String> env) {
    super(log, env);
  }

  /**
   * @param env The current system environment variables, obtained via System.getenv().
   * @return true, if the system environment variables contain the Github specific environment variable; false otherwise
   * @see <a href="https://help.github.com/en/actions/automating-your-workflow-with-github-actions/using-environment-variables">GitHubActionsUsingEnvironmentVariables</a>
   */
  public static boolean isActiveServer(Map<String, String> env) {
    return env.containsKey("GITHUB_ACTIONS");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    String buildNumber = env.getOrDefault("GITHUB_RUN_NUMBER", "");

    maybePut(properties, GitCommitPropertyConstant.BUILD_NUMBER, () -> buildNumber);
  }

  @Override
  public String getBuildBranch() {
    String gitHubRef = env.get("GITHUB_REF");
    if (gitHubRef != null && !gitHubRef.isEmpty()) {
      if (gitHubRef.startsWith(BRANCH_REF_PREFIX)) {
        String branchName = gitHubRef.substring(BRANCH_REF_PREFIX.length());
        log.info(String.format("Using environment variable based branch name. GITHUB_REF = %s (branch = %s)", gitHubRef, branchName));
        return branchName;
      }
      if (gitHubRef.startsWith(PULL_REQUEST_REF_PREFIX)) {
        String branchName = env.get("GITHUB_HEAD_REF");
        log.info(String.format("Using environment variable based branch name. GITHUB_HEAD_REF = %s", branchName));
        return branchName;
      }
    }
    return "";
  }
}
