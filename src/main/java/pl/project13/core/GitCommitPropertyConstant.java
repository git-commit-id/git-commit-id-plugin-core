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

/**
 * A class that represents all properties that may be generated by the plugin and exposed to maven.
 */
public class GitCommitPropertyConstant {
  /**
   * Represents the current branch name. Falls back to commit-id for detached HEAD.
   *
   * Note: When an user uses the {@code evaluateOnCommit} property to gather the
   * branch for an arbitrary commit (really anything besides the default {@code HEAD})
   * this plugin will perform a {@code git branch --points-at} which might
   * return a comma separated list of branch names that points to the specified commit.
   */
  public static final String BRANCH = "branch";
  /**
   * Represents the count of commits that your local branch is ahead in perspective to the remote branch
   * (usually the case when your local branch has committed changes that are not pushed yet to the remote branch).
   *
   * <p>
   *
   * Note: To obtain the right value for this property this plugin should operate in online mode
   * ({@code <offline>false</offline>}) so a {@code git fetch} will be performed before retrieval.
   */
  public static final String LOCAL_BRANCH_AHEAD = "local.branch.ahead";
  /**
   * Represents the count of commits that your local branch is behind in perspective to the remote branch
   * (usually the case when there are commits in the remote branch that are not yet integrated into your local branch).
   *
   * <p>
   *
   * Note: To obtain the right value for this property this plugin should operate in online mode
   *  ({@code <offline>false</offline>}) so a {@code git fetch} will be performed before retrieval.
   */
  public static final String LOCAL_BRANCH_BEHIND = "local.branch.behind";
  /**
   * A working tree is said to be "dirty" if it contains modifications
   * which have not been committed to the current branch.
   */
  public static final String DIRTY = "dirty";
  /**
   * Represents the commit’s SHA-1 hash. Note this is exchangeable with the git.commit.id.full property
   * and might not be exposed. See {@code commitIdGenerationMode}.
   */
  public static final String COMMIT_ID_FLAT = "commit.id";
  /**
   * Represents the commit’s SHA-1 hash. Note this is exchangeable with the git.commit.id property
   * and might not be exposed. See {@code commitIdGenerationMode}.
   */
  public static final String COMMIT_ID_FULL = "commit.id.full";
  /**
   * Represents the abbreviated (shorten version) commit hash.
   */
  public static final String COMMIT_ID_ABBREV = "commit.id.abbrev";
  /**
   * Represents an object a human readable name based on a the commit
   * (provides {@code git describe} for the given commit).
   */
  public static final String COMMIT_DESCRIBE = "commit.id.describe";
  /**
   * Represents the same value as git.commit.id.describe,
   * just with the git hash part removed (the {@code g2414721} part from {@code git describe}).
   */
  public static final String COMMIT_SHORT_DESCRIBE = "commit.id.describe-short";
  /**
   * Represents the git user name that is configured where the properties have been generated.
   */
  public static final String BUILD_AUTHOR_NAME = "build.user.name";
  /**
   * Represents the git user eMail that is configured where the properties have been generated.
   */
  public static final String BUILD_AUTHOR_EMAIL = "build.user.email";
  /**
   * Represents the (formatted) timestamp when the last build was executed.
   * If written to the git.properties file represents the latest build time when that file was written / updated.
   */
  public static final String BUILD_TIME = "build.time";
  /**
   * Represents the project version of the current project.
   */
  public static final String BUILD_VERSION = "build.version";
  /**
   * Represents the hostname where the properties have been generated.
   */
  public static final String BUILD_HOST = "build.host";
  /**
   * The git.build.number* variables are available on some hosted CIs and can be used to identify the
   * "number" of the build. This represents a project specific build number.
   *
   * <p>
   *
   * Currently supported CIs:
   * <ul>
   *   <li>AWS CodeBuild</li>
   *   <li>Azure DevOps</li>
   *   <li>Bamboo</li>
   *   <li>Bitbucket Pipelines</li>
   *   <li>GitHub Actions</li>
   *   <li>Gitlab CI (Gitlab &gt;8.10 &amp; Gitlab CI &gt;0.5)</li>
   *   <li>Hudson/Jenkins</li>
   *   <li>TeamCity</li>
   *   <li>Travis</li>
   * </ul>
   */
  public static final String BUILD_NUMBER = "build.number";
  /**
   * The git.build.number* variables are available on some hosted CIs and can be used to identify the
   * "number" of the build. This represents a system wide unique build number.
   *
   * <p>
   *
   * Currently supported CIs:
   * <ul>
   *   <li>AWS CodeBuild</li>
   *   <li>Gitlab CI (Gitlab &gt;11.0)</li>
   *   <li>GitHub Actions</li>
   *   <li>TeamCity</li>
   *   <li>Travis</li>
   * </ul>
   */
  public static final String BUILD_NUMBER_UNIQUE = "build.number.unique";
  /**
   * Represents the user name of the user who performed the commit.
   */
  public static final String COMMIT_AUTHOR_NAME = "commit.user.name";
  /**
   * Represents the user eMail of the user who performed the commit.
   */
  public static final String COMMIT_AUTHOR_EMAIL = "commit.user.email";
  /**
   * Represents the raw body (unwrapped subject and body) of the commit message.
   * Similar to running
   * <pre>
   *     git log -1 --pretty=format:%B
   * </pre>
   */
  public static final String COMMIT_MESSAGE_FULL = "commit.message.full";
  /**
   * Represents the subject of the commit message - may <b>not</b> be suitable for filenames.
   * Similar to running
   * <pre>
   *     git log -1 --pretty=format:%s
   * </pre>
   */
  public static final String COMMIT_MESSAGE_SHORT = "commit.message.short";
  /**
   * Represents the (formatted) time stamp when the commit has been performed.
   */
  public static final String COMMIT_TIME = "commit.time";
  /**
   * Represents the (formatted) time stamp when the commit has been originally performed.
   */
  public static final String COMMIT_AUTHOR_TIME = "commit.author.time";
  /**
   * Represents the (formatted) time stamp when the commit has been performed.
   */
  public static final String COMMIT_COMMITTER_TIME = "commit.committer.time";
  /**
   * Represents the URL of the remote repository for the current git project.
   */
  public static final String REMOTE_ORIGIN_URL = "remote.origin.url";
  /**
   * Represents a list of tags which contain the specified commit.
   * Similar to running
   * <pre>
   *     git tag --contains
   * </pre>
   */
  public static final String TAGS = "tags";
  /**
   * Represents the name of the closest available tag.
   * The closest tag may depend on your git describe config that may or may not take lightweight tags into consideration.
   */
  public static final String CLOSEST_TAG_NAME = "closest.tag.name";
  /**
   * Represents the tag on the current commit.
   * Similar to running
   * <pre>
   *     git tag --points-at HEAD
   * </pre>
   */
  public static final String TAG = "tag";
  /**
   * Represents the number of commits to the closest available tag.
   * The closest tag may depend on your git describe config that may or may not take lightweight tags into consideration.
   */
  public static final String CLOSEST_TAG_COMMIT_COUNT = "closest.tag.commit.count";
  /**
   * Represents the total count of all commits in the current repository.
   * Similar to running
   * <pre>
   *     git rev-list HEAD --count
   * </pre>
   */
  public static final String TOTAL_COMMIT_COUNT = "total.commit.count";

}
