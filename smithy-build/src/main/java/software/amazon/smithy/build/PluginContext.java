/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.build;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Context object used in plugin execution.
 */
public final class PluginContext {
    private final ProjectionConfig projection;
    private final String projectionName;
    private final Model model;
    private final Model originalModel;
    private final List<ValidationEvent> events;
    private final ObjectNode settings;
    private final FileManifest fileManifest;
    private final ClassLoader pluginClassLoader;
    private final Set<Path> sources;

    private PluginContext(Builder builder) {
        model = SmithyBuilder.requiredState("model", builder.model);
        fileManifest = SmithyBuilder.requiredState("fileManifest", builder.fileManifest);
        projection = builder.projection;
        projectionName = builder.projectionName;
        originalModel = builder.originalModel;
        events = Collections.unmodifiableList(builder.events);
        settings = builder.settings;
        pluginClassLoader = builder.pluginClassLoader;
        sources = SetUtils.copyOf(builder.sources);
    }

    /**
     * Creates a new PluginContext Builder.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Get the projection the plugin is optionally attached to.
     */
    public Optional<ProjectionConfig> getProjection() {
        return Optional.ofNullable(projection);
    }

    /**
     * Gets the name of the projection being applied.
     *
     * <p>If no projection could be found, "source" is assumed.
     *
     * @return Returns the explicit or assumed projection name.
     */
    public String getProjectionName() {
        return projectionName;
    }

    /**
     * Gets the model that was projected.
     *
     * @return Get the projected model.
     */
    public Model getModel() {
        return model;
    }

    /**
     * Get the original model before applying the projection.
     *
     * @return The optionally provided original model.
     */
    public Optional<Model> getOriginalModel() {
        return Optional.ofNullable(originalModel);
    }

    /**
     * Gets the validation events encountered after projecting the model.
     *
     * @return Get the validation events that were encountered.
     */
    public List<ValidationEvent> getEvents() {
        return events;
    }

    /**
     * Gets the plugin configuration settings.
     *
     * @return Plugins settings object.
     */
    public ObjectNode getSettings() {
        return settings;
    }

    /**
     * Gets the FileManifest used to create files in the projection.
     *
     * <p>All files written by a plugin should either be written using this
     * manifest or added to the manifest via {@link FileManifest#addFile}.
     *
     * @return Returns the file manifest.
     */
    public FileManifest getFileManifest() {
        return fileManifest;
    }

    /**
     * Gets the ClassLoader that should be used in build plugins to load
     * services.
     *
     * @return Returns the optionally set ClassLoader.
     */
    public Optional<ClassLoader> getPluginClassLoader() {
        return Optional.ofNullable(pluginClassLoader);
    }

    /**
     * Gets the source models, or models that are considered the subject
     * of the build.
     *
     * <p>This does not return an exhaustive set of model paths! There are
     * typically two kinds of models that are added to a build: source
     * models and discovered models. Discovered models are someone else's
     * models. Source models are the models owned by the package being built.
     *
     * @return Returns the source models.
     */
    public Set<Path> getSources() {
        return Collections.unmodifiableSet(sources);
    }

    /**
     * Checks if the given shape/ID is either not present in the original
     * model (thus a new, source shape), or is present and the filename of
     * the shape in the original model matches one of the defined
     * {@code sources}.
     *
     * @param shape Shape or Shape ID to check.
     * @return Returns true if this shape is considered a source shape.
     */
    public boolean isSourceShape(ToShapeId shape) {
        return originalModel == null
               || isSource(originalModel.getShapeIndex().getShape(shape.toShapeId()).orElse(null));
    }

    /**
     * Checks if the given metadata key-value pair is either not present
     * in the old model (thus a new, source metadata), or is present and
     * the filename of the entry in the original model matches one of
     * the defined {@code sources}.
     *
     * @param metadataKey Metadata key to check.
     * @return Returns true if this metadata is considered a source entry.
     */
    public boolean isSourceMetadata(String metadataKey) {
        return originalModel == null || isSource(originalModel.getMetadataProperty(metadataKey).orElse(null));
    }

