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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;

/**
 * Annotates a class as a plugin which should be loaded.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {

  /**
   * Gets the id of the plugin.
   *
   * @return the id
   */
  @Nonnull
  String id();

  /**
   * Gets the version of the plugin.
   *
   * @return the version
   */
  @Nonnull
  String version();

  /**
   * Gets the dependencies of the plugin.
   *
   * @return the dependencies
   */
  @Nonnull
  Dependency[] dependencies() default {};

  /**
   * A plugin dependency.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({}) // none
  @interface Dependency {

    /**
     * Gets the id of the dependency.
     *
     * @return the id
     */
    @Nonnull
    String id();

    /**
     * Gets the version range of the dependency.
     *
     * <p>The version should be specified as a maven version range.</p>
     *
     * <p>If an empty string is provided (this is the default behaviour), then any version
     * of the dependency will be accepted.</p>
     *
     * @return the version range
     * @see <a href="https://maven.apache.org/components/enforcer/enforcer-rules/versionRanges.html">version range specification</a>
     */
    @Nonnull
    String version() default "";
  }
}
