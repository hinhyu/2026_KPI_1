package com.example.docinsight.api;

public record ApiEndpointInfo(
        String controllerClass,
        String httpMethod,
        String path,
        String handlerMethod,
        String requestDto,
        String responseDto,
        String filePath
) {}
