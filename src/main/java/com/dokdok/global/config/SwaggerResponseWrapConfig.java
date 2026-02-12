package com.dokdok.global.config;

import com.dokdok.global.response.ApiResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class SwaggerResponseWrapConfig {

    @Bean
    public OpenApiCustomizer apiResponseSchemaRegistrar() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }

            Map<String, Schema> schemas = components.getSchemas();
            if (schemas == null) {
                schemas = new LinkedHashMap<>();
                components.setSchemas(schemas);
            }

            ResolvedSchema resolved = ModelConverters.getInstance()
                    .resolveAsResolvedSchema(new AnnotatedType(ApiResponse.class).resolveAsRef(true));

            if (resolved.referencedSchemas != null) {
                resolved.referencedSchemas.forEach(schemas::putIfAbsent);
            }

            Schema<?> apiResponseSchema = resolved.schema;
            if (apiResponseSchema != null) {
                if (apiResponseSchema.getName() == null) {
                    apiResponseSchema.setName("ApiResponse");
                }
                schemas.putIfAbsent("ApiResponse", apiResponseSchema);
            }
        };
    }

    @Bean
    public OpenApiCustomizer wrapApiResponseSchema() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem -> {
                if (pathItem.readOperations() == null) {
                    return;
                }

                pathItem.readOperations().forEach(operation -> {
                    if (operation.getResponses() == null) {
                        return;
                    }

                    operation.getResponses().values().forEach(this::wrapApiResponse);
                });
            });
        };
    }

    private void wrapApiResponse(io.swagger.v3.oas.models.responses.ApiResponse apiResponse) {
        Content content = apiResponse.getContent();
        if (content == null) {
            return;
        }

        content.values().forEach(this::wrapMediaTypeSchema);
    }

    private void wrapMediaTypeSchema(MediaType mediaType) {
        Schema<?> original = mediaType.getSchema();
        if (original == null || isApiResponseSchema(original)) {
            return;
        }

        ComposedSchema composed = new ComposedSchema();
        composed.addAllOfItem(new Schema<>().$ref("#/components/schemas/ApiResponse"));

        Schema<?> override = new Schema<>();
        override.addProperties("data", original);

        composed.addAllOfItem(override);
        mediaType.setSchema(composed);
    }

    private boolean isApiResponseSchema(Schema<?> schema) {
        if (schema.get$ref() != null && schema.get$ref().endsWith("/ApiResponse")) {
            return true;
        }

        if (schema instanceof ComposedSchema composed && composed.getAllOf() != null) {
            return composed.getAllOf().stream()
                    .anyMatch(s -> s.get$ref() != null && s.get$ref().endsWith("/ApiResponse"));
        }

        return false;
    }
}
