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

package software.amazon.smithy.openapi.fromsmithy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.PathItem;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class OpenApiConverterTest {
    @Test
    public void convertsModelsToOpenApi() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-service.json"))
                .assemble()
                .unwrap();
        ObjectNode result = OpenApiConverter.create().convertToNode(model, ShapeId.from("example.rest#RestService"));
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("test-service.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void convertsModelsToOpenApiAndDisablesInlining() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-service.json"))
                .assemble()
                .unwrap();
        ObjectNode result = OpenApiConverter.create()
                .putSetting(OpenApiConstants.DISABLE_PRIMITIVE_INLINING, true)
                .convertToNode(model, ShapeId.from("example.rest#RestService"));
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("test-service.openapi.not-inlined.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void passesThroughTags() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("tagged-service.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create()
                .putSetting(OpenApiConstants.OPEN_API_TAGS, true)
                .putSetting(OpenApiConstants.OPEN_API_SUPPORTED_TAGS, Node.fromStrings("baz", "foo"))
                .convert(model, ShapeId.from("smithy.example#Service"));
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("tagged-service.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void requiresProtocolsTrait() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler()
                    .addImport(getClass().getResource("missing-protocols-trait.json"))
                    .assemble()
                    .unwrap();
            OpenApiConverter.create()
                    .convert(model, ShapeId.from("smithy.example#Service"));
        });

        assertThat(thrown.getMessage(), containsString("`protocols` trait"));
    }

    @Test
    public void mustBeAbleToResolveProtocol() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler()
                    .addImport(getClass().getResource("unable-to-resolve-protocol.json"))
                    .assemble()
                    .unwrap();
            OpenApiConverter.create()
                    .convert(model, ShapeId.from("smithy.example#Service"));
        });

        assertThat(thrown.getMessage(), containsString("Unable to resolve"));
    }

    @Test
    public void mustBeAbleToResolveExplicitProtocol() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler()
                    .addImport(getClass().getResource("unable-to-resolve-protocol.json"))
                    .assemble()
                    .unwrap();
            OpenApiConverter.create()
                    .protocolName("not-found")
                    .convert(model, ShapeId.from("smithy.example#Service"));
        });

        assertThat(thrown.getMessage(), containsString("Unable to resolve"));
    }

    @Test
    public void omitsUnsupportedHttpMethods() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("unsupported-http-method.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("unsupported-http-method.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void protocolsCanOmitOperations() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("missing-http-bindings.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create()
                .convert(model, ShapeId.from("smithy.example#Service"));

        for (PathItem pathItem : result.getPaths().values()) {
            Assertions.assertFalse(pathItem.getGet().isPresent());
            Assertions.assertFalse(pathItem.getHead().isPresent());
            Assertions.assertFalse(pathItem.getDelete().isPresent());
            Assertions.assertFalse(pathItem.getPatch().isPresent());
            Assertions.assertFalse(pathItem.getPost().isPresent());
            Assertions.assertFalse(pathItem.getPut().isPresent());
            Assertions.assertFalse(pathItem.getTrace().isPresent());
            Assertions.assertFalse(pathItem.getOptions().isPresent());
        }
    }

    @Test
    public void addsEmptyResponseByDefault() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("adds-empty-response.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));

        assertThat(result.getPaths().get("/").getGet().get().getResponses().values(), not(empty()));
    }

    @Test
    public void addsMixedSecurityService() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("mixed-security-service.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("mixed-security-service.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    private static final class NullSecurity implements OpenApiMapper {
        @Override
        public Map<String, List<String>> updateSecurity(
                Context context, Shape shape,
                SecuritySchemeConverter converter,
                Map<String, List<String>> requirement
        ) {
            return null;
        }
    }

    @Test
    public void canOmitSecurityRequirements() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("mixed-security-service.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create()
                .addOpenApiMapper(new NullSecurity())
                .convert(model, ShapeId.from("smithy.example#Service"));

        assertThat(result.getSecurity(), empty());
        assertThat(result.getPaths().get("/2").getGet().get().getSecurity(), empty());
    }

    private static final class ConstantSecurity implements OpenApiMapper {
        @Override
        public Map<String, List<String>> updateSecurity(
                Context context, Shape shape,
                SecuritySchemeConverter converter,
                Map<String, List<String>> requirement
        ) {
            return MapUtils.of("foo_baz", ListUtils.of());
        }
    }

    @Test
    public void canChangeSecurityRequirementName() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("mixed-security-service.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create()
                .addOpenApiMapper(new ConstantSecurity())
                .convert(model, ShapeId.from("smithy.example#Service"));

        assertThat(result.getSecurity().get(0).keySet(), contains("foo_baz"));
        assertThat(result.getPaths().get("/2").getGet().get().getSecurity().get(0).keySet(), contains("foo_baz"));
    }
}
