package com.mercadolibre.planning.model.api.domain.usecase.ratios;

import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.client.db.repository.ratios.RatiosRepository;
import com.mercadolibre.planning.model.api.domain.entity.PhotoRequest;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ratios.Ratios;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.Photo;
import com.mercadolibre.planning.model.api.gateway.BacklogGateway;
import com.mercadolibre.planning.model.api.web.controller.ratios.response.PreloadPackingRatiosOutput;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PreloadPackingWallRatiosUseCase {

  private static final String PW_RATIOS_TYPE = "packing_wall_ratios";

  private static final double DEFAULT_RATIO = 0.5;

  private static final int DEFAULT_PHOTOS_SIZE = 2;

  private static final int SLA_HOURS_AHEAD = 72;

  private static final Map<ProcessName, Predicate<Photo.Group>> FILTER_BY_PROCESS = Map.of(
      PACKING, PreloadPackingWallRatiosUseCase::isPackingTote,
      BATCH_SORTER, PreloadPackingWallRatiosUseCase::isPackingWall
  );

  private final ConfigurationRepository configurationRepository;

  private final RatiosRepository ratiosRepository;

  private final BacklogGateway backlogGateway;

  private static boolean isPackingWall(final Photo.Group group) {
    return group.getStep()
        .map("TO_GROUP"::equals)
        .orElse(false);
  }

  private static boolean isPackingTote(final Photo.Group group) {
    final var isPacking = group.getStep()
        .map("TO_PACK"::equals)
        .orElse(false);

    final var isPackingWall = group.getArea()
        .map("PW"::equals)
        .orElse(false);

    return isPacking && !isPackingWall;
  }

  public List<PreloadPackingRatiosOutput> execute(Instant dateFrom, Instant dateTo) {
    final List<String> allLogisticCenters = configurationRepository.findConfiguratedLogisticCenter();
    final Instant lastMonth = Instant.now().truncatedTo(HOURS).minus(30, DAYS);
    ratiosRepository.deleteRatios(lastMonth, PW_RATIOS_TYPE);
    return allLogisticCenters.stream()
        .map(logisticCenter -> calculateRatiosByLogisticCenter(logisticCenter, dateFrom, dateTo))
        .collect(Collectors.toList());
  }

  private PreloadPackingRatiosOutput calculateRatiosByLogisticCenter(String logisticCenterId, Instant dateFrom, Instant dateTo) {
    final List<ProcessedUnitsAtHourAndProcess> processedUnits = getProcessedUnitsPerHourByProcess(logisticCenterId, dateFrom, dateTo);
    final Map<Instant, RatioGroup> ratioGroupByDate = transformProcessedUnitsIntoRatioGroups(processedUnits);
    final List<Ratios> ratios = ratioGroupByDate.entrySet()
        .stream()
        .map(ratioGroup -> new Ratios(
            FBM_WMS_OUTBOUND,
            logisticCenterId,
            PW_RATIOS_TYPE,
            ratioGroup.getKey(),
            calculateRatio(ratioGroup.getValue().getPackingWallUnits(), ratioGroup.getValue().getPackingToteUnits())
        )).collect(Collectors.toList());
    try {
      final List<Ratios> savedRatios = ratiosRepository.saveAll(ratios);
      return new PreloadPackingRatiosOutput(logisticCenterId, savedRatios.size());
    } catch (Exception e) {
      log.error(e.getMessage());
      return new PreloadPackingRatiosOutput(logisticCenterId, 0);
    }
  }

  private List<ProcessedUnitsAtHourAndProcess> getProcessedUnitsPerHourByProcess(final String logisticCenterId,
                                                                                 final Instant dateFrom,
                                                                                 final Instant dateTo) {

    return LongStream.range(0, HOURS.between(dateFrom, dateTo))
        .mapToObj(i -> getProcessedUnitsByCoupleOfPhotos(logisticCenterId, dateFrom.plus(i, HOURS), dateFrom.plus(i + 1, HOURS)))
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private List<ProcessedUnitsAtHourAndProcess> getProcessedUnitsByCoupleOfPhotos(final String logisticCenterId,
                                                                                 final Instant dateFrom,
                                                                                 final Instant dateTo) {

    final List<Photo> photos = getBacklog(logisticCenterId, dateFrom, dateTo);
    return photos.size() < DEFAULT_PHOTOS_SIZE
        ? emptyList()
        : Stream.of(
            calculateProcessedUnitsByProcess(photos.get(0), photos.get(1), BATCH_SORTER),
            calculateProcessedUnitsByProcess(photos.get(0), photos.get(1), PACKING))
        .collect(Collectors.toList());
  }

  private ProcessedUnitsAtHourAndProcess calculateProcessedUnitsByProcess(
      final Photo currentPhoto,
      final Photo nextPhoto,
      final ProcessName process
  ) {

    final ProcessedUnitsAtHourAndProcess currentHourProcessedUnits = currentPhoto.getGroups()
        .stream()
        .filter(FILTER_BY_PROCESS.get(process))
        .collect(
            Collectors.collectingAndThen(
                Collectors.reducing(0, Photo.Group::getAccumulatedTotal, Integer::sum),
                total -> new ProcessedUnitsAtHourAndProcess(currentPhoto.getTakenOn(), process, total)
            )
        );

    final ProcessedUnitsAtHourAndProcess nextHourProcessedUnits = nextPhoto.getGroups()
        .stream()
        .filter(FILTER_BY_PROCESS.get(process))
        .collect(
            Collectors.collectingAndThen(
                Collectors.reducing(0, Photo.Group::getAccumulatedTotal, Integer::sum),
                total -> new ProcessedUnitsAtHourAndProcess(currentPhoto.getTakenOn(), process, total)
            )
        );

    return new ProcessedUnitsAtHourAndProcess(
        currentHourProcessedUnits.getDate(),
        currentHourProcessedUnits.getProcess(),
        Math.max(nextHourProcessedUnits.getQuantity() - currentHourProcessedUnits.getQuantity(), 0)
    );
  }

  private Map<Instant, RatioGroup> transformProcessedUnitsIntoRatioGroups(final List<ProcessedUnitsAtHourAndProcess> processedUnits) {
    return processedUnits
        .stream()
        .collect(Collectors.toMap(
            ProcessedUnitsAtHourAndProcess::getDate,
            pu -> new RatioGroup(
                pu.getProcess().equals(PACKING) ? pu.getQuantity() : 0,
                pu.getProcess().equals(BATCH_SORTER) ? pu.getQuantity() : 0),
            (oldPu, newPu) -> new RatioGroup(
                newPu.getPackingToteUnits() + oldPu.getPackingToteUnits(),
                newPu.getPackingWallUnits() + oldPu.getPackingWallUnits()
            )
        ));
  }

  private double calculateRatio(final int packingWallUnits, final int packingToteUnits) {
    return packingWallUnits + packingToteUnits == 0
        ? DEFAULT_RATIO
        : (double) packingWallUnits / (double) (packingWallUnits + packingToteUnits);
  }

  private List<Photo> getBacklog(final String logisticCenterId,
                                 final Instant dateFrom,
                                 final Instant dateTo) {

    return backlogGateway.getPhotos(
            new PhotoRequest(
                List.of(FBM_WMS_OUTBOUND),
                logisticCenterId,
                List.of("to_pack", "to_group"),
                dateFrom,
                dateTo,
                null,
                null,
                dateFrom,
                dateFrom.plus(SLA_HOURS_AHEAD, HOURS),
                List.of(STEP, AREA)
            )
        ).stream()
        .filter(photo -> photo.getTakenOn().equals(photo.getTakenOn().truncatedTo(HOURS)))
        .sorted(Comparator.comparing(Photo::getTakenOn))
        .collect(Collectors.toList());
  }

  @Value
  private static class ProcessedUnitsAtHourAndProcess {
    Instant date;

    ProcessName process;

    int quantity;
  }

  @Value
  static class RatioGroup {
    int packingToteUnits;

    int packingWallUnits;
  }
}
