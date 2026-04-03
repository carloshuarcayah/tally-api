package pe.com.carlosh.tallyapi.category.dto;

import java.math.BigDecimal;

public record CategoryStatsDTO(
        long total,
        String topName,
        BigDecimal topSpent
) {

}
