/*
 * This file is part of membrane, licensed under the MIT License.
 *
 * Copyright (c) 2017-2018 KyoriPowered
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

import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import net.kyori.blizzard.NonNull;

/**
 * A facet binder.
 */
public class FacetBinder {
  /**
   * The facet set.
   */
  private final Multibinder<Facet> binder;

  /**
   * Creates a new facet binder.
   *
   * @param binder the binder
   * @return a new facet binder
   */
  @NonNull
  public static FacetBinder create(final Binder binder) {
    return new FacetBinder(binder);
  }

  protected FacetBinder(@NonNull final Binder binder) {
    this.binder = Multibinder.newSetBinder(binder, Facet.class);
  }

  /**
   * Returns a binding builder used to add a new element in the set.
   *
   * <p>Each bound element must have a distinct value.</p>
   *
   * @return a binding builder
   * @see Multibinder#addBinding()
   */
  @NonNull
  public LinkedBindingBuilder<Facet> add() {
    return this.binder.addBinding();
  }

  /**
   * Adds a facet to the set.
   *
   * @param facet the facet
   * @return this facet binder
   */
  public FacetBinder add(@NonNull final Class<? extends Facet> facet) {
    this.add().to(facet);
    return this;
  }
}
