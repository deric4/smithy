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

package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;

public class IntegTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(IntegTest.class.getResource("integration-test-model.json"))
                .assemble()
                .unwrap();
    }

    @Test
    public void removesResources() {
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapesIf(model, shape -> shape.getId().toString().equals("ns.foo#MyResource"));
        ShapeIndex index = result.getShapeIndex();

        assertValidModel(result);
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResource")), Matchers.is(Optional.empty()));
        // The operations bound to the resource remain, now orphaned.
        assertThat(index.getShape(ShapeId.from("ns.foo#CreateMyResource")), Matchers.not(Optional.empty()));
    }

    @Test
    public void removesServices() {
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapesIf(model, shape -> shape.getId().toString().equals("ns.foo#MyService"));
        ShapeIndex index = result.getShapeIndex();

        assertValidModel(result);
        // Operations and resources bound to the service remain.
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResource")), Matchers.not(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResourceIdentifier")), Matchers.not(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#CreateMyResource")), Matchers.not(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#MyOperation")), Matchers.not(Optional.empty()));
    }

    @Test
    public void removesUnreferencedShapes() {
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapesIf(model, shape -> shape.getId().toString().equals("ns.foo#MyResource"));
        result = transformer.removeUnreferencedShapes(result);
        ShapeIndex index = result.getShapeIndex();

        assertValidModel(result);
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResource")), Matchers.is(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResourceIdentifier")), Matchers.not(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#CreateMyResource")), Matchers.is(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#CreateMyResourceOutput")), Matchers.is(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResourceOperationInput")), Matchers.is(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResourceOperationInputString")), Matchers.is(Optional.empty()));
    }

    @Test
    public void removesUnreferencedShapesWithFilter() {
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapesIf(model, shape -> shape.getId().toString().equals("ns.foo#MyResource"));
        result = transformer.removeUnreferencedShapes(result, shape -> !shape.getTags().contains("foo"));
        ShapeIndex index = result.getShapeIndex();

        assertValidModel(result);
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResource")), Matchers.is(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResourceIdentifier")), Matchers.not(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#CreateMyResource")), Matchers.is(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#CreateMyResourceOutput")), Matchers.is(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResourceOperationInput")), Matchers.is(Optional.empty()));
        assertThat(index.getShape(ShapeId.from("ns.foo#MyResourceOperationInputString")), Matchers.not(Optional.empty()));
    }

    private void assertValidModel(Model model) {
        assertFalse(Model.assembler().addModel(model).assemble().isBroken());
    }
}
