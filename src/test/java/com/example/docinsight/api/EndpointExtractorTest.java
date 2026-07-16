package com.example.docinsight.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndpointExtractorTest {

    private final EndpointExtractor extractor = new EndpointExtractor();

    @TempDir
    Path tempDir;

    @Test
    void extractsRequestMappingHttpMethodsAndParams() throws Exception {
        Path javaFile = tempDir.resolve("LegacyOrderController.java");
        Files.writeString(javaFile, """
                package com.sample.order;

                import org.springframework.web.bind.annotation.*;

                @RestController
                @RequestMapping("/api/legacy")
                public class LegacyOrderController {

                    @Transactional
                    @RequestMapping(value = "/orders", method = RequestMethod.GET)
                    public String listOrders(@RequestParam(name = "status") String status) {
                        return status;
                    }

                    @RequestMapping(path = {"/orders/{orderId}", "/order/{orderId}"}, method = {RequestMethod.GET, RequestMethod.HEAD})
                    public String getLegacyOrder(@PathVariable("orderId") String orderId) {
                        return orderId;
                    }
                }
                """);

        List<ApiEndpointInfo> endpoints = extractor.extract(tempDir, javaFile);

        assertEquals(5, endpoints.size());
        assertTrue(endpoints.stream().anyMatch(e ->
                e.httpMethod().equals("GET")
                        && e.path().equals("/api/legacy/orders")
                        && e.queryParams().equals(List.of("status"))
        ));
        assertTrue(endpoints.stream().anyMatch(e ->
                e.httpMethod().equals("GET")
                        && e.path().equals("/api/legacy/orders/{orderId}")
                        && e.pathVariables().equals(List.of("orderId"))
        ));
        assertTrue(endpoints.stream().anyMatch(e ->
                e.httpMethod().equals("HEAD")
                        && e.path().equals("/api/legacy/order/{orderId}")
        ));
    }

    @Test
    void ignoresNonMappingAnnotationsBeforeMapping() throws Exception {
        Path javaFile = tempDir.resolve("AnnotatedController.java");
        Files.writeString(javaFile, """
                package com.sample.order;

                import org.springframework.web.bind.annotation.*;

                @RestController
                public class AnnotatedController {
                    @Override
                    @GetMapping("/health")
                    public String health() {
                        return "ok";
                    }
                }
                """);

        List<ApiEndpointInfo> endpoints = extractor.extract(tempDir, javaFile);

        assertEquals(1, endpoints.size());
        assertEquals("GET", endpoints.getFirst().httpMethod());
        assertEquals("/health", endpoints.getFirst().path());
    }
}
