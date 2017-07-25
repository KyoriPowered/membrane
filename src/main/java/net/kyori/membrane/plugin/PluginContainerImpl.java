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
package net.kyori.membrane.plugin;

import com.google.common.base.MoreObjects;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkState;

final class PluginContainerImpl implements PluginContainer {

  private final Class<?> klass;
  private final String id;
  private final String version;
  private State state = State.CONSTRUCT;
  private Object instance;

  PluginContainerImpl(final Class<?> klass, final String id, final String version) {
    this.klass = klass;
    this.id = id;
    this.version = version;
  }

  void construct() throws IllegalAccessException, InstantiationException {
    this.expectState(State.CONSTRUCT);
    this.instance = this.klass.newInstance();
    this.state = State.ENABLE;
  }

  void enable() {
    this.expectState(State.ENABLE);
    if(this.instance instanceof ModularPlugin) {
      ((ModularPlugin) this.instance).enable();
    }
    this.state = State.ACTIVE;
  }

  void disable() {
    this.expectState(State.ACTIVE);
    if(this.instance instanceof ModularPlugin) {
      ((ModularPlugin) this.instance).disable();
    }
    this.state = null; // set to invalid state, nothing should happen after disable
  }

  private void expectState(final State expected) {
    checkState(this.state != null, "container is in an invalid state");
    checkState(this.state == expected, "expected container state to be %s, was %s", expected, this.state);
  }

  @Nonnull
  @Override
  public String id() {
    return this.id;
  }

  @Nonnull
  @Override
  public String version() {
    return this.version;
  }

  @Nonnull
  @Override
  public Object instance() {
    return this.instance;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("klass", this.klass)
      .add("id", this.id)
      .add("version", this.version)
      .add("state", this.state)
      .toString();
  }

  private enum State {
    CONSTRUCT,
    ENABLE,
    ACTIVE;
  }
}
