package com.mercadolibre.planning.model.api.client.db.repository.current;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanUseCase.CreateSimulationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanUseCase.CurrentProcessingDistributionGateway;
import com.mercadolibre.planning.model.api.exception.TagsParsingException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class CurrentProcessingDistributionImpl implements CurrentProcessingDistributionGateway {

  private static final String PROCESS_KEY = "process";
  private static final String PROCESS_PATH_KEY = "process_path";

  private final CurrentProcessingDistributionRepository currentProcessingDistributionRepository;
  private final ObjectMapper objectMapper;

  @Override
  public void deactivateStaffingUpdate(
      final Workflow workflow,
      final String logisticCenterId,
      final ZonedDateTime date,
      final Map<String, String> tags,
      final long userId,
      final ProcessingType type,
      final MetricUnit metricUnit
  ) {
    currentProcessingDistributionRepository.deactivateProcessingDistributionByTags(
        workflow,
        date,
        logisticCenterId,
        getTagsAsString(tags),
        type,
        metricUnit,
        userId
    );
  }


  @Override
  public void createStaffingUpdates(final List<CreateSimulationInput> input) {
    final List<CurrentProcessingDistribution> distributions = input.stream()
        .map(this::toCurrentProcessingDistribution)
        .toList();
    currentProcessingDistributionRepository.saveAll(distributions);
  }

  private CurrentProcessingDistribution toCurrentProcessingDistribution(
      final CreateSimulationInput input
  ) {
    final Map<String, String> tags = input.tags() == null ? Map.of() : input.tags();
    return CurrentProcessingDistribution.builder()
        .workflow(input.workflow())
        //Todo: delete line bellow when column {processName} will deleted
        .processName(
            tags.get(PROCESS_KEY) == null
                ? ProcessName.GLOBAL
                : ProcessName.of(tags.get(PROCESS_KEY)).orElse(ProcessName.GLOBAL)
        )
        //Todo: delete line bellow when column {processPath} will deleted
        .processPath(
            tags.get(PROCESS_PATH_KEY) == null
                ? GLOBAL
                : ProcessPath.of(tags.get(PROCESS_PATH_KEY)).orElse(GLOBAL)
        )
        .date(input.date())
        .quantity(input.quantity())
        .logisticCenterId(input.logisticCenterId())
        .userId(input.userId())
        .type(input.type().getProcessingType())
        .quantityMetricUnit(input.type().getMetricUnit())
        .tags(getTagsAsString(tags))
        .isActive(true)
        .build();
  }

  private String getTagsAsString(final Map<String, String> tags) {
    try {
      //NOTE: object mapper config is used to guarantee the order of the tags when parsing them to JSON
      objectMapper.setConfig(objectMapper.getSerializationConfig().with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
      return objectMapper.writeValueAsString(tags);
    } catch (JsonProcessingException e) {
      log.error("Error staffing plan update tags. {}", e.getMessage());
      throw new TagsParsingException(tags, e);
    }
  }
}
