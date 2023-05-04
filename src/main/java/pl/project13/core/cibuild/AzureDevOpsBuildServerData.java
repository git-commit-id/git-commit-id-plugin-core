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

import pl.project13.core.log.LogInterface;
import pl.project13.core.GitCommitPropertyConstant;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Properties;

public class AzureDevOpsBuildServerData extends BuildServerDataProvider {

  AzureDevOpsBuildServerData(@Nonnull LogInterface log, @Nonnull Map<String, String> env) {
    super(log, env);
  }

  /**
   * @param env The current system environment variables, obtained via System.getenv().
   * @return true, if the system environment variables contain the Azure specific environment variable; false otherwise
   * @see <a href="https://docs.microsoft.com/en-us/azure/devops/pipelines/build/variables?view=azure-devops&tabs=yaml#build-variables">Azure DevOps - Build variables</a>
   */
  public static boolean isActiveServer(@Nonnull Map<String, String> env) {
    return env.containsKey("AZURE_HTTP_USER_AGENT") || env.containsKey("TF_BUILD");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    String buildNumber = env.getOrDefault("BUILD_BUILDNUMBER", "");

    maybePut(properties, GitCommitPropertyConstant.BUILD_NUMBER, () -> buildNumber);
  }

  /**
   * Attempts to extract the branch name from the Build.SourceBranch
   *
   * The branch of the triggering repo the build was queued for. Some examples:
   * - Git repo branch: refs/heads/main
   * - Git repo pull request: refs/pull/1/merge
   * - TFVC repo branch: $/teamproject/main
   * - TFVC repo gated check-in: Gated_2016-06-06_05.20.51.4369;username@live.com
   * - TFVC repo shelveset build: myshelveset;username@live.com
   * - When your pipeline is triggered by a tag: refs/tags/your-tag-name
   */
  @Override
  public String getBuildBranch() {
    String environmentBasedBuildSourceBranch = env.get("BUILD_SOURCEBRANCH");
    if (environmentBasedBuildSourceBranch != null && !environmentBasedBuildSourceBranch.isEmpty()) {
      if (environmentBasedBuildSourceBranch.startsWith(BRANCH_REF_PREFIX)) {
        String branchName = environmentBasedBuildSourceBranch.substring(BRANCH_REF_PREFIX.length());
        log.info(String.format("Using environment variable based branch name. BUILD_SOURCEBRANCH = %s (branch = %s)",
            environmentBasedBuildSourceBranch, branchName));
        return branchName;
      }
      if (environmentBasedBuildSourceBranch.startsWith(PULL_REQUEST_REF_PREFIX)) {
        String branchName = environmentBasedBuildSourceBranch.substring(PULL_REQUEST_REF_PREFIX.length());
        log.info(String.format("Using environment variable based branch name. BUILD_SOURCEBRANCH = %s (branch = %s)",
            environmentBasedBuildSourceBranch, branchName));
        return branchName;
      }
      if (environmentBasedBuildSourceBranch.startsWith(TAG_REF_PREFIX)) {
        String branchName = environmentBasedBuildSourceBranch.substring(TAG_REF_PREFIX.length());
        log.info(String.format("Using environment variable based branch name. BUILD_SOURCEBRANCH = %s (branch = %s)",
            environmentBasedBuildSourceBranch, branchName));
        return branchName;
      }
    }
    return "";
  }
}
