package com.example.docinsight.summary;

/**
 * 빌드 도구/런타임 메타데이터. AI가 추측하지 않도록 실제 빌드 파일에서 추출한다.
 * 확인 불가한 값은 UNKNOWN("확인 필요")으로 두어 hallucination을 차단한다 (PPT 슬라이드6).
 */
public record BuildInfo(
        String buildTool,
        String javaVersion,
        String springBootVersion
) {
    public static final String UNKNOWN = "확인 필요";

    public static BuildInfo unknown() {
        return new BuildInfo(UNKNOWN, UNKNOWN, UNKNOWN);
    }
}
