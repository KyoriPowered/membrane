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
package net.kyori.membrane.facet.internal;

import com.google.inject.ImplementedBy;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.kyori.membrane.facet.Facet;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A collection of facets.
 */
@ImplementedBy(FacetsImpl.class)
public interface Facets {
  /**
   * Gets a stream of all bound facets.
   *
   * @return a stream of all bound facets
   */
  @NonNull Stream<? extends Facet> all();

  /**
   * Gets a stream of all bound facets of the specified type.
   *
   * @param type the type
   * @param <F> the type
   * @return a stream of all bound facets of the specified type
   */
  default <F extends Facet> @NonNull Stream<? extends F> of(@NonNull final Class<F> type) {
    return (Stream<? extends F>) this.all().filter(type::isInstance);
  }

  /**
   * Gets a stream of all bound facets of the specified type that match the specified predicate.
   *
   * @param type the type
   * @param predicate the predicate
   * @param <F> the type
   * @return a stream of all bound facets of the specified type
   */
  default <F extends Facet> @NonNull Stream<? extends F> of(@NonNull final Class<F> type, @NonNull final Predicate<F> predicate) {
    return this.of(type).filter(predicate);
  }

  /**
   * Enables all facets.
   */
  void enable();

  /**
   * Disables all facets.
   */
  void disable();
}
