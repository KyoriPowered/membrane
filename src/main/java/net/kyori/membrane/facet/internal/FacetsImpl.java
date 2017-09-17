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

import net.kyori.lunar.exception.Exceptions;
import net.kyori.membrane.facet.Activatable;
import net.kyori.membrane.facet.Connectable;
import net.kyori.membrane.facet.Enableable;
import net.kyori.membrane.facet.Facet;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class FacetsImpl implements Facets {

  private final Set<Facet> facets;
  @Nullable private Set<Entry> entries;

  @Inject
  protected FacetsImpl(final Set<Facet> facets) {
    this.facets = facets;
  }

  @Override
  public void enable() {
    if(this.entries != null) {
      throw new IllegalStateException("facets already enabled");
    }
    this.entries = this.facets.stream().map(Entry::new).collect(Collectors.toSet());
    this.entries.forEach(Exceptions.rethrowConsumer(Entry::enable));
  }

  @Override
  public void disable() {
    if(this.entries == null) {
      throw new IllegalStateException("facets were not enabled");
    }
    this.entries.forEach(Exceptions.rethrowConsumer(Entry::disable));
    this.entries = null;
  }

  @Nonnull
  @Override
  public Stream<? extends Facet> all() {
    return this.facets.stream();
  }

  private static class Entry {

    private final Facet facet;
    private boolean enabled;

    Entry(final Facet facet) {
      this.facet = facet;
    }

    void enable() throws IOException, TimeoutException {
      this.enabled = !(this.facet instanceof Activatable) || ((Activatable) this.facet).active();
      if(!this.enabled) {
        return;
      }

      if(this.facet instanceof Connectable) {
        ((Connectable) this.facet).connect();
      }

      if(this.facet instanceof Enableable) {
        ((Enableable) this.facet).enable();
      }
    }

    void disable() throws IOException, TimeoutException {
      if(!this.enabled) {
        return;
      }

      if(this.facet instanceof Enableable) {
        ((Enableable) this.facet).disable();
      }

      if(this.facet instanceof Connectable) {
        ((Connectable) this.facet).disconnect();
      }
    }
  }
}
