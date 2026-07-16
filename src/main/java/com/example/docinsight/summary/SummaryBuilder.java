package com.example.docinsight.summary;

import com.example.docinsight.api.ApiEndpointInfo;
import com.example.docinsight.java.JavaClassInfo;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SummaryBuilder {

    public ProjectSummary build(
            Path root,
            List<JavaClassInfo> classes,
            List<ApiEndpointInfo> endpoints,
            Map<String, List<String>> configKeysByFile,
            List<Path> markdownFiles
    ) {
        Map<String, Long> layerCounts = classes.stream()
                .collect(Collectors.groupingBy(JavaClassInfo::layerType, Collectors.counting()));

        List<String> packages = classes.stream()
                .map(JavaClassInfo::packageName)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .sorted()
                .limit(100)
                .toList();

        List<JavaClassInfo> importantClasses = classes.stream()
                .filter(c -> List.of(
                        "CONTROLLER", "SERVICE", "REPOSITORY", "ENTITY", "CONFIG", "COMPONENT", "INTERFACE"
                ).contains(c.layerType()))
                .limit(200)
                .toList();

        return new ProjectSummary(
                root.getFileName().toString(),
                layerCounts,
                packages,
                importantClasses,
                endpoints,
                configKeysByFile,
                markdownFiles.stream().map(root::relativize).map(Path::toString).toList()
        );
    }
}
