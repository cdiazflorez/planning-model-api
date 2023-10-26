package com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.projection.waverless.Wave;
import com.mercadolibre.planning.model.api.projection.waverless.WavesCalculator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;


public final class ResponseMapper {

  private static final int FIRST_CONDITION = 2;
  private static final int SECOND_CONDITION = 4;
  private static final int THIRD_CONDITION = 5;
  private static final int SECOND_LIMIT = 2;
  private static final int THIRD_LIMIT = 3;
  private static final int SECONDS_TO_HOUR = 3600;
  private static final int MAXIMUM_SLA_AMOUNT = 1;

  private ResponseMapper() {
  }

  public static WaverlessResponse mapToDto(final String logisticCenterId, final Instant viewDate,
                                           final WavesCalculator.TriggerProjection triggers) {
    final var suggestions = triggers.getWaves().stream()
        .map(wave -> new WaveDto(wave.getDate(), getWaveConfigurations(wave), wave.getReason()))
        .toList();

    final var projectedBacklogs = triggers.getProjectedBacklogs()
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue()
                    .entrySet()
                    .stream()
                    .map(unitsByDate -> new WaverlessResponse.UnitsAtOperationHour(unitsByDate.getKey(), unitsByDate.getValue()))
                    .sorted(Comparator.comparing(WaverlessResponse.UnitsAtOperationHour::getDate))
                    .toList()
            )
        );

    return new WaverlessResponse(logisticCenterId, viewDate, suggestions, projectedBacklogs);
  }

  private static List<WaveConfigurationDto> getWaveConfigurations(final Wave wave) {
    return wave.getReason().equals(TriggerName.IDLENESS)
        ? toIdlenessWaves(wave)
        : toSLAWaves(wave);
  }

  private static List<WaveConfigurationDto> toIdlenessWaves(final Wave wave) {
    return wave.getConfiguration().entrySet()
        .stream()
        .map(conf -> getWaveConfiguration(conf.getKey(), conf.getValue()))
        .toList();
  }

  private static List<WaveConfigurationDto> toSLAWaves(final Wave wave) {
    final List<WaveConfigurationDto> waves = new ArrayList<>();

    wave.getConfiguration().forEach((key, value) -> {
      var slasQuantity = value.getWavedUnitsByCpt().size();
      if (slasQuantity == MAXIMUM_SLA_AMOUNT) {
        waves.add(getWaveConfiguration(key, value));
      } else {
        waves.addAll(getWaveConfigurationDtoAtSla(wave));
      }
    });

    return waves;
  }

  private static WaveConfigurationDto getWaveConfiguration(final ProcessPath processPath, final Wave.WaveConfiguration configuration) {
    return new WaveConfigurationDto(
        processPath,
        (int) configuration.getLowerBound(),
        (int) configuration.getUpperBound(),
        new TreeSet<>(configuration.getWavedUnitsByCpt().keySet()),
        configuration.getWavedUnitsByCpt()
            .entrySet()
            .stream()
            .map(entry -> new WaveConfigurationDto.UnitsAtSla(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(WaveConfigurationDto.UnitsAtSla::getSla))
            .toList()
    );
  }

  private static List<WaveConfigurationDto> getWaveConfigurationDtoAtSla(final Wave wave) {
    return wave.getConfiguration().entrySet().stream().flatMap(configurationEntry -> {
      var waveConfiguration = configurationEntry.getValue();
      final List<List<Instant>> split = splitSLAs(waveConfiguration.getWavedUnitsByCpt().keySet().stream().toList(), wave.getDate());
      return split.stream().map(list -> {
        var lower = list.stream().mapToLong(sla -> waveConfiguration.getWavedUnitsByCpt().get(sla)).sum();
        var unitsByCpt = list.stream()
            .map(sla -> new WaveConfigurationDto.UnitsAtSla(sla, waveConfiguration.getWavedUnitsByCpt().get(sla)))
            .toList();

        return new WaveConfigurationDto(configurationEntry.getKey(), (int) lower, (int) waveConfiguration.getUpperBound(),
            new TreeSet<>(list), unitsByCpt);
      });
    }).toList();
  }

  private static List<List<Instant>> splitSLAs(final List<Instant> slas, final Instant initialDate) {

    final TreeSet<Instant> orderedSlas = new TreeSet<>(slas);
    final List<List<Instant>> parentList = new ArrayList<>();

    List<Instant> subList = new ArrayList<>();
    boolean limitTwo = false;
    boolean limitThree = false;

    for (Instant sla : orderedSlas) {
      double diffSeconds = sla.getEpochSecond() - initialDate.getEpochSecond();
      double diffHours = diffSeconds / (SECONDS_TO_HOUR);

      if (diffHours < FIRST_CONDITION) {
        parentList.add(List.of(sla));
      } else if (diffHours <= SECOND_CONDITION) {
        limitTwo = true;
        if (subList.size() < SECOND_LIMIT) {
          subList.add(sla);
        } else {
          parentList.add(subList);
          subList = new ArrayList<>();
          subList.add(sla);
        }
      } else if (diffHours <= THIRD_CONDITION) {
        limitThree = true;
        if (subList.size() < THIRD_LIMIT) {
          if (limitTwo && subList.size() == SECOND_LIMIT) {
            limitTwo = false;
            parentList.add(subList);
            subList = new ArrayList<>();
          }
          subList.add(sla);
        } else {
          parentList.add(subList);
          subList = new ArrayList<>();
          subList.add(sla);
        }
      } else {
        if ((limitTwo && subList.size() == SECOND_LIMIT) || (limitThree && subList.size() == THIRD_LIMIT)) {
          parentList.add(subList);
          subList = new ArrayList<>();
          limitTwo = false;
          limitThree = false;
        }
        subList.add(sla);
      }
    }

    parentList.add(subList);

    return parentList;

  }

}
