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

package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.transform.ModelTransformer;

public class IncludeNamespacesTest {

    @Test
    public void removesShapesNotInNamespaces() {
        StringShape string1 = StringShape.builder().id("ns.foo#yuck").build();
        StringShape string2 = StringShape.builder().id("ns.foo#qux").build();
        StringShape string3 = StringShape.builder().id("ns.bar#yuck").build();
        StringShape string4 = StringShape.builder().id("ns.qux#yuck").build();
        ShapeIndex index = ShapeIndex.builder().addShapes(string1, string2, string3, string4).build();
        Model model = Model.builder().shapeIndex(index).build();
        Model result = new IncludeNamespaces()
                .createTransformer(Arrays.asList("ns.foo", "ns.bar"))
                .apply(ModelTransformer.create(), model);

        assertThat(result.getShapeIndex().getShape(string1.getId()), not(Optional.empty()));
        assertThat(result.getShapeIndex().getShape(string2.getId()), not(Optional.empty()));
        assertThat(result.getShapeIndex().getShape(string3.getId()), not(Optional.empty()));
        assertThat(result.getShapeIndex().getShape(string4.getId()), is(Optional.empty()));
    }
}
