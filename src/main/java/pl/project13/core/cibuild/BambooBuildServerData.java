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
import java.util.*;

public class BambooBuildServerData extends BuildServerDataProvider {

  BambooBuildServerData(LogInterface log, @Nonnull Map<String, String> env) {
    super(log, env);
  }

  /**
   * @param env The current system environment variables, obtained via System.getenv().
   * @return true, if the system environment variables contain the Bamboo specific environment variable; false otherwise
   * @see <a href="https://confluence.atlassian.com/bamboo/bamboo-variables-289277087.html#Bamboovariables-Build-specificvariables">Bamboo Variables</a>
   */
  public static boolean isActiveServer(Map<String, String> env) {
    return env.containsKey("bamboo_buildKey") ||
            env.containsKey("bamboo.buildKey") ||
            env.containsKey("BAMBOO_BUILDKEY");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    String buildNumber = Optional.ofNullable(env.get("bamboo.buildNumber"))
            .or(() -> Optional.ofNullable(env.get("BAMBOO_BUILDNUMBER")))
            .orElseGet(() -> env.getOrDefault("bamboo_buildNumber", ""));

    maybePut(properties, GitCommitPropertyConstant.BUILD_NUMBER, () -> buildNumber);
  }

  @Override
  public String getBuildBranch() {
    String environmentBasedKey = null;
    String environmentBasedBranch = null;

    for (String envKey : Arrays.asList(
            "bamboo.planRepository.branchName",
            "bamboo.planRepository.<position>.branchName",
            "BAMBOO_PLANREPOSITORY_BRANCH")) {
      environmentBasedBranch = env.get(envKey);
      if (environmentBasedBranch != null) {
        environmentBasedKey = envKey;
        break;
      }
    }
    log.info(String.format("Using environment variable based branch name. %s = %s", environmentBasedKey, environmentBasedBranch));
    return environmentBasedBranch;
  }
}
