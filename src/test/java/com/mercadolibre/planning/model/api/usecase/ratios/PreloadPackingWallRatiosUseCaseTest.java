package com.mercadolibre.planning.model.api.usecase.ratios;

import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.client.db.repository.ratios.RatiosRepository;
import com.mercadolibre.planning.model.api.domain.entity.PhotoRequest;
import com.mercadolibre.planning.model.api.domain.entity.ratios.Ratio;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.Photo;
import com.mercadolibre.planning.model.api.domain.usecase.ratios.PreloadPackingWallRatiosUseCase;
import com.mercadolibre.planning.model.api.gateway.BacklogGateway;
import com.mercadolibre.planning.model.api.web.controller.ratios.response.PreloadPackingRatiosOutput;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.BadSqlGrammarException;

@ExtendWith(MockitoExtension.class)
class PreloadPackingWallRatiosUseCaseTest {
  private static final String ARTW01 = "ARTW01";

  private static final String COTW01 = "COTW01";

  private static final String TO_GROUP_STEP = "TO_GROUP";

  private static final String TO_PACK_STEP = "TO_PACK";
  private static final String AREA_MU = "MU";

  private static final String AREA_PW = "PW";

  private static final List<Instant> LAST_HOUR = of(Instant.parse("2023-02-12T14:00:00Z"), Instant.parse("2023-02-12T15:00:00Z"));

  private static final List<Instant> MULTIPLE_HOURS = of(
      Instant.parse("2023-02-12T00:00:00Z"),
      Instant.parse("2023-02-12T01:00:00Z"),
      Instant.parse("2023-02-12T02:00:00Z"),
      Instant.parse("2023-02-12T03:00:00Z"),
      Instant.parse("2023-02-12T04:00:00Z"),
      Instant.parse("2023-02-12T05:00:00Z"),
      Instant.parse("2023-02-12T06:00:00Z")
  );

  @InjectMocks
  private PreloadPackingWallRatiosUseCase preloadPackingWallRatiosUseCase;

  @Mock
  private ConfigurationRepository configurationRepository;

  @Mock
  private BacklogGateway backlogGateway;

  @Mock
  private RatiosRepository ratiosRepository;

  @Captor
  private ArgumentCaptor<List<Ratio>> ratiosCaptor;

  @Test
  void testSaveLastHourRatiosSuccessfullyMultipleWarehouses() {
    when(configurationRepository.findConfiguratedLogisticCenter()).thenReturn(of(ARTW01, COTW01));
    mockPhotoResponseForLastHour(makeRequestForPhotos(ARTW01, LAST_HOUR.get(0), LAST_HOUR.get(1)));
    mockPhotoResponseForLastHour(makeRequestForPhotos(COTW01, LAST_HOUR.get(0), LAST_HOUR.get(1)));
    preloadPackingWallRatiosUseCase.execute(LAST_HOUR.get(0), LAST_HOUR.get(1));

    verify(ratiosRepository, times(2)).saveAll(ratiosCaptor.capture());

    assertEquals(2, ratiosCaptor.getAllValues().size());

    final List<Ratio> firstWarehouse = ratiosCaptor.getAllValues().get(0);
    final List<Ratio> secondWarehouse = ratiosCaptor.getAllValues().get(1);

    assertEquals(1, firstWarehouse.size());
    final var firstWarehouseRatio = firstWarehouse.get(0);
    assertEquals(FBM_WMS_OUTBOUND, firstWarehouseRatio.getWorkflow());
    assertEquals(ARTW01, firstWarehouseRatio.getLogisticCenterId());
    assertEquals(LAST_HOUR.get(0), firstWarehouseRatio.getDate());
    assertEquals(0.625, firstWarehouseRatio.getValue());

    assertEquals(1, secondWarehouse.size());
    final var secondWarehouseRatio = secondWarehouse.get(0);
    assertEquals(FBM_WMS_OUTBOUND, secondWarehouseRatio.getWorkflow());
    assertEquals(COTW01, secondWarehouseRatio.getLogisticCenterId());
    assertEquals(LAST_HOUR.get(0), secondWarehouseRatio.getDate());
    //Second warehouse do not have unit movement between hours therefore returns <DEFAULT_RATIO>
    assertEquals(0.5, secondWarehouseRatio.getValue());
  }

