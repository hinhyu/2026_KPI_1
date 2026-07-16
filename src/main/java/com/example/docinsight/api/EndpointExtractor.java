package com.example.docinsight.api;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class EndpointExtractor {

    static {
        StaticJavaParser.setConfiguration(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    }
    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "PatchMapping", "DeleteMapping", "RequestMapping"
    );

    public List<ApiEndpointInfo> extract(Path root, Path javaFile) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            List<ApiEndpointInfo> endpoints = new ArrayList<>();

            for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (!isController(clazz)) {
                    continue;
                }

                List<String> basePaths = extractClassBasePaths(clazz);
                if (basePaths.isEmpty()) {
                    basePaths = List.of("/");
                }

                for (MethodDeclaration method : clazz.getMethods()) {
                    Mapping mapping = extractMethodMapping(method);
                    if (mapping == null) {
                        continue;
                    }

                    String requestDto = method.getParameters().stream()
                            .filter(p -> hasAnnotation(p, "RequestBody"))
                            .map(p -> p.getType().asString())
                            .findFirst()
                            .orElse(null);

                    List<String> pathVariables = method.getParameters().stream()
                            .filter(p -> hasAnnotation(p, "PathVariable"))
                            .map(this::resolveParamName)
                            .toList();

                    List<String> queryParams = method.getParameters().stream()
                            .filter(p -> hasAnnotation(p, "RequestParam"))
                            .map(this::resolveParamName)
                            .toList();

                    for (String basePath : basePaths) {
                        for (String methodPath : mapping.paths()) {
                            for (String httpMethod : mapping.httpMethods()) {
                                endpoints.add(new ApiEndpointInfo(
                                        clazz.getNameAsString(),
                                        httpMethod,
                                        normalizePath(basePath, methodPath),
                                        method.getNameAsString(),
                                        requestDto,
                                        method.getType().asString(),
                                        pathVariables,
                                        queryParams,
                                        root.relativize(javaFile).toString()
                                ));
                            }
                        }
                    }
                }
            }
            return endpoints;
        } catch (Exception e) {
            throw new IllegalStateException("Endpoint extraction failed: " + javaFile + " / " + e.getMessage(), e);
        }
    }

    private boolean isController(ClassOrInterfaceDeclaration clazz) {
        return clazz.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .anyMatch(name -> name.equals("RestController") || name.equals("Controller"));
    }

    private List<String> extractClassBasePaths(ClassOrInterfaceDeclaration clazz) {
        return clazz.getAnnotations().stream()
                .filter(a -> a.getNameAsString().equals("RequestMapping"))
                .flatMap(a -> extractPaths(a).stream())
                .toList();
    }

    private Mapping extractMethodMapping(MethodDeclaration method) {
        for (AnnotationExpr annotation : method.getAnnotations()) {
            String name = annotation.getNameAsString();
            if (!MAPPING_ANNOTATIONS.contains(name)) {
                continue;
            }

            List<String> paths = extractPaths(annotation);
            if (paths.isEmpty()) {
                paths = List.of("/");
            }

            return switch (name) {
                case "GetMapping" -> new Mapping(List.of("GET"), paths);
                case "PostMapping" -> new Mapping(List.of("POST"), paths);
                case "PutMapping" -> new Mapping(List.of("PUT"), paths);
                case "PatchMapping" -> new Mapping(List.of("PATCH"), paths);
                case "DeleteMapping" -> new Mapping(List.of("DELETE"), paths);
                case "RequestMapping" -> new Mapping(extractHttpMethods(annotation), paths);
                default -> null;
            };
        }
        return null;
    }

    private List<String> extractHttpMethods(AnnotationExpr annotation) {
        Optional<Expression> methodExpr = findMemberValue(annotation, "method");
        if (methodExpr.isEmpty()) {
            return List.of("REQUEST");
        }

        List<String> methods = new ArrayList<>();
        collectHttpMethods(methodExpr.get(), methods);
        if (methods.isEmpty()) {
            return List.of("REQUEST");
        }
        return methods;
    }

    private void collectHttpMethods(Expression expression, List<String> methods) {
        if (expression instanceof ArrayInitializerExpr array) {
            for (Expression item : array.getValues()) {
                collectHttpMethods(item, methods);
            }
            return;
        }

        if (expression instanceof FieldAccessExpr fieldAccess) {
            methods.add(fieldAccess.getNameAsString().toUpperCase(Locale.ROOT));
            return;
        }

        if (expression instanceof NameExpr nameExpr) {
            methods.add(nameExpr.getNameAsString().toUpperCase(Locale.ROOT));
        }
    }

    private List<String> extractPaths(AnnotationExpr annotation) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();

        if (annotation instanceof SingleMemberAnnotationExpr single) {
            collectStringValues(single.getMemberValue(), paths);
            return new ArrayList<>(paths);
        }

        if (annotation instanceof NormalAnnotationExpr normal) {
            for (String key : List.of("value", "path")) {
                findMemberValue(normal, key).ifPresent(expr -> collectStringValues(expr, paths));
            }
        }

        return new ArrayList<>(paths);
    }

    private Optional<Expression> findMemberValue(AnnotationExpr annotation, String memberName) {
        if (!(annotation instanceof NormalAnnotationExpr normal)) {
            return Optional.empty();
        }
        return normal.getPairs().stream()
                .filter(pair -> pair.getNameAsString().equals(memberName))
                .map(MemberValuePair::getValue)
                .findFirst();
    }

    private void collectStringValues(Expression expression, Set<String> values) {
        if (expression instanceof StringLiteralExpr literal) {
            values.add(literal.getValue());
            return;
        }
        if (expression instanceof ArrayInitializerExpr array) {
            for (Expression item : array.getValues()) {
                collectStringValues(item, values);
            }
        }
    }

    private boolean hasAnnotation(Parameter parameter, String annotationName) {
        return parameter.getAnnotations().stream()
                .anyMatch(a -> a.getNameAsString().equals(annotationName));
    }

    private String resolveParamName(Parameter parameter) {
        for (AnnotationExpr annotation : parameter.getAnnotations()) {
            String name = annotation.getNameAsString();
            if (!name.equals("PathVariable") && !name.equals("RequestParam")) {
                continue;
            }

            if (annotation instanceof SingleMemberAnnotationExpr single
                    && single.getMemberValue() instanceof StringLiteralExpr literal) {
                return literal.getValue();
            }

            Optional<Expression> named = findMemberValue(annotation, "name")
                    .or(() -> findMemberValue(annotation, "value"));
            if (named.isPresent() && named.get() instanceof StringLiteralExpr literal) {
                return literal.getValue();
            }
        }
        return parameter.getNameAsString();
    }

    private String normalizePath(String basePath, String subPath) {
        String base = basePath == null || basePath.isBlank() || basePath.equals("/") ? "" : basePath;
        String sub = subPath == null || subPath.isBlank() || subPath.equals("/") ? "" : subPath;
        String combined = ("/" + base + "/" + sub).replaceAll("/{2,}", "/");
        if (combined.length() > 1 && combined.endsWith("/")) {
            return combined.substring(0, combined.length() - 1);
        }
        return combined;
    }

    private record Mapping(List<String> httpMethods, List<String> paths) {}
}
