package com.example.docinsight.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class JavaStructureAnalyzer {

    public JavaClassInfo analyze(Path root, Path javaFile) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);

            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getName().asString())
                    .orElse("");

            ClassOrInterfaceDeclaration clazz = cu.findFirst(ClassOrInterfaceDeclaration.class)
                    .orElseThrow(() -> new IllegalArgumentException("class not found: " + javaFile));

            List<String> annotations = clazz.getAnnotations().stream()
                    .map(a -> a.getNameAsString())
                    .toList();

            List<JavaMethodInfo> methods = clazz.getMethods().stream()
                    .map(method -> new JavaMethodInfo(
                            method.getNameAsString(),
                            method.getType().asString(),
                            method.getAnnotations().stream().map(a -> a.getNameAsString()).toList(),
                            method.getParameters().stream().map(p -> p.getType().asString()).toList()
                    ))
                    .toList();

            return new JavaClassInfo(
                    packageName,
                    clazz.getNameAsString(),
                    detectLayer(clazz.getNameAsString(), annotations),
                    root.relativize(javaFile).toString(),
                    annotations,
                    methods
            );
        } catch (Exception e) {
            throw new IllegalStateException("Java analysis failed: " + javaFile + " / " + e.getMessage(), e);
        }
    }

    private String detectLayer(String className, List<String> annotations) {
        if (annotations.contains("RestController") || annotations.contains("Controller")) return "CONTROLLER";
        if (annotations.contains("Service")) return "SERVICE";
        if (annotations.contains("Repository")) return "REPOSITORY";
        if (annotations.contains("Entity")) return "ENTITY";
        if (className.endsWith("Request") || className.endsWith("Response") || className.endsWith("Dto") || className.endsWith("DTO")) return "DTO";
        if (className.endsWith("Config") || annotations.contains("Configuration")) return "CONFIG";
        return "UNKNOWN";
    }
}
