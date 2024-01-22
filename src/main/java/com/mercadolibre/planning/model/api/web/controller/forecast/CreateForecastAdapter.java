package com.mercadolibre.planning.model.api.web.controller.forecast;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.forecast.dto.CreateForecastInputDto;
import com.mercadolibre.planning.model.api.web.controller.forecast.dto.StaffingPlanDto;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.CreateForecastRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.HeadcountProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.PolyvalentProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.ProcessingDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.TagsBuilder;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Adapter to create the input for the use case {@link CreateForecastUseCase}.
 * Este es un adaptador temporal, nos sirve para mapear el request(conocido) que llega desde Planning-model-me
 * una vez entre la iniciativa para obtener el plan de staffing desde S&OP se debe eliminar este adaptador y
 * pasar el DTO directamente al caso de uso.
 */
public final class CreateForecastAdapter {

  private static final Integer ONE_HUNDRED_PERCENT = 100;
  private static final String POLYVALENCE_KEY = "polyvalence";

  private CreateForecastAdapter() {}


  public static CreateForecastInputDto createStaffingPlan(
      final Workflow workflow,
      final CreateForecastRequest input
  ) {
    return new CreateForecastInputDto(
        input.getWeek(),
        input.getUserId(),
        workflow,
        input.getLogisticCenterId(),
        input.getMetadata(),
        input.getHeadcountDistributions(),
        createStaffingPlanDto(input, workflow),
        input.getPlanningDistributions()
    );
  }

  private static List<StaffingPlanDto> createStaffingPlanDto(
      final CreateForecastRequest request,
      final Workflow workflow
  ) {
    final List<ProcessingDistributionRequest> processingDistributions = new ArrayList<>();
    if (request.getProcessingDistributions() != null) {
      processingDistributions.addAll(request.getProcessingDistributions());
    }
    if (request.getBacklogLimits() != null) {
      processingDistributions.addAll(request.getBacklogLimits());
    }

    final var staffingPlan = processingDistributions.stream()
        .flatMap(pd -> processingDistributionToStaffingPlan(workflow, pd));
    final var productivity = getProductivityFromHeadcountProductivity(
        workflow,
        request.getHeadcountProductivities(),
        request.getPolyvalentProductivities()
    );

    return Stream.concat(staffingPlan, productivity).toList();
  }

  private static Stream<StaffingPlanDto> processingDistributionToStaffingPlan(
      final Workflow workflow,
      final ProcessingDistributionRequest pd
  ) {
    return pd.getData().stream().map(data -> new StaffingPlanDto(
        pd.getType(),
        buildTags(data.getDate(), workflow, pd.getType(), pd),
        pd.getQuantityMetricUnit(),
        data.getQuantity())
    );
  }

  private static Stream<StaffingPlanDto> getProductivityFromHeadcountProductivity(
      final Workflow workflow,
      final List<HeadcountProductivityRequest> productivities,
      final List<PolyvalentProductivityRequest> polyvalentProductivities
  ) {
    if (productivities == null) {
      return Stream.empty();
    }
    return productivities.stream()
        .flatMap(pr -> Stream.concat(
            regularProductivityToStaffingPlan(workflow, pr),
            polyvalentProductivityToStaffingPlan(workflow, pr, polyvalentProductivities)
        )).distinct();
  }

  private static Stream<StaffingPlanDto> polyvalentProductivityToStaffingPlan(
      final Workflow workflow,
      final HeadcountProductivityRequest productivity,
      final List<PolyvalentProductivityRequest> polyvalentProductivities
  ) {
    return productivity.getData().stream()
        .map(data -> {
          final var polyvalentProductivity = polyvalentProductivities.stream()
              .filter(pp -> pp.getProcessName() == productivity.getProcessName())
              .findFirst();
          if (polyvalentProductivity.isPresent()) {
            final var tags = buildTags(data.getDayTime(), workflow, ProcessingType.PRODUCTIVITY, productivity);
            tags.put(POLYVALENCE_KEY, String.valueOf(polyvalentProductivity.get().getAbilityLevel()));
            return new StaffingPlanDto(
                ProcessingType.PRODUCTIVITY,
                tags,
                productivity.getProductivityMetricUnit(),
                (double) (data.getProductivity() * polyvalentProductivity.get().getProductivity()) / ONE_HUNDRED_PERCENT
            );
          }
          return null;
        }).filter(Objects::nonNull);
  }

  private static Stream<StaffingPlanDto> regularProductivityToStaffingPlan(
      final Workflow workflow,
      final HeadcountProductivityRequest productivity
  ) {
    return productivity.getData().stream()
        .map(data -> new StaffingPlanDto(
            productivity.getType(),
            buildTags(data.getDayTime(), workflow, productivity.getType(), productivity),
            productivity.getProductivityMetricUnit(),
            data.getProductivity())
        );
  }


  private static Map<String, String> buildTags(
      final ZonedDateTime date,
      final Workflow workflow,
      final ProcessingType type,
      final TagsBuilder staffing
  ) {
    final var tags = Stream.of(Tags.values())
        .filter(tag -> tag.isTagRequired(workflow, type))
        .collect(Collectors.toMap(
                tag -> tag.name().toLowerCase(Locale.ROOT),
                tag -> tag.valueGetter.apply(staffing)
            )
        );
    tags.put("date", date.toString());
    return tags;
  }

  @Getter
  @AllArgsConstructor
  public enum Tags {
    PROCESS(
        Tags::getProcess,
        Set.of(Workflow.values()),
        Stream.of(ProcessingType.values()).filter(pt -> pt != ProcessingType.MAX_CAPACITY).toList()
    ),
    PROCESS_PATH(
        Tags::getProcessPath,
        Set.of(Workflow.FBM_WMS_OUTBOUND),
        List.of(ProcessingType.EFFECTIVE_WORKERS, ProcessingType.EFFECTIVE_WORKERS_NS, ProcessingType.PRODUCTIVITY,
            ProcessingType.THROUGHPUT)
    ),
    HEADCOUNT_TYPE(
        Tags::getHeadcountType,
        Set.of(Workflow.FBM_WMS_INBOUND, Workflow.FBM_WMS_OUTBOUND),
        List.of(ProcessingType.EFFECTIVE_WORKERS, ProcessingType.EFFECTIVE_WORKERS_NS)
    ),
    POLYVALENCE(
        Tags::getPolyvalence,
        Set.of(Workflow.FBM_WMS_INBOUND, Workflow.FBM_WMS_OUTBOUND),
        List.of(ProcessingType.PRODUCTIVITY, ProcessingType.THROUGHPUT)
    );

    private final Function<TagsBuilder, String> valueGetter;
    private final Set<Workflow> availableWorkflows;
    private final List<ProcessingType> availableTypes;

    private static String getProcess(TagsBuilder staffing) {
      return staffing.getProcessName().toJson();
    }

    private static String getProcessPath(TagsBuilder staffing) {
      return staffing.getProcessPath().toJson();
    }

    private static String getHeadcountType(TagsBuilder staffing) {
      return staffing.getHeadcountType();
    }

    private static String getPolyvalence(TagsBuilder staffing) {
      return String.valueOf(staffing.getAbilityLevel());
    }

    public boolean isTagRequired(final Workflow workflow, final ProcessingType type) {
      return availableWorkflows.contains(workflow)
          && availableTypes.contains(type);
    }
  }
}
