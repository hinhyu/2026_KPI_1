package com.example.docinsight.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaStructureAnalyzerTest {

    private final JavaStructureAnalyzer analyzer = new JavaStructureAnalyzer();

    @TempDir
    Path tempDir;

    @Test
    void analyzesTopLevelAndNestedTypes() throws Exception {
        Path javaFile = tempDir.resolve("NestedTypesSample.java");
        Files.writeString(javaFile, """
                package com.sample.order;

                public class NestedTypesSample {
                    public interface OrderPolicy {
                        boolean isAllowed(String orderId);
                    }

                    public enum OrderStatus {
                        CREATED, PAID
                    }

                    public static class OrderView {
                        public String getOrderId() {
                            return "1";
                        }
                    }
                }
                """);

        List<JavaClassInfo> classes = analyzer.analyze(tempDir, javaFile);

        assertEquals(4, classes.size());
        assertTrue(classes.stream().anyMatch(c -> c.className().equals("NestedTypesSample") && c.layerType().equals("UNKNOWN")));
        assertTrue(classes.stream().anyMatch(c -> c.className().equals("OrderPolicy") && c.layerType().equals("INTERFACE")));
        assertTrue(classes.stream().anyMatch(c -> c.className().equals("OrderStatus") && c.layerType().equals("ENUM")));
        assertTrue(classes.stream().anyMatch(c -> c.className().equals("OrderView") && c.layerType().equals("UNKNOWN")));
    }

    @Test
    void analyzesJavaRecordsAsDto() throws Exception {
        Path javaFile = tempDir.resolve("OrderRequest.java");
        Files.writeString(javaFile, """
                package com.sample.order;

                public record OrderRequest(String customerId, String productCode, int quantity) {}
                """);

        List<JavaClassInfo> classes = analyzer.analyze(tempDir, javaFile);

        assertEquals(1, classes.size());
        assertEquals("OrderRequest", classes.getFirst().className());
        assertEquals("DTO", classes.getFirst().layerType());
    }
}
