package com.example.docinsight.api;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class EndpointExtractor {

    public List<ApiEndpointInfo> extract(Path root, Path javaFile) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            ClassOrInterfaceDeclaration clazz = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();

            boolean isController = clazz.getAnnotations().stream()
                    .map(a -> a.getNameAsString())
                    .anyMatch(name -> name.equals("RestController") || name.equals("Controller"));

            if (!isController) return List.of();

            String basePath = clazz.getAnnotations().stream()
                    .filter(a -> a.getNameAsString().equals("RequestMapping"))
                    .map(this::extractFirstStringValue)
                    .findFirst()
                    .orElse("/");

            List<ApiEndpointInfo> endpoints = new ArrayList<>();
            for (MethodDeclaration method : clazz.getMethods()) {
                Mapping mapping = extractMethodMapping(method);
                if (mapping == null) continue;

                String requestDto = method.getParameters().stream()
                        .filter(p -> p.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals("RequestBody")))
                        .map(p -> p.getType().asString())
                        .findFirst()
                        .orElse(null);

                endpoints.add(new ApiEndpointInfo(
                        clazz.getNameAsString(),
                        mapping.httpMethod(),
                        normalizePath(basePath, mapping.path()),
                        method.getNameAsString(),
                        requestDto,
                        method.getType().asString(),
                        root.relativize(javaFile).toString()
                ));
            }
            return endpoints;
        } catch (Exception e) {
            throw new IllegalStateException("Endpoint extraction failed: " + javaFile + " / " + e.getMessage(), e);
        }
    }

    private Mapping extractMethodMapping(MethodDeclaration method) {
        for (AnnotationExpr annotation : method.getAnnotations()) {
            String name = annotation.getNameAsString();
            String path = extractFirstStringValue(annotation);
            return switch (name) {
                case "GetMapping" -> new Mapping("GET", path);
                case "PostMapping" -> new Mapping("POST", path);
                case "PutMapping" -> new Mapping("PUT", path);
                case "PatchMapping" -> new Mapping("PATCH", path);
                case "DeleteMapping" -> new Mapping("DELETE", path);
                case "RequestMapping" -> new Mapping("REQUEST", path);
                default -> null;
            };
        }
        return null;
    }

    private String extractFirstStringValue(AnnotationExpr annotation) {
        String text = annotation.toString();
        int first = text.indexOf('"');
        int second = text.indexOf('"', first + 1);
        if (first >= 0 && second > first) return text.substring(first + 1, second);
        return "/";
    }

    private String normalizePath(String basePath, String subPath) {
        String base = basePath == null || basePath.isBlank() || basePath.equals("/") ? "" : basePath;
        String sub = subPath == null || subPath.isBlank() || subPath.equals("/") ? "" : subPath;
        return ("/" + base + "/" + sub).replaceAll("/{2,}", "/");
    }

    private record Mapping(String httpMethod, String path) {}
}
