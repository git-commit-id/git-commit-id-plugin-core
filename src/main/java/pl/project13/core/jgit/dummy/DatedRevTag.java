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

package pl.project13.core.jgit.dummy;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.revwalk.RevTag;
import java.time.Instant;

public class DatedRevTag {

  public final AnyObjectId id;
  public final String tagName;
  public final Instant date;

  public DatedRevTag(RevTag tag) {
    this(tag.getId(), tag.getTagName(), (tag.getTaggerIdent() != null) ? tag.getTaggerIdent().getWhen().toInstant() : Instant.now());
  }

  public DatedRevTag(AnyObjectId id, String tagName) {
    this(id, tagName, Instant.now());
  }

  public DatedRevTag(AnyObjectId id, String tagName, Instant date) {
    this.id = id;
    this.tagName = tagName;
    this.date = date;
  }

  @Override
  public String toString() {
    return "DatedRevTag{" +
        "id=" + id.name() +
        ", tagName='" + tagName + '\'' +
        ", date=" + date +
        '}';
  }
}
