package com.example.docinsight.summary;

import com.example.docinsight.api.ApiEndpointInfo;
import com.example.docinsight.java.JavaClassInfo;

import java.util.List;
import java.util.Map;

public record ProjectSummary(
        String projectName,
        BuildInfo buildInfo,
        Map<String, Long> layerCounts,
        List<String> packages,
        List<JavaClassInfo> importantClasses,
        List<ApiEndpointInfo> endpoints,
        Map<String, List<String>> configKeysByFile,
        List<String> markdownFiles
) {}
