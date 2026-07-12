package com.example.docinsight.java;

import java.util.List;

public record JavaClassInfo(
        String packageName,
        String className,
        String layerType,
        String filePath,
        List<String> annotations,
        List<JavaMethodInfo> methods
) {}
