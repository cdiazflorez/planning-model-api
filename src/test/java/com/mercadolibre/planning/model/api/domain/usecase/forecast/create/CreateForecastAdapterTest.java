package com.mercadolibre.planning.model.api.domain.usecase.forecast.create;

import static com.mercadolibre.planning.model.api.util.TestUtils.CALLER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.WEEK;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCreateForecastRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.forecast.CreateForecastAdapter;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.CreateForecastRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.ProcessingDistributionDataRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.ProcessingDistributionRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateForecastAdapterTest {

  private static final String LOGISTIC_CENTER_ID = "ARTW01";
  private static final String HEADCOUNT_TYPE_KEY = "headcount_type";

  @Test
  void createInputWithNullEntities() {
    final var request = new CreateForecastRequest(
        LOGISTIC_CENTER_ID,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        WEEK,
        CALLER_ID
    );
    final var input = CreateForecastAdapter.createStaffingPlan(Workflow.FBM_WMS_OUTBOUND, request);
    assertEquals(0, input.staffingPlan().size());
  }

  @Test
  void createInputWithEmptyEntities() {
    final var request = new CreateForecastRequest(
        LOGISTIC_CENTER_ID,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        WEEK,
        CALLER_ID
    );
    final var input = CreateForecastAdapter.createStaffingPlan(Workflow.FBM_WMS_OUTBOUND, request);
    assertEquals(0, input.staffingPlan().size());
  }

  @Test
  void createOutboundForecast() {
    final var processingDistributions = List.of(
        createPDRequest(ProcessPath.TOT_MONO, ProcessName.PICKING, ProcessingType.EFFECTIVE_WORKERS),
        createPDRequest(ProcessPath.TOT_SINGLE_SKU, ProcessName.PICKING, ProcessingType.EFFECTIVE_WORKERS_NS),
        createPDRequest(ProcessPath.GLOBAL, ProcessName.PACKING, ProcessingType.EFFECTIVE_WORKERS),
        createPDRequest(ProcessPath.GLOBAL, ProcessName.PACKING, ProcessingType.EFFECTIVE_WORKERS_NS)
    );
    final var request = mockCreateForecastRequest(processingDistributions);
    final var input = CreateForecastAdapter.createStaffingPlan(Workflow.FBM_WMS_OUTBOUND, request);
    final var effectiveWorkers = input.staffingPlan().stream()
        .filter(sp -> sp.type() == ProcessingType.EFFECTIVE_WORKERS).toList();
    final var effectiveWorkersNonSystemic = input.staffingPlan().stream()
        .filter(sp -> sp.type() == ProcessingType.EFFECTIVE_WORKERS_NS).toList();
    assertEquals(10, effectiveWorkers.size());
    assertEquals("systemic", effectiveWorkers.get(0).tags().get(HEADCOUNT_TYPE_KEY));

    assertEquals(10, effectiveWorkersNonSystemic.size());
    assertEquals("non_systemic", effectiveWorkersNonSystemic.get(0).tags().get(HEADCOUNT_TYPE_KEY));

    assertNull(request.getHeadcountProductivities().get(0).getHeadcountType());
    assertEquals(0, request.getProcessingDistributions().get(0).getAbilityLevel());
  }

  @Test
  void createInboundForecast() {
    final var processingDistributions = List.of(
        createPDRequest(ProcessPath.GLOBAL, ProcessName.STAGE_IN, ProcessingType.EFFECTIVE_WORKERS),
        createPDRequest(ProcessPath.GLOBAL, ProcessName.PUT_AWAY, ProcessingType.EFFECTIVE_WORKERS)
    );
    final var request = mockCreateForecastRequest(processingDistributions);
    final var input = CreateForecastAdapter.createStaffingPlan(Workflow.FBM_WMS_INBOUND, request);
    assertNotNull(input);
    final var effectiveWorkers = input.staffingPlan().stream()
        .filter(sp -> sp.type() == ProcessingType.EFFECTIVE_WORKERS).toList();
    assertFalse(effectiveWorkers.get(0).tags().containsKey("process_path"));
  }


  private ProcessingDistributionRequest createPDRequest(
      final ProcessPath processPath,
      final ProcessName processName,
      final ProcessingType processingType
  ) {
    return new ProcessingDistributionRequest(
        processPath,
        processName,
        processingType,
        MetricUnit.UNITS,
        List.of(
            new ProcessingDistributionDataRequest(DATE_IN, 35),
            new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 45),
            new ProcessingDistributionDataRequest(DATE_IN.plusHours(2), 55),
            new ProcessingDistributionDataRequest(DATE_IN.plusHours(3), 65),
            new ProcessingDistributionDataRequest(DATE_IN.plusHours(4), 75)
        )
    );
  }
}
