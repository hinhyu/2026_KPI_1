package com.example.docinsight.java;

import java.util.List;

public record JavaMethodInfo(
        String methodName,
        String returnType,
        List<String> annotations,
        List<String> parameterTypes
) {}
