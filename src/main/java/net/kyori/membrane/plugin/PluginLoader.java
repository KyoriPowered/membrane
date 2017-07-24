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
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import net.kyori.lunar.exception.Exceptions;
import net.kyori.membrane.util.ClassLoaderInjector;
import net.kyori.version.DefaultArtifactVersion;
import net.kyori.version.InvalidVersionSpecificationException;
import net.kyori.version.VersionRange;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

/**
 * A plugin loader.
 */
public final class PluginLoader implements PluginFinder {

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginLoader.class);
  private static final String PLUGIN_DEFINITION_TYPE = "Lnet/kyori/membrane/plugin/Plugin;";
  private static final String DEPENDENCY_DEFINITION_TYPE = "Lnet/kyori/membrane/plugin/Plugin$Dependency;";
  private static final String CLASS_EXTENSION = ".class";
  private static final String JAR_EXTENSION = ".jar";
  private static final String FILE_PROTOCOL = "file";
  private static final String JAVA_HOME = System.getProperty("java.home");
  private final List<Candidate> candidates = new ArrayList<>();
  private final List<PluginContainerImpl> containers = new ArrayList<>();
  private final Map<String, PluginContainerImpl> namedContainers = new HashMap<>();
  private final Supplier<SimpleFileVisitor<Path>> classPathVisitor = Suppliers.memoize(() -> new SimpleFileVisitor<Path>() {
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
      if(!file.toString().endsWith(CLASS_EXTENSION)) {
        return FileVisitResult.CONTINUE;
      }
      try(final InputStream is = Files.newInputStream(file)) {
        PluginLoader.this.find(Source.CLASSPATH, is);
      }
      return FileVisitResult.CONTINUE;
    }
  });
  @Nonnull private final ClassLoaderInjector classLoaderInjector;
  private State state = State.FIND;

  public PluginLoader(@Nonnull final ClassLoaderInjector classLoaderInjector) {
    this.classLoaderInjector = classLoaderInjector;
  }

  /**
   * Search {@code directory} for plugin candidates.
   *
   * @param directory the directory to search
   * @throws IOException if an exception occurred while searching for plugin candidates
   */
  public void findAll(@Nonnull final Path directory) throws IOException {
    this.ensureFinding();
    LOGGER.debug("Searching directory '{}' for plugin candidates...", directory.toAbsolutePath());
    try(final DirectoryStream<Path> stream = Files.newDirectoryStream(directory, entry -> entry.toString().endsWith(JAR_EXTENSION))) {
      for(final Path path : stream) {
        this.find(path);
      }
    }
  }

  /**
   * Search {@code loader} for plugin candidates.
   *
   * @param loader the class loader to search
   * @throws IOException if an exception occurred while searching for plugin candidates
   */
  public void findClassPath(@Nonnull final URLClassLoader loader) throws IOException {
    this.ensureFinding();
    LOGGER.debug("Searching classpath of classloader '{}' for plugin candidates...", loader);
    Arrays.stream(loader.getURLs())
      // ignore urls which have a protocol other than 'file'
      .filter(url -> url.getProtocol().equals(FILE_PROTOCOL))
      // ignore java libraries
      .filter(url -> !url.getProtocol().startsWith(JAVA_HOME))
      .map(Exceptions.rethrowFunction(URL::toURI))
      .map(Paths::get)
      .forEach(Exceptions.rethrowConsumer(path -> {
        if(Files.isDirectory(path)) {
          Files.walkFileTree(path, Collections.singleton(FileVisitOption.FOLLOW_LINKS), 50, this.classPathVisitor.get());
        } else {
          this.find(Source.CLASSPATH, path);
        }
      }));
  }

  private void find(@Nonnull final Path path) throws IOException {
    this.find(Source.path(path), path);
  }

  private void find(@Nonnull final Source source, @Nonnull final Path path) throws IOException {
    try(final JarFile jar = new JarFile(path.toFile())) {
      final List<JarEntry> entries = Collections.list(jar.entries()).stream()
        .filter(entry -> !entry.isDirectory())
        .filter(entry -> entry.getName().endsWith(CLASS_EXTENSION))
        .collect(Collectors.toList());
      for(final JarEntry entry : entries) {
        try(final InputStream is = jar.getInputStream(entry)) {
          this.find(source, is);
        }
      }
    }
  }

  private void find(@Nonnull final Source source, @Nonnull final InputStream is) throws IOException {
    final ClassReader cr = new ClassReader(is);
    cr.accept(new Visitor(source), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
  }

  /**
   * Load plugin candidates into plugin containers.
   *
   * <p>Candidate sources will be injected into the classloader provided by the {@link #classLoaderInjector classloader injector}.</p>
   *
   * @return {@code true} if candidates have been loaded into containers, {@code false} if not
   */
  public boolean load() {
    // prevent loading containers twice
    if(this.state != State.FIND) {
      throw new IllegalStateException("Candidates have already been loaded into containers");
    }
    // prevent candidates from being found after loading has been done
    this.state = State.CONSTRUCT;
    // ensure dependencies are satisfied first
    this.ensureDependenciesSatisfied();
    // build a dependency graph
    final MutableGraph<Candidate> graph = GraphBuilder.directed().build();
    for(final Candidate candidate : this.candidates) {
      graph.addNode(candidate);
      for(final Dependency dependency : candidate.dependencies) {
        graph.putEdge(candidate, dependency.candidate);
      }
    }
    final List<Candidate> candidates = new ArrayList<>(this.candidates.size());
    for(final EndpointPair<Candidate> edge : graph.edges()) {
      if(!candidates.contains(edge.nodeU()) && !candidates.contains(edge.nodeV())) {
        candidates.add(edge.nodeV());
        candidates.add(edge.nodeU());
      }
    }
    for(final Candidate candidate : candidates) {
      // inject candidate source into classloader
      candidate.source.inject(this.classLoaderInjector);
      final PluginContainerImpl container;
      try {
        final Class<?> klass = Class.forName(candidate.className, true, this.classLoaderInjector.classLoader());
        container = new PluginContainerImpl(klass, candidate.id, candidate.version);
      } catch(final ClassNotFoundException e) {
        LOGGER.error(String.format("Encountered an exception while constructing the container for plugin '%s'", candidate.id), e);
        return false;
      }
      try {
        container.construct();
      } catch(final Throwable t) {
        LOGGER.error(String.format("Encountered an exception while constructing plugin '%s'", container.id()), t);
        return false;
      }
      this.containers.add(container);
      this.namedContainers.put(container.id(), container);
    }
    this.state = State.ENABLE;
    return true;
  }

  /**
   * Enable loaded containers.
   */
  public void enable() {
    this.ensureState(State.ENABLE);
    this.containers.forEach(PluginContainerImpl::enable);
    this.state = State.ACTIVE;
  }

  /**
   * Disable loaded containers.
   */
  public void disable() {
    this.ensureState(State.ACTIVE);
    Lists.reverse(this.containers).forEach(PluginContainerImpl::disable);
    this.state = null; // set to invalid state, nothing should happen after disable
  }

  @Nonnull
  @Override
  public Collection<PluginContainer> all() {
    return Collections.unmodifiableList(this.containers);
  }

  @Nullable
  @Override
  public PluginContainer find(@Nonnull final String id) {
    this.ensureState(State.ACTIVE);
    return this.namedContainers.get(id);
  }

  private void ensureDependenciesSatisfied() {
    final Map<String, Candidate> candidates = this.candidates.stream().collect(Collectors.toMap(input -> input.id, Function.identity()));
    for(final Candidate candidate : candidates.values()) {
      for(final Dependency dependency : candidate.dependencies) {
        if(candidate.id.equals(dependency.id)) {
          throw new IllegalStateException(String.format("The plugin '%s' has defined a dependency against itself", candidate.id));
        }
        @Nullable final Candidate dependencyCandidate = candidates.get(dependency.id);
        if(dependencyCandidate == null) {
          throw new IllegalArgumentException(String.format("The plugin '%s' has defined a dependency on '%s', which is missing", candidate.id, dependency.id));
        }
        if(dependency.version != null && !dependency.version.containsVersion(new DefaultArtifactVersion(dependencyCandidate.version))) {
          throw new IllegalArgumentException(String.format("The plugin '%s' has defined a dependency on '%s' version '%s', but version '%s' was found", candidate.id, dependency.id, dependency.version.toString(), dependencyCandidate.version));
        }
        dependency.candidate = dependencyCandidate;
      }
    }
  }

  private void ensureFinding() {
    if(this.state != State.FIND) {
      throw new IllegalStateException("Cannot find candidates after containers have been loaded");
    }
  }

  private void ensureState(final State expected) {
    checkState(this.state != null, "loader is in an invalid state");
    checkState(this.state == expected, "expected loader state to be %s, was %s", expected, this.state);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("candidates", this.candidates)
      .add("state", this.state)
      .toString();
  }

  private enum State {
    FIND,
    CONSTRUCT,
    ENABLE,
    ACTIVE;
  }

  private final class Visitor extends ClassVisitor {

    private final List<Dependency> dependencies = new ArrayList<>();
    private final Source source;
    private String className;
    private String id;
    private String version;
    private boolean nested;

    Visitor(final Source source) {
      super(Opcodes.ASM5);
      this.source = source;
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
      this.className = name.replace('/', '.');
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
      if(!visible || !desc.equals(PLUGIN_DEFINITION_TYPE)) {
        return null;
      }
      return new AnnotationVisitor(Opcodes.ASM5) {
        @Override
        public void visit(final String name, final Object value) {
          switch(name) {
            case "id":
              Visitor.this.id = (String) value;
              break;
            case "version":
              Visitor.this.version = (String) value;
              break;
          }
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String name, final String desc) {
          if(desc.equals(DEPENDENCY_DEFINITION_TYPE)) {
            return new AnnotationVisitor(Opcodes.ASM5) {
              private String id;
              private String version;

              @Override
              public void visit(final String name, final Object value) {
                switch(name) {
                  case "id":
                    this.id = (String) value;
                    break;
                  case "version":
                    this.version = (String) value;
                    break;
                }
              }

              @Override
              public void visitEnd() {
                try {
                  Visitor.this.dependencies.add(new Dependency(this.id, this.version));
                } catch(final InvalidVersionSpecificationException e) {
                  throw new IllegalArgumentException(String.format("The plugin '%s' has defined a dependency on '%s' with an invalid version range of '%s'", Visitor.this.id, this.id, this.version), e);
                }
              }
            };
          }
          return super.visitAnnotation(name, desc);
        }

        @Override
        public AnnotationVisitor visitArray(final String name) {
          if(name.equals("dependencies")) {
            Visitor.this.nested = true;
            return this;
          }
          return super.visitArray(name);
        }

        @Override
        public void visitEnd() {
          if(Visitor.this.nested) {
            Visitor.this.nested = false;
            return;
          }
          PluginLoader.this.candidates.add(new Candidate(Visitor.this.source, Visitor.this.className, Visitor.this.id, Visitor.this.version, Visitor.this.dependencies));
          LOGGER.debug("Queued plugin candidate '{}' for loading", Visitor.this.id);
        }
      };
    }
  }

  private final class Candidate {

    final Source source;
    final String className;
    final String id;
    final String version;
    final List<Dependency> dependencies;

    Candidate(final Source source, final String className, final String id, final String version, final List<Dependency> dependencies) {
      this.source = source;
      this.className = className;
      this.id = id;
      this.version = version;
      this.dependencies = dependencies;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
        .add("className", this.className)
        .add("id", this.id)
        .add("version", this.version)
        .add("dependencies", this.dependencies)
        .toString();
    }
  }

  private final class Dependency {

    @Nonnull final String id;
    @Nullable final VersionRange version;
    Candidate candidate;

    Dependency(@Nonnull final String id, @Nullable final String version) throws InvalidVersionSpecificationException {
      this.id = id;
      this.version = Strings.emptyToNull(version) != null ? VersionRange.createFromVersionSpec(version) : null;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
        .add("id", this.id)
        .add("version", this.version)
        .toString();
    }
  }

  private interface Source {

    // We have nothing to insert, already on the classpath.
    Source CLASSPATH = classLoader -> {};

    @Nonnull
    static Source path(@Nonnull final Path path) {
      // Avoid inserting the same path more than once
      final boolean[] inserted = {false};
      return classLoader -> {
        if(!inserted[0]) {
          inserted[0] = true;
          classLoader.addPath(path);
        }
      };
    }

    void inject(@Nonnull final ClassLoaderInjector classLoader);
  }
}
