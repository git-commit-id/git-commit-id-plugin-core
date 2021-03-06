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
import javax.annotation.Nullable;
import java.util.Objects;

public class Pair<A, B> {

  @Nonnull
  public final A first;

  @Nonnull
  public final B second;

  @SuppressWarnings("ConstantConditions")
  public Pair(A first, B second) {
    Objects.requireNonNull(first, "The first parameter must not be null.");
    Objects.requireNonNull(second, "The second parameter must not be null.");

    this.first = first;
    this.second = second;
  }

  @Nonnull
  public static <A, B> Pair<A, B> of(A first, B second) {
    return new Pair<>(first, second);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Pair<?,?> pair = (Pair<?,?>) o;

    if (!first.equals(pair.first)) {
      return false;
    }
    //noinspection RedundantIfStatement
    if (!second.equals(pair.second)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = first.hashCode();
    result = 31 * result + (second.hashCode());
    return result;
  }

  @Nonnull
  @Override
  public String toString() {
    return String.format("Pair(%s, %s)", first, second);
  }
}
