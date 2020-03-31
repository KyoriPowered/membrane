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

import com.google.inject.Guice;
import net.kyori.membrane.facet.internal.Facets;
import net.kyori.membrane.facet.internal.FacetsImpl;
import net.kyori.violet.AbstractModule;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FacetTest {
  @Test
  void test() {
    final Container container = new Container();

    Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        this.bind(Facets.class).to(FacetsImpl.class);

        this.bind(FacetA.class).to(FacetAImpl.class);

        final FacetBinder facets = new FacetBinder(this.binder());
        facets.addBinding().to(FacetA.class);
        facets.addBinding().to(FacetB.class);
        facets.addBinding().to(FacetC.class);

        this.requestInjection(container);
      }
    });

    assertEquals(3, container.facets.all().count()); // A, B, C
    assertEquals(1, container.facets.of(SomeFacet.class).count()); // A
    assertEquals(2, container.facets.of(Enableable.class).count()); // A, C
    assertEquals(2, container.facets.of(Connectable.class).count()); // B, C
  }

  private interface SomeFacet extends Facet {}

  private interface FacetA extends Enableable, SomeFacet {}
  private static class FacetAImpl implements FacetA {
    @Override public void enable() {}
    @Override public void disable() {}
  }
  private static class FacetB implements Connectable {
    @Override public void connect() {}
    @Override public void disconnect() {}
  }
  private static class FacetC implements Connectable, Enableable {
    @Override public void connect() {}
    @Override public void disconnect() {}
    @Override public void enable() {}
    @Override public void disable() {}
  }

  private static class Container {
    @Inject Facets facets;
  }
}
