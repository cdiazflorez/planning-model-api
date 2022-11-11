package com.mercadolibre.planning.model.api.web.controller.forecast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessPathEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/logistic_center/{logisticCenter}/plan/units")
public class PlannedUnitsController {

  private ObjectMapper objectMapper;

  //TODO: Eliminar cuando se llame al servicio
  private static String getResourceAsString() throws IOException {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    try (InputStream resource = classLoader.getResourceAsStream("mockForecastPP.json")) {
      assert resource != null;
      return IOUtils.toString(resource, StandardCharsets.UTF_8);
    }

  }

  @GetMapping
  public ResponseEntity<List<PlannedUnitsResponse>> getForecast(
      @PathVariable final String logisticCenter,
      @RequestParam final Workflow workflow,
      @RequestParam(required = false) List<ProcessPath> processPaths,
      @RequestParam(required = false) final Instant dateInFrom,
      @RequestParam(required = false) final Instant dateInTo,
      @RequestParam(required = false) final Instant dateOutFrom,
      @RequestParam(required = false) final Instant dateOutTo,
      @RequestParam final Instant viewDate,
      @RequestParam final List<String> groupBy
  ) throws IOException, MissingServletRequestParameterException {

    validateDatesRanges(dateInFrom, dateInTo, dateOutFrom, dateOutTo);

    List<PlannedUnitsResponse> response = objectMapper.treeToValue(
        objectMapper.readTree(getResourceAsString()),
        objectMapper.getTypeFactory().constructCollectionType(List.class, PlannedUnitsResponse.class));


    return ResponseEntity.ok(response);
  }

  private void validateDatesRanges(final Instant dateInFrom,
                                   final Instant dateInTo,
                                   final Instant dateOutFrom,
                                   final Instant dateOutTo) throws MissingServletRequestParameterException {
    if (!((dateInFrom != null && dateInTo != null) || (dateOutFrom != null && dateOutTo != null))) {
      final String message = String.format(
          "The range of dates of entry or exit is not present. dateInFrom: %s, dateInTo: %s, dateOutFrom: %s, dateOutTo: %s",
          dateInFrom,
          dateInTo,
          dateOutFrom,
          dateOutTo);
      throw new MissingServletRequestParameterException(message, Instant.class.getTypeName());
    }
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(ProcessPath.class, new ProcessPathEditor());
  }


}