  @Test
  void testSaveMultipleRatiosByWarehouse() {
    when(configurationRepository.findConfiguratedLogisticCenter()).thenReturn(of(ARTW01));
    mockMultiplePhotoResponse(makeRequestForPhotos(ARTW01, MULTIPLE_HOURS.get(0), MULTIPLE_HOURS.get(6)));
    preloadPackingWallRatiosUseCase.execute(MULTIPLE_HOURS.get(0), MULTIPLE_HOURS.get(6));
    verify(ratiosRepository, times(1)).saveAll(argThat(
        ratios -> {
          List<Ratio> ratioList = StreamSupport.stream(ratios.spliterator(), false)
              .sorted(Comparator.comparing(Ratio::getDate))
              .collect(Collectors.toList());
          assertEquals(MULTIPLE_HOURS.get(0), ratioList.get(0).getDate());
          assertEquals(MULTIPLE_HOURS.get(1), ratioList.get(1).getDate());
          assertEquals(MULTIPLE_HOURS.get(2), ratioList.get(2).getDate());
          assertEquals(MULTIPLE_HOURS.get(3), ratioList.get(3).getDate());
          assertEquals(MULTIPLE_HOURS.get(4), ratioList.get(4).getDate());
          assertEquals(MULTIPLE_HOURS.get(5), ratioList.get(5).getDate());
          assertEquals(0.625, ratioList.get(0).getValue());
          assertEquals(0.2, ratioList.get(1).getValue());
          assertEquals(0.1, ratioList.get(2).getValue());
          assertEquals(1.0, ratioList.get(3).getValue());
          assertEquals(0.75, ratioList.get(4).getValue());
          assertEquals(0.5, ratioList.get(5).getValue());
          return 6 == ratioList.size();
        }
    ));
  }

  @Test
  void testIncompletePhotosToCalculateProcessedUnits() {
    when(configurationRepository.findConfiguratedLogisticCenter()).thenReturn(of(ARTW01));
    mockPhotoResponseForIncompletePhotos(makeRequestForPhotos(ARTW01, LAST_HOUR.get(0), LAST_HOUR.get(1)));
    preloadPackingWallRatiosUseCase.execute(LAST_HOUR.get(0), LAST_HOUR.get(1));
    verify(ratiosRepository, times(1)).saveAll(argThat(
        ratios -> {
          final List<Ratio> ratioList = StreamSupport.stream(ratios.spliterator(), false)
              .sorted(Comparator.comparing(Ratio::getDate))
              .collect(Collectors.toList());
          return ratioList.isEmpty();
        }
    ));
  }

  @Test
  void testDataBaseFailure() {
    when(configurationRepository.findConfiguratedLogisticCenter()).thenReturn(of(ARTW01));
    when(ratiosRepository.saveAll(any())).thenThrow(BadSqlGrammarException.class);
    mockPhotoResponseForLastHour(makeRequestForPhotos(ARTW01, LAST_HOUR.get(0), LAST_HOUR.get(1)));
    final List<PreloadPackingRatiosOutput> response = preloadPackingWallRatiosUseCase.execute(LAST_HOUR.get(0), LAST_HOUR.get(1));
    assertEquals(1, response.size());
    assertEquals(ARTW01, response.get(0).getLogisticCenterId());
    assertEquals(0, response.get(0).getRatiosSaved());
  }

  private PhotoRequest makeRequestForPhotos(final String logisticCenterId, final Instant dateFrom, final Instant dateTo) {
    return new PhotoRequest(
        of(FBM_WMS_OUTBOUND),
        logisticCenterId,
        of("to_pack", "to_group"),
        dateFrom,
        dateTo,
        null,
        null,
        dateFrom,
        dateFrom.plus(72, ChronoUnit.HOURS),
        of(STEP, AREA)
    );
  }

