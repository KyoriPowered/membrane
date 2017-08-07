/*
 * This file is part of membrane, licensed under the MIT License.
 *
 * Copyright (c) 2017 KyoriPowered
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

import net.kyori.membrane.facet.Connectable;
import net.kyori.membrane.facet.Enableable;
import net.kyori.membrane.facet.Facet;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class FacetsImpl implements Facets {

  private final Set<Facet> facets;

  @Inject
  protected FacetsImpl(final Set<Facet> facets) {
    this.facets = facets;
  }

  // TODO(kashike): order?
  @Override
  public void enable() {
    this.of(Enableable.class).forEach(Enableable::enable);
    this.of(Connectable.class).forEach(Connectable::connect);
  }

  // TODO(kashike): order?
  @Override
  public void disable() {
    this.of(Connectable.class).forEach(Connectable::disconnect);
    this.of(Enableable.class).forEach(Enableable::disable);
  }

  @Nonnull
  @Override
  public Stream<? extends Facet> all() {
    return this.facets.stream();
  }
}
