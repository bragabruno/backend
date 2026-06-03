package com.bragdev.frauddetection.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetail {
    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;
    private Instant timestamp;
    private List<FieldError> errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String message;
    }

    public static ProblemDetail of(int status, String title, String detail) {
        return ProblemDetail.builder()
                .type("about:blank")
                .title(title)
                .status(status)
                .detail(detail)
                .timestamp(Instant.now())
                .build();
    }

    public static ProblemDetail of(int status, String title, String detail, List<FieldError> errors) {
        return ProblemDetail.builder()
                .type("about:blank")
                .title(title)
                .status(status)
                .detail(detail)
                .timestamp(Instant.now())
                .errors(errors)
                .build();
    }
}
