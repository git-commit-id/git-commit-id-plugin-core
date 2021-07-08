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

import javax.annotation.Nonnull;
import java.io.File;
import java.util.EventListener;

public interface BuildFileChangeListener extends EventListener {
  /**
   * Event will be fired when then {@link pl.project13.core.PropertiesFileGenerator} changed the output file.
   * A client who may want to implement that listener may want to perform other actions.
   * E.g.
   * {code}
   * BuildContext buildContext = ...
   * new BuildFileChangeListener() {
   *   void changed(@Nonnull File file) {
   *     buildContext.refresh(file);
   *   }
   * }
   * {code}
   * @param file The output properties File that was changed by the generator
   */
  void changed(@Nonnull File file);
}
