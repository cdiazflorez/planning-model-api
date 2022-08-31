package com.mercadolibre.planning.model.api.web.controller.metadata;


import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

import com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetPolyvalenceForecastMetadata;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/metadata")
public class PolyvalenceMetadataController {

  private final GetPolyvalenceForecastMetadata polyvalenceMetadata;

  @GetMapping("/polyvalences")
  public ResponseEntity<Map<ProductivityPolyvalenceCardinality, Float>> getPolyvalenceMetadata(@PathVariable final Workflow workflow,
                                                                                               @RequestParam final String warehouseId,
                                                                                               @RequestParam
                                                                                               @DateTimeFormat(iso = DATE_TIME)
                                                                                               final ZonedDateTime dateTime
  ) {


    return ResponseEntity
        .status(HttpStatus.OK)
        .body(polyvalenceMetadata.getPolyvalencePercentage(warehouseId, workflow, dateTime).getPolyvalences());

  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
  }
}
