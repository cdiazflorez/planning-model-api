package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static com.mercadolibre.planning.model.api.web.controller.projection.response.BacklogProjectionStatus.CARRY_OVER;
import static com.mercadolibre.planning.model.api.web.controller.projection.response.BacklogProjectionStatus.PROCESSED;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.BacklogProjectionUseCaseFactory;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.GetOutboundBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.CurrentBacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjection;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.QueueProjectionService;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.DeferralStatus;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeferralProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeliveryPromiseProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetSlaProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeferralProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.output.DeferralProjectionOutput;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.api.web.controller.projection.BacklogProjectionAdapter;
import com.mercadolibre.planning.model.api.web.controller.projection.ProjectionController;
import com.mercadolibre.planning.model.api.web.controller.projection.request.AreaShareAtSlaAndProcessDto;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ThroughputDto;
import com.mercadolibre.planning.model.api.web.controller.projection.response.BacklogProjectionByAreaDto;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SuppressWarnings("PMD.LongVariable")
@WebMvcTest(controllers = ProjectionController.class)
class ProjectionControllerTest {

  private static final String URL = "/planning/model/workflows/{workflow}/projections";

  private static final Instant DATE_FROM = Instant.parse("2020-07-27T09:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2020-07-28T08:00:00Z");

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetDeliveryPromiseProjectionUseCase delPromiseProjection;

  @MockBean
  private GetDeferralProjectionUseCase deferralProjectionUseCase;

  @MockBean
  private BacklogProjectionUseCaseFactory backlogProjectionUseCaseFactory;

  @MockBean
  private GetSlaProjectionUseCase getSlaProjectionUseCase;

  @MockBean
  private BacklogProjectionAdapter backlogProjectionAdapter;

  @MockBean
  private QueueProjectionService queueProjectionService;

  @Test
  public void testGetCptProjectionForecastNotFound() throws Exception {
    // GIVEN
    when(getSlaProjectionUseCase.execute(any(GetSlaProjectionInput.class)))
        .thenThrow(ForecastNotFoundException.class);

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/cpts", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("get_cpt_projection_request.json"))
    );

