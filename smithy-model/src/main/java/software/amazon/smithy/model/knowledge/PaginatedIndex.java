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

package software.amazon.smithy.model.knowledge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.validators.PaginatedTraitValidator;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Index of operation shapes to paginated trait information.
 *
 * <p>This index makes it easy to slice up paginated operations and
 * get the resolved members. This index performs some basic validation
 * of the paginated trait like ensuring that the operation has input
 * and output, and that the members defined in the paginated trait can
 * be found in the input or output of the operation. Additional
 * validation is performed in the {@link PaginatedTraitValidator}
 * (which makes use of this index).
 */
public final class PaginatedIndex implements KnowledgeIndex {
    private final Map<ShapeId, Map<ShapeId, PaginationInfo>> paginationInfo = new HashMap<>();

    public PaginatedIndex(Model model) {
        ShapeIndex index = model.getShapeIndex();
        TopDownIndex topDownIndex = model.getKnowledge(TopDownIndex.class);
        OperationIndex opIndex = model.getKnowledge(OperationIndex.class);

        index.shapes(ServiceShape.class).forEach(service -> {
            PaginatedTrait serviceTrait = service.getTrait(PaginatedTrait.class).orElse(null);
            Map<ShapeId, PaginationInfo> mappings = topDownIndex.getContainedOperations(service).stream()
                    .flatMap(operation -> Trait.flatMapStream(operation, PaginatedTrait.class))
                    .flatMap(p -> OptionalUtils.stream(create(service, opIndex, p.left, p.right.merge(serviceTrait))))
                    .collect(Collectors.toMap(i -> i.getOperation().getId(), Function.identity()));
            paginationInfo.put(service.getId(), Collections.unmodifiableMap(mappings));
        });
    }

    private Optional<PaginationInfo> create(
            ServiceShape service,
            OperationIndex opIndex,
            OperationShape operation,
            PaginatedTrait trait
    ) {
        StructureShape input = opIndex.getInput(operation.getId()).orElse(null);
        StructureShape output = opIndex.getOutput(operation.getId()).orElse(null);

        if (input == null || output == null) {
            return Optional.empty();
        }

        MemberShape inputToken = trait.getInputToken().flatMap(input::getMember).orElse(null);
        MemberShape outputToken = trait.getOutputToken().flatMap(output::getMember).orElse(null);

        if (inputToken == null || outputToken == null) {
            return Optional.empty();
        }

        MemberShape pageSizeMember = trait.getPageSize().flatMap(input::getMember).orElse(null);
        MemberShape itemsMember = trait.getItems().flatMap(output::getMember).orElse(null);

        return Optional.of(new PaginationInfo(
                service, operation, input, output, trait,
                inputToken, outputToken, pageSizeMember, itemsMember));
    }

    public Optional<PaginationInfo> getPaginationInfo(ToShapeId service, ToShapeId operation) {
        return Optional.ofNullable(paginationInfo.get(service.toShapeId()))
                .flatMap(mappings -> Optional.ofNullable(mappings.get(operation.toShapeId())));
    }
}
