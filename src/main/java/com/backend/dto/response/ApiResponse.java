package com.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Không trả về các field null
public class ApiResponse<T> {

    @Builder.Default
    private int code = 1000; // 1000 là thành công

    private String message;

    private T result;
}