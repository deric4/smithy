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

package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.validators.ReferencesTraitValidator;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines references to resources within a structure.
 *
 * @see ReferencesTraitValidator
 */
public final class ReferencesTrait extends AbstractTrait implements ToSmithyBuilder<ReferencesTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#references");

    private final List<Reference> references;

    private ReferencesTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.references = ListUtils.copyOf(builder.references);
    }

    /**
     * Gets the references.
     *
     * @return Returns the unmodifiable list of references.
     */
    public List<Reference> getReferences() {
        return references;
    }

    /**
     * Gets a list of all references to a particular shape.
     *
     * @param shapeId Shape ID to search for.
     *
     * @return Returns the list of found references.
     */
    public List<Reference> getResourceReferences(ShapeId shapeId) {
        return getReferences().stream()
                .filter(reference -> reference.getResource().equals(shapeId))
                .collect(Collectors.toList());
    }

    @Override
    protected Node createNode() {
        return references.stream().map(Reference::toNode).collect(ArrayNode.collect());
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder().sourceLocation(getSourceLocation());
        references.forEach(builder::addReference);
        return builder;
    }

    /**
     * @return Returns a builder used to create a references trait.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder use to create the references trait.
     */
    public static final class Builder extends AbstractTraitBuilder<ReferencesTrait, Builder> {
        private List<Reference> references = new ArrayList<>();

        public Builder addReference(Reference reference) {
            references.add(Objects.requireNonNull(reference));
            return this;
        }

        public Builder clearReferences() {
            references.clear();
            return this;
        }

        public Builder references(List<Reference> references) {
            this.references.clear();
            this.references.addAll(references);
            return this;
        }

        @Override
        public ReferencesTrait build() {
            return new ReferencesTrait(this);
        }
    }

    /**
     * Reference to a resource.
     */
    public static final class Reference implements ToSmithyBuilder<Reference>, ToNode {
        private ShapeId resource;
        private Map<String, String> ids;
        private ShapeId service;
        private String rel;

        private Reference(Builder builder) {
            resource = SmithyBuilder.requiredState("resource", builder.resource);
            ids = Collections.unmodifiableMap(new TreeMap<>(builder.ids));
            rel = builder.rel;
            service = builder.service;
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public Builder toBuilder() {
            return builder().resource(resource).ids(ids).service(service).rel(rel);
        }

        /**
         * Get the referenced shape.
         *
         * @return Returns the referenced shape.
         */
        public ShapeId getResource() {
            return resource;
        }

        /**
         * Get the service binding.
         *
         * @return Returns the optionally referenced service.
         */
        public Optional<ShapeId> getService() {
            return Optional.ofNullable(service);
        }

        /**
         * @return Returns the immutable mapping of member names to resource identifier name.
         */
        public Map<String, String> getIds() {
            return ids;
        }

        /**
         * @return Gets the optional rel property.
         */
        public Optional<String> getRel() {
            return Optional.ofNullable(rel);
        }

        @Override
        public String toString() {
            return "Reference" + Node.printJson(toNode());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof Reference)) {
                return false;
            }

            Reference reference = (Reference) o;
            return resource.equals(reference.resource)
                   && Objects.equals(ids, reference.ids)
                   && Objects.equals(service, reference.service)
                   && Objects.equals(rel, reference.rel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resource, ids, service, rel);
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember("resource", Node.from(resource.toString()))
                    .withOptionalMember("ids", ids.isEmpty()
                            ? Optional.empty()
                            : Optional.of(ObjectNode.fromStringMap(getIds())))
                    .withOptionalMember("service", getService().map(ShapeId::toString).map(Node::from))
                    .withOptionalMember("rel", getRel().map(Node::from))
                    .build();
        }

        /**
         * Builder to create a {@link Reference}.
         */
        public static final class Builder implements SmithyBuilder<Reference> {
            private ShapeId resource;
            private String rel;
            private Map<String, String> ids = MapUtils.of();
            private ShapeId service;

            private Builder() {}

            @Override
            public Reference build() {
                return new Reference(this);
            }

            public Builder ids(Map<String, String> members) {
                this.ids = Objects.requireNonNull(members);
                return this;
            }

            public Builder resource(ShapeId resource) {
                this.resource = Objects.requireNonNull(resource);
                return this;
            }

            public Builder service(ShapeId service) {
                this.service = service;
                return this;
            }

            public Builder rel(String rel) {
                this.rel = rel;
                return this;
            }
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public ReferencesTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            ArrayNode refs = value.expectArrayNode();
            for (ObjectNode member : refs.getElementsAs(ObjectNode.class)) {
                builder.addReference(referenceFromNode(target.getNamespace(), member));
            }
            return builder.build();
        }

        private static Reference referenceFromNode(String namespace, ObjectNode referenceProperties) {
            return Reference.builder()
                    .resource(referenceProperties
                                      .expectMember("resource")
                                      .expectStringNode()
                                      .expectShapeId(namespace))
                    .ids(referenceProperties.getObjectMember("ids")
                                 .map(obj -> obj.getMembers().entrySet().stream()
                                         .map(entry -> Pair.of(
                                                 entry.getKey().getValue(),
                                                 entry.getValue().expectStringNode().getValue()))
                                         .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)))
                                 .orElseGet(Collections::emptyMap))
                    .service(referenceProperties
                                     .getStringMember("service")
                                     .map(string -> string.expectShapeId(namespace))
                                     .orElse(null))
                    .rel(referenceProperties.getStringMember("rel")
                                 .map(StringNode::getValue)
                                 .orElse(null))
                    .build();
        }
    }
}
