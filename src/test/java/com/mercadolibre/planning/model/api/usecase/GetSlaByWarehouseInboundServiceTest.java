package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseInboundService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("PMD.LongVariable")
@ExtendWith(MockitoExtension.class)
public class GetSlaByWarehouseInboundServiceTest {

    private static final String WAREHOUSE = "ARBA01";
    private static final String TIME_ZONE = "America/Argentina/Buenos_Aires";
    private static final ZonedDateTime DAY = ZonedDateTime.now();
    private static final ZonedDateTime SLA_FROM = DAY;
    private static final ZonedDateTime SLA_TO = SLA_FROM.plusDays(1);
    private static final List<ZonedDateTime> BACKLOG = List.of(DAY, DAY.plusDays(1), DAY.plusDays(2));

    @InjectMocks
    private GetSlaByWarehouseInboundService getSlaByWarehouseInboundService;

    @Test
    public void slaByBacklog() {
        final List<GetSlaByWarehouseOutput> result = getSlaByWarehouseInboundService.execute(
                generateInputByBacklog());

        assertEquals(expectedSlaByBacklog(), result);
    }

    @Test
    public void defaultBacklog() {
        final List<GetSlaByWarehouseOutput> result = getSlaByWarehouseInboundService.execute(
                new GetSlaByWarehouseInput(WAREHOUSE, SLA_FROM, SLA_TO, null, TIME_ZONE)
        );

        assertEquals(0, result.size());
    }

    private GetSlaByWarehouseInput generateInputByBacklog() {
        return new GetSlaByWarehouseInput(WAREHOUSE, SLA_FROM, SLA_TO, BACKLOG, TIME_ZONE);
    }

    private List<GetSlaByWarehouseOutput> expectedSlaByBacklog() {
        return List.of(
                GetSlaByWarehouseOutput.builder()
                        .logisticCenterId(WAREHOUSE)
                        .date(DAY)
                        .build(),

                GetSlaByWarehouseOutput.builder()
                        .logisticCenterId(WAREHOUSE)
                        .date(DAY.plusDays(1))
                        .build()
        );
    }


}
