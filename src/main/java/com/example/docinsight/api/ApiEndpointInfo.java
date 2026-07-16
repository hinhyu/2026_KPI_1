package com.example.docinsight.api;

import java.util.List;

public record ApiEndpointInfo(
        String controllerClass,
        String httpMethod,
        String path,
        String handlerMethod,
        String requestDto,
        String responseDto,
        List<String> pathVariables,
        List<String> queryParams,
        String filePath
) {}