    private boolean isSource(FromSourceLocation sourceLocation) {
        if (sourceLocation == null) {
            return true;
        }

        String location = sourceLocation.getSourceLocation().getFilename();
        int offsetFromStart = findOffsetFromStart(location);

        for (Path path : sources) {
            String pathString = path.toString();
            int offsetFromStartInSource = findOffsetFromStart(pathString);
            // Compare the strings in a way that normalizes them and strips off protocols.
            if (location.regionMatches(offsetFromStart, pathString, offsetFromStartInSource, pathString.length())) {
                return true;
            }
        }

        return false;
    }

    private int findOffsetFromStart(String location) {
        // This accounts for "jar:file:" and "file:".
        int position = location.indexOf("file:");
        return position == -1 ? 0 : position + "file:".length();
    }

    /**
     * Builds a {@link PluginContext}.
     */
    public static final class Builder implements SmithyBuilder<PluginContext> {
        private ProjectionConfig projection;
        private String projectionName = "source";
        private Model model;
        private Model originalModel;
        private List<ValidationEvent> events = Collections.emptyList();
        private ObjectNode settings = Node.objectNode();
        private FileManifest fileManifest;
        private ClassLoader pluginClassLoader;
        private Set<Path> sources = Collections.emptySet();

        private Builder() {}

        @Override
        public PluginContext build() {
            return new PluginContext(this);
        }

        /**
         * Sets the <strong>required</strong> {@link FileManifest} to use
         * in the plugin.
         *
         * @param fileManifest FileManifest to use.
         * @return Returns the builder.
         */
        public Builder fileManifest(FileManifest fileManifest) {
            this.fileManifest = fileManifest;
            return this;
        }

        /**
         * Sets the <strong>required</strong> model that is being built.
         *
         * @param model Model to set.
         * @return Returns the builder.
         */
        public Builder model(Model model) {
            this.model = Objects.requireNonNull(model);
            return this;
        }

        /**
         * Sets the projection that the plugin belongs to.
         *
         * @param name Name of the projection.
         * @param projection ProjectionConfig to set.
         * @return Returns the builder.
         */
        public Builder projection(String name, ProjectionConfig projection) {
            this.projectionName = Objects.requireNonNull(name);
            this.projection = Objects.requireNonNull(projection);
            return this;
        }

        /**
         * Sets the model that is being built before it was transformed in
         * the projection.
         *
         * @param originalModel Original Model to set.
         * @return Returns the builder.
         */
        public Builder originalModel(Model originalModel) {
            this.originalModel = Objects.requireNonNull(originalModel);
            return this;
        }

        /**
         * Sets the validation events that occurred after projecting the model.
         *
         * @param events Validation events to set.
         * @return Returns the builder.
         */
        public Builder events(List<ValidationEvent> events) {
            this.events = Objects.requireNonNull(events);
            return this;
        }

        /**
         * Sets the settings of the plugin.
         *
         * @param settings Settings to set.
         * @return Returns the builder.
         */
        public Builder settings(ObjectNode settings) {
            this.settings = Objects.requireNonNull(settings);
            return this;
        }

        /**
         * Sets a ClassLoader that should be used by build plugins when loading
         * services.
         *
         * @param pluginClassLoader ClassLoader to use in build plugins.
         * @return Retruns the builder.
         */
        public Builder pluginClassLoader(ClassLoader pluginClassLoader) {
            this.pluginClassLoader = pluginClassLoader;
            return this;
        }

        /**
         * Sets the path to models that are considered "source" models of the
         * package being built.
         *
         * @param sources Source models to set.
         * @return Returns the builder.
         */
        public Builder sources(Collection<Path> sources) {
            this.sources = new HashSet<>(sources);
            return this;
        }
    }
}
