package com.example.docinsight.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * outputs/ai-request-log.json 에 요청 이력을 누적 저장하고,
 * 이번 달 누적 비용을 집계해 예산 통제에 사용한다 (단일 스레드 CLI 기준 read-modify-write).
 */
@Component
public class AiRequestLog {

    private final ObjectMapper objectMapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void append(Path outputDir, AiRequestLogEntry entry) {
        try {
            Path logPath = outputDir.resolve("ai-request-log.json");
            List<AiRequestLogEntry> entries = read(logPath);
            entries.add(entry);
            objectMapper.writeValue(logPath.toFile(), entries);
        } catch (IOException e) {
            System.err.println("[WARN] ai-request-log 저장 실패: " + e.getMessage());
        }
    }

    /** 이번 달(YYYY-MM) 누적 비용(원). */
    public double monthlyCostKrw(Path outputDir) {
        Path logPath = outputDir.resolve("ai-request-log.json");
        String thisMonth = YearMonth.now().toString();
        double sum = 0;
        for (AiRequestLogEntry e : read(logPath)) {
            if (e.timestamp() != null && e.timestamp().startsWith(thisMonth)) {
                sum += e.costKrw();
            }
        }
        return sum;
    }

    public static String now() {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private List<AiRequestLogEntry> read(Path logPath) {
        if (!Files.exists(logPath)) {
            return new ArrayList<>();
        }
        try {
            AiRequestLogEntry[] arr = objectMapper.readValue(logPath.toFile(), AiRequestLogEntry[].class);
            List<AiRequestLogEntry> list = new ArrayList<>();
            for (AiRequestLogEntry e : arr) {
                list.add(e);
            }
            return list;
        } catch (IOException e) {
            System.err.println("[WARN] ai-request-log 읽기 실패, 새로 시작: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
