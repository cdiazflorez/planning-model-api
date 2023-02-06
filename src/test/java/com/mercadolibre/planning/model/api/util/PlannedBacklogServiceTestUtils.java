package com.mercadolibre.planning.model.api.util;

import static com.mercadolibre.planning.model.api.domain.entity.DeviationType.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.DeviationType.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND_TRANSFER;
import static java.time.ZonedDateTime.parse;
import static java.util.List.of;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import java.time.ZonedDateTime;
import java.util.List;

public final class PlannedBacklogServiceTestUtils {
  private static final String WAREHOUSE_ID = "ARBA01";
  private static final String INBOUND_WORKFLOW = "inbound";
  private static final String INBOUND_TRANSFER_WORKFLOW = "inbound-transfer";
  private static final ZonedDateTime DATE_11 = parse("2020-01-01T11:00:00Z");
  private static final String JAN_01_12 = "2020-01-01T12:00:00Z";
  private static final String JAN_01_13 = "2020-01-01T13:00:00Z";
  private static final String JAN_01_14 = "2020-01-01T14:00:00Z";
  private static final String JAN_10_05 = "2020-01-10T05:00:00Z";
  private static final String JAN_10_08 = "2020-01-10T08:00:00Z";
  private static final String JAN_10_09 = "2020-01-10T09:00:00Z";
  private static final String JAN_10_11 = "2020-01-10T11:00:00Z";
  private static final String JAN_10_12 = "2020-01-10T12:00:00Z";
  private static final String JAN_10_13 = "2020-01-10T13:00:00Z";
  private static final String JAN_10_14 = "2020-01-10T14:00:00Z";
  private static final String JAN_10_15 = "2020-01-10T15:00:00Z";
  private static final String JAN_10_17 = "2020-01-10T17:00:00Z";
  private static final String JAN_11_05 = "2020-01-11T05:00:00Z";
  private static final String JAN_11_08 = "2020-01-11T08:00:00Z";
  private static final String JAN_11_12 = "2020-01-11T12:00:00Z";
  private static final String JAN_11_13 = "2020-01-11T13:00:00Z";
  private static final String JAN_11_14 = "2020-01-11T14:00:00Z";
  private static final String JAN_11_18 = "2020-01-11T18:00:00Z";
  private static final String JAN_12_13 = "2020-01-12T13:00:00Z";
  private static final String JAN_12_14 = "2020-01-12T14:00:00Z";
  private static final String JAN_12_17 = "2020-01-12T17:00:00Z";
  private static final String JAN_13_13 = "2020-01-13T13:00:00Z";
  private static final String JAN_13_15 = "2020-01-13T15:00:00Z";

  private PlannedBacklogServiceTestUtils() { }

