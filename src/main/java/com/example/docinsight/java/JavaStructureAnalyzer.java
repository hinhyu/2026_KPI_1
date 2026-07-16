package com.example.docinsight.java;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class JavaStructureAnalyzer {

    static {
        StaticJavaParser.setConfiguration(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    }
    public List<JavaClassInfo> analyze(Path root, Path javaFile) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);

            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getName().asString())
                    .orElse("");

            Set<TypeDeclaration<?>> types = new LinkedHashSet<>();
            for (TypeDeclaration<?> type : cu.getTypes()) {
                collectTypes(type, types);
            }

            if (types.isEmpty()) {
                throw new IllegalArgumentException("type not found: " + javaFile);
            }

            List<JavaClassInfo> classes = new ArrayList<>();
            for (TypeDeclaration<?> type : types) {
                classes.add(toClassInfo(packageName, root, javaFile, type));
            }
            return classes;
        } catch (Exception e) {
            throw new IllegalStateException("Java analysis failed: " + javaFile + " / " + e.getMessage(), e);
        }
    }

    private void collectTypes(TypeDeclaration<?> type, Set<TypeDeclaration<?>> types) {
        types.add(type);
        for (TypeDeclaration<?> nested : type.getMembers().stream()
                .filter(TypeDeclaration.class::isInstance)
                .map(TypeDeclaration.class::cast)
                .toList()) {
            collectTypes(nested, types);
        }
    }

    private JavaClassInfo toClassInfo(String packageName, Path root, Path javaFile, TypeDeclaration<?> type) {
        List<String> annotations = type.getAnnotations().stream()
                .map(a -> a.getNameAsString())
                .toList();

        List<JavaMethodInfo> methods = type.getMethods().stream()
                .map(method -> new JavaMethodInfo(
                        method.getNameAsString(),
                        method.getType().asString(),
                        method.getAnnotations().stream().map(a -> a.getNameAsString()).toList(),
                        method.getParameters().stream().map(p -> p.getType().asString()).toList()
                ))
                .toList();

        String className = type.getNameAsString();
        return new JavaClassInfo(
                packageName,
                className,
                detectLayer(className, annotations, type),
                root.relativize(javaFile).toString(),
                annotations,
                methods
        );
    }

    private String detectLayer(String className, List<String> annotations, TypeDeclaration<?> type) {
        if (type instanceof EnumDeclaration) {
            return "ENUM";
        }
        if (type instanceof RecordDeclaration
                || className.endsWith("Request")
                || className.endsWith("Response")
                || className.endsWith("Dto")
                || className.endsWith("DTO")) {
            return "DTO";
        }
        if (annotations.contains("RestController") || annotations.contains("Controller")) return "CONTROLLER";
        if (annotations.contains("Service")) return "SERVICE";
        if (annotations.contains("Repository")) return "REPOSITORY";
        if (annotations.contains("Entity")) return "ENTITY";
        if (annotations.contains("Component")) return "COMPONENT";
        if (className.endsWith("Config") || annotations.contains("Configuration")) return "CONFIG";
        if (type instanceof ClassOrInterfaceDeclaration clazz && clazz.isInterface()) return "INTERFACE";
        return "UNKNOWN";
    }
}
