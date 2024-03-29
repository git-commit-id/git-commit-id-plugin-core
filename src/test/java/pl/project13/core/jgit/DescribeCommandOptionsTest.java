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

package pl.project13.core.jgit;

import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.project13.core.git.GitDescribeConfig;
import pl.project13.core.log.LogInterface;

import static org.mockito.Mockito.*;

public class DescribeCommandOptionsTest {
  private static final String evaluateOnCommit = "HEAD";

  @Test
  public void abbrev_shouldVerifyLengthContract_failOn41() throws Exception {
    // given
    final Repository repo = mock(Repository.class);
    final int length = 41;
    final LogInterface logInterface = mock(LogInterface.class);

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      DescribeCommand.on(evaluateOnCommit, repo, logInterface).abbrev(length);
    });
  }

  @Test
  public void abbrev_shouldVerifyLengthContract_failOnMinus12() throws Exception {
    // given
    final Repository repo = mock(Repository.class);
    final int length = -12;
    final LogInterface logInterface = mock(LogInterface.class);

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      DescribeCommand.on(evaluateOnCommit, repo, logInterface).abbrev(length);
    });
  }

  @Test
  public void apply_shouldDelegateToAllOptions() throws Exception {
    // given
    final String devel = "DEVEL";
    final String match = "*";
    final int abbrev = 12;

    GitDescribeConfig config = new GitDescribeConfig(true, devel, match, abbrev, true, true);

    Repository repo = mock(Repository.class);
    final LogInterface logInterface = mock(LogInterface.class);
    DescribeCommand command = DescribeCommand.on(evaluateOnCommit, repo, logInterface);
    DescribeCommand spiedCommand = spy(command);

    // when
    spiedCommand.apply(config);

    // then
    verify(spiedCommand).always(eq(true));
    verify(spiedCommand).abbrev(eq(abbrev));
    verify(spiedCommand).dirty(eq(devel));
    verify(spiedCommand).tags(eq(true));
    verify(spiedCommand).forceLongFormat(eq(true));
  }
}