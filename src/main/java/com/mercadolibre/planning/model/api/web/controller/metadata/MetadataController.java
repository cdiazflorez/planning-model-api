package com.mercadolibre.planning.model.api.web.controller.metadata;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.MetricUnitEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/metadata")
public class MetadataController {

    private final GetForecastMetadataUseCase getForecastMetadataUseCase;
    private final GetForecastUseCase getForecastUseCase;

    @GetMapping
    public ResponseEntity<List<ForecastMetadataView>> getLastHistoricForecast(
            @PathVariable final Workflow workflow,
            @Valid final GetForecastMetadataRequest request) {
        final List<Long> forecastIds = getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(workflow)
                .warehouseId(request.getWarehouseId())
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .build());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(getForecastMetadataUseCase.execute(
                        GetForecastMetadataInput.builder()
                                .forecastIds(forecastIds)
                                .dateFrom(request.getDateFrom())
                                .dateTo(request.getDateTo())
                                .build()
                        )
                );
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(MetricUnit.class, new MetricUnitEditor());
    }
}
