package com.backend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CompetitionSetupRequest {
    private EventRequest event;
    private List<PrizeRequest> prizes;
}
