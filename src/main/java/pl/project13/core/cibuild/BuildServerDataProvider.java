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
import pl.project13.core.PropertiesFilterer;
import pl.project13.core.log.LogInterface;
import pl.project13.core.util.PropertyManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

public abstract class BuildServerDataProvider {
  final LogInterface log;
  final Map<String, String> env;
  private String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
  private String dateFormatTimeZone = null;
  private String prefixDot = "";
  private List<String> excludeProperties = null;
  private List<String> includeOnlyProperties = null;
  private Map<String, Supplier<String>> additionalProperties = new HashMap<>();
  protected static final String BRANCH_REF_PREFIX = "refs/heads/";
  protected static final String PULL_REQUEST_REF_PREFIX = "refs/pull/";
  protected static final String TAG_REF_PREFIX = "refs/tags/";

  BuildServerDataProvider(@Nonnull LogInterface log, @Nonnull Map<String, String> env) {
    this.log = log;
    this.env = env;
  }

  public BuildServerDataProvider setDateFormat(@Nonnull String dateFormat) {
    this.dateFormat = dateFormat;
    return this;
  }

  public BuildServerDataProvider setDateFormatTimeZone(@Nonnull String dateFormatTimeZone) {
    this.dateFormatTimeZone = dateFormatTimeZone;
    return this;
  }

  public BuildServerDataProvider setPrefixDot(@Nonnull String prefixDot) {
    this.prefixDot = prefixDot;
    return this;
  }

  public BuildServerDataProvider setExcludeProperties(List<String> excludeProperties) {
    this.excludeProperties = excludeProperties;
    return this;
  }

  public BuildServerDataProvider setIncludeOnlyProperties(List<String> includeOnlyProperties) {
    this.includeOnlyProperties = includeOnlyProperties;
    return this;
  }

  public BuildServerDataProvider setAdditionalProperties(Map<String, Supplier<String>> additionalProperties) {
    this.additionalProperties = additionalProperties;
    return this;
  }

  /**
   * Get the {@link BuildServerDataProvider} implementation for the running environment
   *
   * @param env environment variables which get used to identify the environment
   * @param log logging provider which will be used to log events
   * @return the corresponding {@link BuildServerDataProvider} for your environment or {@link UnknownBuildServerData}
   */
  public static BuildServerDataProvider getBuildServerProvider(@Nonnull Map<String, String> env, @Nonnull LogInterface log) {
    if (BambooBuildServerData.isActiveServer(env)) {
      return new BambooBuildServerData(log, env);
    }
    if (GitlabBuildServerData.isActiveServer(env)) {
      return new GitlabBuildServerData(log, env);
    }
    if (HudsonJenkinsBuildServerData.isActiveServer(env)) {
      return new HudsonJenkinsBuildServerData(log, env);
    }
    if (TeamCityBuildServerData.isActiveServer(env)) {
      return new TeamCityBuildServerData(log, env);
    }
    if (TravisBuildServerData.isActiveServer(env)) {
      return new TravisBuildServerData(log, env);
    }
    if (AzureDevOpsBuildServerData.isActiveServer(env)) {
      return new AzureDevOpsBuildServerData(log, env);
    }
    if (CircleCiBuildServerData.isActiveServer(env)) {
      return new CircleCiBuildServerData(log, env);
    }
    if (GitHubBuildServerData.isActiveServer(env)) {
      return new GitHubBuildServerData(log, env);
    }
    if (AwsCodeBuildBuildServerData.isActiveServer(env)) {
      return new AwsCodeBuildBuildServerData(log, env);
    }
    if (BitbucketBuildServerData.isActiveServer(env)) {
      return new BitbucketBuildServerData(log, env);
    }
    return new UnknownBuildServerData(log, env);
  }

  public void loadBuildData(@Nonnull Properties properties, @Nullable Date reproducibleBuildOutputTimestamp) {
    loadBuildVersionAndTimeData(properties, reproducibleBuildOutputTimestamp);
    loadBuildHostData(properties);
    loadBuildNumber(properties);
  }

  /**
   * Fill the properties file build number environment variables which are supplied by build servers
   * build.number is the project specific build number, if this number is not available the unique number will be used
   * build.number.unique is a server wide unique build number
   *
   * @param properties a properties instance to put the entries on
   */
  abstract void loadBuildNumber(@Nonnull Properties properties);

  /**
   * @return the branch name provided by the server or an empty string
   */
  public abstract String getBuildBranch();

  private void loadBuildVersionAndTimeData(@Nonnull Properties properties, @Nullable Date reproducibleBuildOutputTimestamp) {
    Supplier<String> buildTimeSupplier = () -> {
      Date buildDate = (reproducibleBuildOutputTimestamp == null) ? new Date() : reproducibleBuildOutputTimestamp;
      SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
      if (dateFormatTimeZone != null) {
        smf.setTimeZone(TimeZone.getTimeZone(dateFormatTimeZone));
      }
      return smf.format(buildDate);
    };
    maybePut(properties, GitCommitPropertyConstant.BUILD_TIME, buildTimeSupplier);

    if (additionalProperties != null) {
      for (Map.Entry<String, Supplier<String>> e : additionalProperties.entrySet()) {
        maybePut(properties, e.getKey(), e.getValue());
      }
    }
  }

  private void loadBuildHostData(@Nonnull Properties properties) {
    Supplier<String> buildHostSupplier = () -> {
      String buildHost = null;
      try {
        // this call might be costly - try to avoid it too (similar concept as in GitDataProvider)
        buildHost = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        log.info(String.format("Unable to get build host, skipping property %s. Error message: %s",
            GitCommitPropertyConstant.BUILD_HOST,
            e.getMessage()));
      }
      return buildHost;
    };
    maybePut(properties, GitCommitPropertyConstant.BUILD_HOST, buildHostSupplier);
  }

  protected void maybePut(@Nonnull Properties properties, @Nonnull String key, Supplier<String> supplier) {
    String keyWithPrefix = prefixDot + key;
    if (properties.stringPropertyNames().contains(keyWithPrefix)) {
      String propertyValue = properties.getProperty(keyWithPrefix);
      log.info(String.format("Using cached %s with value %s", keyWithPrefix, propertyValue));
    } else if (PropertiesFilterer.isIncluded(keyWithPrefix, includeOnlyProperties, excludeProperties)) {
      String propertyValue = supplier.get();
      log.info(String.format("Collected %s with value %s", keyWithPrefix, propertyValue));
      PropertyManager.putWithoutPrefix(properties, keyWithPrefix, propertyValue);
    }
  }

}
