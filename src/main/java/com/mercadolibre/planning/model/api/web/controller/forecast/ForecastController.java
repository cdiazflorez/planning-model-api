package com.mercadolibre.planning.model.api.web.controller.forecast;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastOutput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.remove.DeleteForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.remove.DeleteForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationInput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.MetricUnitEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessingTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.CreateForecastRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.MetadataRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.ProcessingDistributionDataRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.ProcessingDistributionRequest;
import com.newrelic.api.agent.Trace;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/forecasts")
public class ForecastController {

    private final CreateForecastUseCase createForecastUseCase;

    private final DeleteForecastUseCase deleteForecastUseCase;

    private final DeactivateSimulationUseCase deactivateSimulationUseCase;

    @PostMapping
    @Trace(dispatcher = true)
    public ResponseEntity<CreateForecastResponse> createForecast(
            @PathVariable final Workflow workflow,
            @Valid @RequestBody final CreateForecastRequest createForecastRequest) {

        final CreateForecastInput input = createForecastRequest.toCreateForecastInput(workflow);

        final CreateForecastOutput output = createForecastUseCase.execute(input);

        deactivateSimulationUseCase.deactivateSimulation(buildDeactivateSimulationInputs(input));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateForecastResponse(output.getId()));
    }

    @PostMapping("/purge/weeks/{weeks}")
    public ResponseEntity<DeleteForecastResponse> deleteOldForecasts(
            @PathVariable final Workflow workflow,
            @PathVariable final Integer weeks,
            @RequestParam final Integer limit) {

        final DeleteForecastInput input = new DeleteForecastInput(workflow, weeks, limit);

        final Integer updatedRows = deleteForecastUseCase.execute(input);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new DeleteForecastResponse(updatedRows));
    }

    private List<DeactivateSimulationInput> buildDeactivateSimulationInputs(final CreateForecastInput input) {

        final String logisticCenterId = input.getMetadata().stream()
                .filter(metadataRequest -> metadataRequest.getKey().equals("warehouse_id"))
                .map(MetadataRequest::getValue)
                .findFirst().orElseThrow();

        final var groupingByDateAndByProcessName = input.getProcessingDistributions().stream()
                .collect(
                        toMap(
                                ProcessingDistributionRequest::getProcessName,
                                processDistribution -> processDistribution.getData().stream()
                                        .map(ProcessingDistributionDataRequest::getDate)
                                        .distinct()
                                        .collect(toList())
                        )
                );

        return groupingByDateAndByProcessName.entrySet().stream()
                .map(group -> new DeactivateSimulationInput(
                        logisticCenterId,
                        input.getWorkflow(),
                        group.getKey(),
                        group.getValue(),
                        input.getUserId()
                        )
                ).collect(toList());


    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(MetricUnit.class, new MetricUnitEditor());
        dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
        dataBinder.registerCustomEditor(ProcessingType.class, new ProcessingTypeEditor());
    }
}
