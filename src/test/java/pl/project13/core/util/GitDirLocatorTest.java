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

package pl.project13.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


public class GitDirLocatorTest {
  @TempDir
  public Path folder;

  @Test
  public void shouldUseTheManuallySpecifiedDirectory() throws Exception {
    // given
    File dotGitDir = folder.toFile();
    try {
      // when
      GitDirLocator locator = new GitDirLocator(dotGitDir, false, true);
      File foundDirectory = locator.lookupGitDirectory(dotGitDir);

      // then
      assertThat(foundDirectory).isNotNull();
      assertThat(foundDirectory.getAbsolutePath()).isEqualTo(dotGitDir.getAbsolutePath());
    } finally {
      if (!dotGitDir.delete()) {
        dotGitDir.deleteOnExit();
      }
    }
  }

  @Test
  public void shouldResolveRelativeSubmodule() throws Exception {
    // given
    folder.resolve("main-project").toFile().mkdirs();
    folder.resolve("main-project")
      .resolve(".git")
      .resolve("modules")
      .resolve("sub-module").toFile().mkdirs();
    folder.resolve("main-project").resolve("sub-module").toFile().mkdirs();

    // and a .git dir in submodule that points to the main's project .git/modules/submodule
    File dotGitDir = folder
        .resolve("main-project")
        .resolve("sub-module")
        .resolve(".git")
        .toFile();
    Files.write(
        dotGitDir.toPath(),
        "gitdir: ../.git/modules/sub-module".getBytes()
    );

    try {
      // when
      GitDirLocator locator = new GitDirLocator(dotGitDir, false, true);
      File foundDirectory = locator.lookupGitDirectory(dotGitDir);

      // then
      assertThat(foundDirectory).isNotNull();
      assertThat(
          foundDirectory.getCanonicalFile()
      ).isEqualTo(
          folder
          .resolve("main-project")
          .resolve(".git")
          .resolve("modules")
          .resolve("sub-module")
          .toFile()
      );
    } finally {
      if (!dotGitDir.delete()) {
        dotGitDir.deleteOnExit();
      }
    }
  }

  @Test
  public void testWorktreeResolution() {
    // tests to ensure we do not try to modify things that should not be modified
    String[] noopCases = {
      "",
      "a",
      "a/b",
      ".git/worktrees",
      ".git/worktrees/",
      "a.git/worktrees/b",
      ".git/modules",
      ".git/modules/",
      "a.git/modules/b",
    };
    for (String path : noopCases) {
      assertThat(GitDirLocator.resolveWorktree(new File(path))).isEqualTo(new File(path));
    }
    // tests that worktree resolution works
    assertThat(GitDirLocator.resolveWorktree(new File("a/.git/worktrees/b")))
        .isEqualTo(new File("a/.git"));
    assertThat(GitDirLocator.resolveWorktree(new File("/a/.git/worktrees/b")))
        .isEqualTo(new File("/a/.git"));
  }
}
