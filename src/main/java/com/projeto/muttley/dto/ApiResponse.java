package com.projeto.muttley.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private T data;

    public static <T> ApiResponse<T> success(int status, String message, String path, T data) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status)
                .message(message)
                .path(path)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String error, String message, String path) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}