  private void mockPhotoResponseForLastHour(final PhotoRequest request) {
    final List<Photo.Group> groups;
    if (ARTW01.equals(request.getLogisticCenterId())) {
      groups = of(
          createPhotoGroup(AREA_MU, TO_GROUP_STEP, 100, 100),
          createPhotoGroup(AREA_MU, TO_PACK_STEP, 200, 200),
          createPhotoGroup(AREA_PW, TO_PACK_STEP, 200, 350),
          createPhotoGroup(AREA_MU, TO_GROUP_STEP, 150, 500),
          createPhotoGroup(AREA_MU, TO_PACK_STEP, 350, 440),
          createPhotoGroup(AREA_PW, TO_PACK_STEP, 518, 320)
      );
    } else {
      groups = of(
          createPhotoGroup(AREA_MU, TO_GROUP_STEP, 100, 0),
          createPhotoGroup(AREA_MU, TO_PACK_STEP, 200, 0),
          createPhotoGroup(AREA_PW, TO_PACK_STEP, 200, 350),
          createPhotoGroup(AREA_MU, TO_GROUP_STEP, 150, 0),
          createPhotoGroup(AREA_MU, TO_PACK_STEP, 350, 0),
          createPhotoGroup(AREA_PW, TO_PACK_STEP, 518, 320)
      );
    }
    when(backlogGateway.getPhotos(request)).thenReturn(
        of(
            new Photo(LAST_HOUR.get(0), groups.subList(0, 3)),
            new Photo(LAST_HOUR.get(1), groups.subList(3, 6))
        )
    );
  }

  private void mockMultiplePhotoResponse(final PhotoRequest request) {
    final List<Photo.Group> groups;
    groups = of(
        createPhotoGroup(AREA_MU, TO_GROUP_STEP, 100, 0),
        createPhotoGroup(AREA_MU, TO_PACK_STEP, 200, 0),
        createPhotoGroup(AREA_PW, TO_PACK_STEP, 200, 350),
        createPhotoGroup(AREA_MU, TO_GROUP_STEP, 150, 200),
        createPhotoGroup(AREA_MU, TO_PACK_STEP, 350, 120),
        createPhotoGroup(AREA_PW, TO_PACK_STEP, 518, 320),
        createPhotoGroup(AREA_MU, TO_GROUP_STEP, 100, 260),
        createPhotoGroup(AREA_MU, TO_PACK_STEP, 200, 360),
        createPhotoGroup(AREA_PW, TO_PACK_STEP, 200, 340),
        createPhotoGroup(AREA_MU, TO_GROUP_STEP, 150, 270),
        createPhotoGroup(AREA_MU, TO_PACK_STEP, 350, 450),
        createPhotoGroup(AREA_PW, TO_PACK_STEP, 518, 320),
        createPhotoGroup(AREA_MU, TO_GROUP_STEP, 150, 300),
        createPhotoGroup(AREA_MU, TO_PACK_STEP, 350, 400),
        createPhotoGroup(AREA_PW, TO_PACK_STEP, 518, 320),
        createPhotoGroup(AREA_MU, TO_GROUP_STEP, 150, 450),
        createPhotoGroup(AREA_MU, TO_PACK_STEP, 350, 450),
        createPhotoGroup(AREA_PW, TO_PACK_STEP, 518, 320),
        createPhotoGroup(AREA_MU, TO_GROUP_STEP, 150, 300),
        createPhotoGroup(AREA_MU, TO_PACK_STEP, 350, 450),
        createPhotoGroup(AREA_PW, TO_PACK_STEP, 518, 320)
    );

    for (int i = 0; i < HOURS.between(request.getDateFrom(), request.getDateTo()); i++) {

      when(backlogGateway.getPhotos(makeRequestForPhotos(
          request.getLogisticCenterId(),
          request.getDateFrom().plus(i, HOURS),
          request.getDateFrom().plus(1 + i, HOURS))
      )).thenReturn(
          of(
              new Photo(MULTIPLE_HOURS.get(i), groups.subList(i * 3, (i * 3) + 3)),
              new Photo(MULTIPLE_HOURS.get(i + 1), groups.subList(((i * 3) + 3), (i * 3) + 6))
          )
      );
    }
  }

  private void mockPhotoResponseForIncompletePhotos(final PhotoRequest request) {
    final List<Photo.Group> groups = of(
        createPhotoGroup(AREA_MU, TO_GROUP_STEP, 100, 100),
        createPhotoGroup(AREA_MU, TO_PACK_STEP, 200, 200),
        createPhotoGroup(AREA_PW, TO_PACK_STEP, 200, 350)
    );

    when(backlogGateway.getPhotos(request)).thenReturn(
        of(
            new Photo(LAST_HOUR.get(0), groups.subList(0, 3))
        )
    );
  }

  private Photo.Group createPhotoGroup(final String area, final String step, final int total, final int accumulatedTotal) {
    final Map<String, String> keys = new ConcurrentHashMap<>();
    keys.put("area", area);
    keys.put("step", step);
    return new Photo.Group(keys, total, accumulatedTotal);
  }
}
