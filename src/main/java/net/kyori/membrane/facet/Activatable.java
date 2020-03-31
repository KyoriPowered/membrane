/*
 * This file is part of membrane, licensed under the MIT License.
 *
 * Copyright (c) 2017-2020 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.kyori.membrane.facet;

import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface Activatable extends Facet {
  /**
   * A predicate that matches facets that are not {@link Activatable} or are {@link #active() active}.
   */
  Predicate<? extends Facet> PREDICATE = facet -> !(facet instanceof Activatable) || ((Activatable) facet).active();

  /**
   * Gets a predicate that matches facets that are not {@link Activatable} or are {@link #active() active}.
   *
   * @param <F> the facet type
   * @return a predicate
   * @see #PREDICATE
   */
  static <F extends Facet> @NonNull Predicate<F> predicate() {
    return (Predicate<F>) PREDICATE;
  }

  /**
   * Checks if this facet should be active.
   *
   * @return {@code true} if this facet should be active, {@code false} otherwise
   */
  boolean active();
}
