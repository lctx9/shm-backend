package com.backend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class MatrixBatchUpdateRequest {
    private List<MatrixUpdateItem> updates;

    @Data
    public static class MatrixUpdateItem {
        private Long matrixId;
        private MatrixUpdateRequest config;
    }
}
