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

package software.amazon.smithy.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.Pair;

/**
 * Queryable container for detected structural differences between two models.
 */
public final class Differences {
    private final Model oldModel;
    private final Model newModel;
    private final List<ChangedShape<Shape>> changedShapes = new ArrayList<>();
    private final List<ChangedMetadata> changedMetadata = new ArrayList<>();

    private Differences(Model oldModel, Model newModel) {
        this.oldModel = oldModel;
        this.newModel = newModel;
        detectMetadataChanges(oldModel, newModel, this);
        detectShapeChanges(oldModel, newModel, this);
    }

    static Differences detect(Model oldModel, Model newModel) {
        return new Differences(oldModel, newModel);
    }

    /**
     * Gets the old model.
     *
     * @return Returns the old model.
     */
    public Model getOldModel() {
        return oldModel;
    }

    /**
     * Gets the new model.
     *
     * @return Returns the new model.
     */
    public Model getNewModel() {
        return newModel;
    }

    /**
     * Gets all added shapes.
     *
     * @return Returns a stream of each added shape.
     */
    public Stream<Shape> addedShapes() {
        return newModel.getShapeIndex().shapes()
                .filter(shape -> !oldModel.getShapeIndex().getShape(shape.getId()).isPresent());
    }

    /**
     * Gets all of the added shapes of a specific type.
     *
     * @param shapeType Type of shape to find.
     * @param <T> Type of shape.
     * @return Returns a stream of each added shape of a specific type.
     */
    public <T extends Shape> Stream<T> addedShapes(Class<T> shapeType) {
        return addedShapes().filter(shapeType::isInstance).map(shapeType::cast);
    }

    /**
     * Gets all added metadata.
     *
     * <p>Each Pair returned contains the name of the metadata key on
     * the left of the Pair and the metadata value on the right.
     *
     * @return Returns a stream of added metadata.
     */
    public Stream<Pair<String, Node>> addedMetadata() {
        return newModel.getMetadata().entrySet().stream()
                .filter(entry -> !oldModel.getMetadata().containsKey(entry.getKey()))
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()));
    }

    /**
     * Gets all removed shapes.
     *
     * @return Returns a stream of each removed shape.
     */
    public Stream<Shape> removedShapes() {
        return oldModel.getShapeIndex().shapes()
                .filter(shape -> !newModel.getShapeIndex().getShape(shape.getId()).isPresent());
    }

    /**
     * Gets all of the removed shapes of a specific type.
     *
     * @param shapeType Type of shape to find.
     * @param <T> Type of shape.
     * @return Returns a stream of each removed shape of a specific type.
     */
    public <T extends Shape> Stream<T> removedShapes(Class<T> shapeType) {
        return removedShapes().filter(shapeType::isInstance).map(shapeType::cast);
    }

    /**
     * Gets all removed metadata.
     *
     * <p>Each Pair returned contains the name of the metadata key on
     * the left of the Pair and the metadata value on the right.
     *
     * @return Returns a stream of removed metadata.
     */
    public Stream<Pair<String, Node>> removedMetadata() {
        return oldModel.getMetadata().entrySet().stream()
                .filter(entry -> !newModel.getMetadata().containsKey(entry.getKey()))
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()));
    }

    /**
     * Gets all changed shapes.
     *
     * @return Returns a stream of changed shapes.
     */
    public Stream<ChangedShape<Shape>> changedShapes() {
        return changedShapes.stream();
    }

    /**
     * Gets all changed shapes of a specific type.
     *
     * @param type Type of shape to find.
     * @param <T> Type of shape.
     * @return Returns a stream of matching changed shapes.
     */
    @SuppressWarnings("unchecked")
    public <T extends Shape> Stream<ChangedShape<T>> changedShapes(Class<T> type) {
        return changedShapes()
                .filter(change -> type.isInstance(change.getOldShape()) && type.isInstance(change.getNewShape()))
                .map(change -> (ChangedShape<T>) change);
    }

    /**
     * Gets a stream of all changed metadata.
     *
     * @return Returns the changed metadata.
     */
    public Stream<ChangedMetadata> changedMetadata() {
        return changedMetadata.stream();
    }

    private static void detectShapeChanges(Model oldModel, Model newModel, Differences differences) {
        for (Shape oldShape : oldModel.getShapeIndex().toSet()) {
            newModel.getShapeIndex().getShape(oldShape.getId()).ifPresent(newShape -> {
                if (!oldShape.equals(newShape)) {
                    differences.changedShapes.add(new ChangedShape<>(oldShape, newShape));
                }
            });
        }
    }

    private static void detectMetadataChanges(Model oldModel, Model newModel, Differences differences) {
        oldModel.getMetadata().forEach((k, v) -> {
            if (newModel.getMetadata().containsKey(k) && !newModel.getMetadata().get(k).equals(v)) {
                differences.changedMetadata.add(new ChangedMetadata(k, v, newModel.getMetadata().get(k)));
            }
        });
    }
}
