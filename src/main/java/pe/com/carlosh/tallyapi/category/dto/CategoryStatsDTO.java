package pe.com.carlosh.tallyapi.category.dto;

import java.math.BigDecimal;

public record CategoryStatsDTO(
        long total,
        long limit,
        String topName,
        BigDecimal topSpent
) {

}
