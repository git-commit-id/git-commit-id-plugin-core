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

public class TravisBuildServerData extends BuildServerDataProvider {

  TravisBuildServerData(LogInterface log, @Nonnull Map<String, String> env) {
    super(log, env);
  }

  /**
   * @param env The current system environment variables, obtained via System.getenv().
   * @return true, if the system environment variables contain the Travis specific environment variable; false otherwise
   * @see <a href=https://docs.travis-ci.com/user/environment-variables/#Default-Environment-Variables>Travis</a>
   */
  public static boolean isActiveServer(@Nonnull Map<String, String> env) {
    return env.containsKey("TRAVIS");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    String buildNumber = env.getOrDefault("TRAVIS_BUILD_NUMBER", "");
    String uniqueBuildNumber = env.getOrDefault("TRAVIS_BUILD_ID", "");

    maybePut(properties, GitCommitPropertyConstant.BUILD_NUMBER, () -> buildNumber);
    maybePut(properties, GitCommitPropertyConstant.BUILD_NUMBER_UNIQUE, () -> uniqueBuildNumber);
  }

  @Override
  public String getBuildBranch() {
    String environmentBasedBranch = env.get("TRAVIS_BRANCH");
    log.info(String.format("Using environment variable based branch name. TRAVIS_BRANCH = %s", environmentBasedBranch));
    return environmentBasedBranch;
  }
}