  public static List<PlannedBacklogService.InboundScheduledBacklog> getPhotoToTestGetInboundScheduledBacklog() {
    return of(
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse(JAN_01_12).toInstant(),
            parse(JAN_01_14).toInstant(),
            null,
            10,
            10
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse(JAN_01_13).toInstant(),
            parse(JAN_01_14).toInstant(),
            null,
            20,
            10
        )
    );
  }

  public static List<PlannedBacklogService.InboundScheduledBacklog> getPhotoToTestGetInboundScheduledBacklogWithDeviation() {
    return of(
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_TRANSFER_WORKFLOW,
            parse(JAN_01_12).toInstant(),
            parse(JAN_01_14).toInstant(),
            null,
            10,
            10
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse(JAN_01_12).toInstant(),
            parse(JAN_01_14).toInstant(),
            null,
            10,
            10
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse(JAN_01_13).toInstant(),
            parse(JAN_01_14).toInstant(),
            null,
            20,
            20
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse("2020-01-01T15:00:00Z").toInstant(),
            parse("2020-01-01T16:00:00Z").toInstant(),
            null,
            30,
            30
        )
    );
  }

  public static List<CurrentForecastDeviation> getDeviationsToTestGetInboundExpectedBacklogWithDeviation() {
    return of(
        createDeviation(DATE_11, parse(JAN_01_12), 1.0, INBOUND, UNITS, null),
        createDeviation(DATE_11, parse(JAN_01_12), 0.5, INBOUND_TRANSFER, UNITS, null)
    );
  }

  public static List<PlannedBacklogService.InboundScheduledBacklog> getPhotoToTestApplyTimeAndUnitsDeviationsToInbound() {
    return of(
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse(JAN_10_08).toInstant(),
            parse("2020-01-12T08:00:00Z").toInstant(),
            Path.PICKUP,
            30,
            20
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse(JAN_10_09).toInstant(),
            parse("2020-01-12T09:00:00Z").toInstant(),
            Path.PICKUP,
            40,
            20
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse(JAN_10_13).toInstant(),
            parse(JAN_12_13).toInstant(),
            Path.SPD,
            10,
            10
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse(JAN_10_14).toInstant(),
            parse(JAN_12_14).toInstant(),
            Path.FTL,
            50,
            20
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_TRANSFER_WORKFLOW,
            parse(JAN_10_15).toInstant(),
            parse(JAN_13_15).toInstant(),
            null,
            10,
            20
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse(JAN_10_17).toInstant(),
            parse(JAN_12_17).toInstant(),
            Path.SPD,
            40,
            20
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_TRANSFER_WORKFLOW,
            parse(JAN_11_08).toInstant(),
            parse("2020-01-13T08:00:00Z").toInstant(),
            Path.TRANSFER,
            30,
            20
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_TRANSFER_WORKFLOW,
            parse(JAN_11_12).toInstant(),
            parse("2020-01-13T12:00:00Z").toInstant(),
            null,
            20,
            20
        ),
        new PlannedBacklogService.InboundScheduledBacklog(
            INBOUND_WORKFLOW,
            parse(JAN_11_13).toInstant(),
            parse(JAN_13_13).toInstant(),
            Path.FTL,
            20,
            20
        )
    );
  }

  public static List<CurrentForecastDeviation> onlyUnitsDeviations() {
    return of(
        createDeviation(parse(JAN_10_05), parse(JAN_10_13), 0.8, INBOUND, UNITS, null),
        createDeviation(parse(JAN_11_05), parse(JAN_11_13), 0.5, INBOUND_TRANSFER, UNITS, null)
    );
  }

  public static List<PlannedUnits> resultsAfterApplyOnlyUnitDeviations() {
    return of(
        new PlannedUnits(parse(JAN_10_08), parse("2020-01-12T08:00:00Z"), 54),
        new PlannedUnits(parse(JAN_10_09), parse("2020-01-12T09:00:00Z"), 72),
        new PlannedUnits(parse(JAN_10_13), parse(JAN_12_13), 18),
        new PlannedUnits(parse(JAN_10_14), parse(JAN_12_14), 50),
        new PlannedUnits(parse(JAN_10_15), parse(JAN_13_15), 10),
        new PlannedUnits(parse(JAN_10_17), parse(JAN_12_17), 40),
        new PlannedUnits(parse(JAN_11_08), parse("2020-01-13T08:00:00Z"), 45),
        new PlannedUnits(parse(JAN_11_12), parse("2020-01-13T12:00:00Z"), 30),
        new PlannedUnits(parse(JAN_11_13), parse(JAN_13_13), 20)
    );
  }

  public static List<CurrentForecastDeviation> onlyTimeDeviations() {
    return of(
        createDeviation(parse(JAN_10_05), parse(JAN_10_13), 180, INBOUND, MINUTES, Path.PICKUP),
        createDeviation(parse(JAN_11_05), parse(JAN_11_13), 360, INBOUND_TRANSFER, MINUTES, null)
    );
  }

  public static List<PlannedUnits> resultsAfterApplyOnlyTimeDeviations() {
    return of(
        new PlannedUnits(parse(JAN_10_11), parse("2020-01-12T11:00:00Z"), 30),
        new PlannedUnits(parse(JAN_10_12), parse("2020-01-12T12:00:00Z"), 40),
        new PlannedUnits(parse(JAN_10_13), parse(JAN_12_13), 10),
        new PlannedUnits(parse(JAN_10_14), parse(JAN_12_14), 50),
        new PlannedUnits(parse(JAN_10_15), parse(JAN_13_15), 10),
        new PlannedUnits(parse(JAN_10_17), parse(JAN_12_17), 40),
        new PlannedUnits(parse(JAN_11_13), parse(JAN_13_13), 20),
        new PlannedUnits(parse(JAN_11_14), parse("2020-01-13T14:00:00Z"), 30),
        new PlannedUnits(parse(JAN_11_18), parse("2020-01-13T18:00:00Z"), 20)
    );
  }

  public static List<CurrentForecastDeviation> firstTimeAndThenUnitsDeviations() {
    return of(
        createDeviation(parse(JAN_10_05), parse(JAN_10_13), 180, INBOUND, MINUTES, Path.PICKUP),
        createDeviation(parse(JAN_10_11), parse(JAN_10_13), 0.6, INBOUND, UNITS, null),
        createDeviation(parse(JAN_11_05), parse(JAN_11_13), 360, INBOUND_TRANSFER, MINUTES, null),
        createDeviation(parse(JAN_11_13), parse(JAN_11_14), 0.5, INBOUND_TRANSFER, UNITS, null)
    );
  }

  public static List<PlannedUnits> resultsAfterApplyFirstTimeAndThenUnitsDeviations() {
    return of(
        new PlannedUnits(parse(JAN_10_11), parse("2020-01-12T11:00:00Z"), 48),
        new PlannedUnits(parse(JAN_10_12), parse("2020-01-12T12:00:00Z"), 64),
        new PlannedUnits(parse(JAN_10_13), parse(JAN_12_13), 16),
        new PlannedUnits(parse(JAN_10_14), parse(JAN_12_14), 50),
        new PlannedUnits(parse(JAN_10_15), parse(JAN_13_15), 10),
        new PlannedUnits(parse(JAN_10_17), parse(JAN_12_17), 40),
        new PlannedUnits(parse(JAN_11_13), parse(JAN_13_13), 20),
        new PlannedUnits(parse(JAN_11_14), parse("2020-01-13T14:00:00Z"), 45),
        new PlannedUnits(parse(JAN_11_18), parse("2020-01-13T18:00:00Z"), 20)
    );
  }

  public static List<CurrentForecastDeviation> firstUnitsAndThenTimeDeviations() {
    return of(
        createDeviation(parse(JAN_10_05), parse(JAN_10_14), 0.8, INBOUND, UNITS, null),
        createDeviation(parse("2020-01-10T10:00:00Z"), parse(JAN_10_15), 120, INBOUND, MINUTES, Path.FTL),
        createDeviation(parse(JAN_11_05), parse(JAN_11_13), 0.5, INBOUND_TRANSFER, UNITS, null),
        createDeviation(parse("2020-01-11T10:00:00Z"), parse(JAN_11_13), 120, INBOUND_TRANSFER, MINUTES, null)
    );
  }

  public static List<PlannedUnits> resultsAfterApplyFirstUnitsAndThenTimeDeviations() {
    return of(
        new PlannedUnits(parse(JAN_10_08), parse("2020-01-12T08:00:00Z"), 54),
        new PlannedUnits(parse(JAN_10_09), parse("2020-01-12T09:00:00Z"), 72),
        new PlannedUnits(parse(JAN_10_13), parse(JAN_12_13), 18),
        new PlannedUnits(parse(JAN_10_15), parse(JAN_13_15), 10),
        new PlannedUnits(parse("2020-01-10T16:00:00Z"), parse("2020-01-12T16:00:00Z"), 90),
        new PlannedUnits(parse(JAN_10_17), parse(JAN_12_17), 40),
        new PlannedUnits(parse(JAN_11_08), parse("2020-01-13T08:00:00Z"), 45),
        new PlannedUnits(parse(JAN_11_13), parse(JAN_13_13), 20),
        new PlannedUnits(parse(JAN_11_14), parse("2020-01-13T14:00:00Z"), 30)
    );
  }

  private static CurrentForecastDeviation createDeviation(
      final ZonedDateTime dateFrom,
      final ZonedDateTime dateTo,
      final double value,
      final Workflow workflow,
      final DeviationType type,
      final Path path
  ) {
    return new CurrentForecastDeviation(
        1L,
        WAREHOUSE_ID,
        dateFrom,
        dateTo,
        value,
        true,
        10L,
        workflow,
        ZonedDateTime.now(),
        ZonedDateTime.now(),
        type,
        path
    );
  }
}