    // THEN
    result.andExpect(status().isNotFound())
        .andExpect(jsonPath("error").value("forecast_not_found"));
  }

  @Test
  public void testGetCptProjection() throws Exception {
    // GIVEN
    final ZonedDateTime etd = parse("2020-01-01T11:00:00Z");
    final ZonedDateTime projectedTime = parse("2020-01-02T10:00:00Z");

    when(getSlaProjectionUseCase.execute(
        new GetSlaProjectionInput(
            FBM_WMS_OUTBOUND,
            WAREHOUSE_ID,
            ProjectionType.CPT,
            List.of(PICKING, PACKING),
            parse("2020-01-01T12:00:00Z[UTC]"),
            parse("2020-01-10T12:00:00Z[UTC]"),
            null,
            "America/Argentina/Buenos_Aires",
            SIMULATION,
            emptyList(),
            false
        )))
        .thenReturn(
            List.of(new CptProjectionOutput(
                etd, projectedTime, 100, new ProcessingTime(45L, MINUTES)
            )));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/cpts", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("get_cpt_projection_request.json"))
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$[0].date")
            .value(etd.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].projected_end_date")
            .value(projectedTime.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].remaining_quantity")
            .value(100))
        .andExpect(jsonPath("$[0].processing_time.value")
            .value(45L))
        .andExpect(jsonPath("$[0].processing_time.unit_metric")
            .value("minutes"));
  }

  @Test
  public void testGetDeliveryPromise() throws Exception {
    // GIVEN
    final ZonedDateTime etd = parse("2021-01-01T11:00:00Z");
    final ZonedDateTime projectedTime = parse("2021-01-02T10:00:00Z");
    final ZonedDateTime payBefore = parse("2021-01-01T07:00:00Z");

    when(delPromiseProjection.execute(GetDeliveryPromiseProjectionInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .dateFrom(parse("2020-01-01T12:00:00Z[UTC]"))
        .dateTo(parse("2020-01-10T12:00:00Z[UTC]"))
        .timeZone("America/Argentina/Buenos_Aires")
        .backlog(emptyList())
        .projectionType(ProjectionType.DEFERRAL)
        .applyDeviation(true)
        .build()
    )).thenReturn(List.of(
        new DeliveryPromiseProjectionOutput(
            etd,
            projectedTime,
            100,
            null,
            new ProcessingTime(240L, MINUTES),
            payBefore,
            false,
            DeferralStatus.NOT_DEFERRED))
    );

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/cpts/delivery_promise", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("get_deferral_projection_request.json"))
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$[0].date")
            .value(etd.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].projected_end_date")
            .value(projectedTime.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].pay_before")
            .value(payBefore.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].remaining_quantity")
            .value(100));
  }

  private void mockDeferralProjection(final Instant etd, final Instant projectedTime) {
    final var results = List.of(
        new DeferralProjectionOutput(
            etd,
            projectedTime,
            100,
            DeferralStatus.DEFERRED_CAP_MAX
        )
    );

    final var dateFrom = parse("2020-01-01T12:00:00Z[UTC]");
    final var dateTo = parse("2020-01-10T12:00:00Z[UTC]");
    when(deferralProjectionUseCase.execute(
        new GetDeferralProjectionInput(
            WAREHOUSE_ID,
            FBM_WMS_OUTBOUND,
            ProjectionType.DEFERRAL,
            dateFrom.toInstant(),
            dateFrom,
            dateTo,
            dateFrom,
            dateTo.plus(72, ChronoUnit.HOURS),
            emptyList(),
            "America/Argentina/Buenos_Aires",
            true,
            null
        )
    )).thenReturn(results);
  }

  @Test
  public void testGetDeferralProjection() throws Exception {
    // GIVEN
    final ZonedDateTime etd = parse("2021-01-01T11:00:00Z");
    final ZonedDateTime projectedTime = parse("2021-01-02T11:30:00Z");

    mockDeferralProjection(etd.toInstant(), projectedTime.toInstant());

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/cpts/deferral_time", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("get_deferral_projection_request.json"))
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$[0].sla")
            .value(etd.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].deferred_at")
            .value(projectedTime.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].deferred_units")
            .value(100))
        .andExpect(jsonPath("$[0].deferral_status")
            .value("deferred_cap_max"));
  }

  @Test
  public void testGetBacklogProjection() throws Exception {
    // GIVEN
    final GetOutboundBacklogProjectionUseCase useCase = Mockito.mock(GetOutboundBacklogProjectionUseCase.class);
    when(backlogProjectionUseCaseFactory.getUseCase(FBM_WMS_OUTBOUND)).thenReturn(useCase);

    when(useCase.execute(any(BacklogProjectionInput.class)))
        .thenReturn(List.of(
            BacklogProjection.builder()
                .processName(WAVING)
                .values(emptyList()).build(),
            BacklogProjection.builder()
                .processName(PICKING)
                .values(emptyList()).build(),
            BacklogProjection.builder()
                .processName(PACKING)
                .values(emptyList()).build()
        ));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/backlogs", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("get_backlog_projection_request.json")));

    // THEN
    //TODO: Add asserts
    result.andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON));
  }

  @Test
  public void testGetBacklogProjectionByArea() throws Exception {
    // GIVEN
    when(
        backlogProjectionAdapter.projectionByArea(
            DATE_FROM,
            DATE_TO,
            FBM_WMS_OUTBOUND,
            List.of(WAVING, PICKING, PACKING),
            getThroughput(),
            getPlanningUnits(),
            getCurrentBacklogBySla(),
            getAreaDistributions()
        )
    ).thenReturn(getProjectionResult());

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/backlogs/grouped/area", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("requests/backlog/get_backlog_projection_by_area_request.json")));

    // THEN
    //TODO: Add asserts
    result.andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON));
  }

  private List<ThroughputDto> getThroughput() {
    ZonedDateTime date = ZonedDateTime.ofInstant(DATE_FROM, ZoneOffset.UTC);
    return List.of(
        new ThroughputDto(FBM_WMS_OUTBOUND, DATE_FROM, WAVING, 100),
        new ThroughputDto(FBM_WMS_OUTBOUND, DATE_FROM, PICKING, 150)
    );
  }

  private List<GetPlanningDistributionOutput> getPlanningUnits() {
    ZonedDateTime dateIn = ZonedDateTime.ofInstant(DATE_FROM, ZoneOffset.UTC);
    ZonedDateTime dateOutA = ZonedDateTime.ofInstant(Instant.parse("2020-07-27T18:00:00Z"), ZoneOffset.UTC);
    ZonedDateTime dateOutB = ZonedDateTime.ofInstant(Instant.parse("2020-07-27T19:00:00Z"), ZoneOffset.UTC);

    return List.of(
        new GetPlanningDistributionOutput(dateIn, dateOutA, UNITS, 150, false),
        new GetPlanningDistributionOutput(dateIn, dateOutB, UNITS, 130, false)
    );
  }

  private List<CurrentBacklogBySla> getCurrentBacklogBySla() {
    return List.of(
        new CurrentBacklogBySla(WAVING, DATE_FROM, 1500),
        new CurrentBacklogBySla(PICKING, DATE_FROM, 1000),
        new CurrentBacklogBySla(PACKING, DATE_FROM, 500)
    );
  }

  private List<AreaShareAtSlaAndProcessDto> getAreaDistributions() {
    return List.of(
        new AreaShareAtSlaAndProcessDto(PICKING, DATE_FROM, "RK-H", 0.5),
        new AreaShareAtSlaAndProcessDto(PICKING, DATE_FROM, "RK-L", 0.5)
    );
  }

  private List<BacklogProjectionByAreaDto> getProjectionResult() {
    return List.of(
        new BacklogProjectionByAreaDto(DATE_FROM, WAVING, "global", PROCESSED, 10),
        new BacklogProjectionByAreaDto(DATE_FROM, WAVING, "global", CARRY_OVER, 20),
        new BacklogProjectionByAreaDto(DATE_FROM, PICKING, "RK-H", PROCESSED, 15),
        new BacklogProjectionByAreaDto(DATE_FROM, PICKING, "RK-H", CARRY_OVER, 25),
        new BacklogProjectionByAreaDto(DATE_FROM, PICKING, "RK-L", PROCESSED, 32),
        new BacklogProjectionByAreaDto(DATE_FROM, PICKING, "RK-L", CARRY_OVER, 33),
        new BacklogProjectionByAreaDto(DATE_FROM, PACKING, "global", PROCESSED, 38),
        new BacklogProjectionByAreaDto(DATE_FROM, PACKING, "global", CARRY_OVER, 34)
    );
  }
}
