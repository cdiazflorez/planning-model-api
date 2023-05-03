package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author (Andrés M. Barragán) anbarragan
 */
public final class LowerBoundCalculator {

  private LowerBoundCalculator() {
    // Empty
  }

  /**
   * <p>Se calcula el Lowe Bound de Picking con X cantidad de minutos de backlogs, teniendo en cuenta que puede ser necesario utilizar
   * el TPH de Picking de más de una hora y que en ese caso se debe dividir proporcionalmente.</p>
   *
   * <p>Ejemplos:</p>
   *
   * <p>90 minutos: 3:50 -> 5:20</P>
   * <ul>
   * <li>minutesToWave = 10, remaining = 90
   * <li>minutesToWave = 60, remaining = 80
   * <li>minutesToWave = 20, remaining = 20
   * </ul>
   *
   * <p>30 minutos: 3:40 -> 4:10</p>
   * <ul>
   * <li>minutesToWave = 20, remaining = 30
   * <li>minutesToWave = 10, remaining = 10
   * </ul>
   *
   * <p>30 minutos: 3:10 -> 3:40</p>
   * <ul>
   * <li>minutesToWave = 30, remaining = 30
   * </ul>
   *
   * @param minutesToProcess Cantidad de minutos a partir de los cuales se requiere calcular el Lower Bound
   * @param waveExecutionDate Instant a partir del cual se requiere calcular lower bound por cada Process Path
   * @param tphByInstant Mapa de Tphs mapeados por Instant
   * @return El cálculo del Lower Bound para un determinado Process Path
   */
  private static Integer calculateLowerBound(
      final Integer minutesToProcess, final Instant waveExecutionDate, final Map<Instant, Integer> tphByInstant) {
    Instant waveExecutionHourIni = waveExecutionDate.truncatedTo(ChronoUnit.HOURS);
    Instant waveExecutionHourEnd = waveExecutionDate.plus(minutesToProcess, ChronoUnit.MINUTES);

    LocalDateTime rangeIni = LocalDateTime.ofInstant(waveExecutionHourIni, ZoneOffset.UTC);
    LocalDateTime rangeEnd = LocalDateTime.ofInstant(waveExecutionHourEnd, ZoneOffset.UTC);

    final int m60 = 60;
    Instant instantToFindTph = waveExecutionHourIni;
    int waveExecutionDateMinutes = waveExecutionDate.atZone(ZoneOffset.UTC).getMinute();
    int remainingMinutes = minutesToProcess;
    int minutesToWave = (m60 - waveExecutionDateMinutes) > minutesToProcess ? minutesToProcess : (m60 - waveExecutionDateMinutes);
    int tphTotal = 0;

    for (LocalDateTime index = rangeIni; index.isBefore(rangeEnd); index = index.plusHours(1)) {
      int currentHourTph = tphByInstant.getOrDefault(instantToFindTph, 0);

      tphTotal += currentHourTph * minutesToWave / m60;

      instantToFindTph = instantToFindTph.plus(1L, ChronoUnit.HOURS);
      remainingMinutes = remainingMinutes - minutesToWave;
      if (remainingMinutes > m60) {
        minutesToWave = m60;
      } else {
        minutesToWave = remainingMinutes;
      }
    }

    return tphTotal;
  }

  /**
   * Método que calcula el lower bound de sugerencia de waves por ociosidad.
   *
   * @param minutesToProcess Cantidad de minutos a partir de los cuales se requiere calcular el Lower Bound
   * @param waveExecutionDate Instant a partir del cual se requiere calcular lower bound por cada Process Path
   * @param throughputByProcessPath Mapa de Tphs mapeados por Instant, ProcessName y ProcessPath
   * @return Mapa de Lower bounds mapeados por Process Path
   */
  public static Map<ProcessPath, Integer> lowerBounds(
      final Integer minutesToProcess,
      final Instant waveExecutionDate,
      final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> throughputByProcessPath) {

    return throughputByProcessPath.entrySet().stream()
        .filter(entry -> entry.getKey() != ProcessPath.GLOBAL)
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> Optional.of(entry)
                    .map(Map.Entry::getValue)
                    .map(tphByProcess -> tphByProcess.get(ProcessName.PICKING))
                    .map(tph -> calculateLowerBound(minutesToProcess, waveExecutionDate, tph))
                    .orElse(0)
            )
        );
  }

}
