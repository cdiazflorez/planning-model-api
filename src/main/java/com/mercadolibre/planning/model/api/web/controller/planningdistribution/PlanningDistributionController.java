package com.mercadolibre.planning.model.api.web.controller.planningdistribution;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.web.controller.editor.MetricUnitEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.newrelic.api.agent.Trace;
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
@RequestMapping("/planning/model/workflows/{workflow}/planning_distributions")
public class PlanningDistributionController {

    private final PlanningDistributionService planningDistributionService;

    @GetMapping
    @Trace(dispatcher = true)
    public ResponseEntity<List<GetPlanningDistributionOutput>> getPlanningDist(
            @PathVariable final Workflow workflow,
            @Valid final GetPlanningDistributionRequest request) {

        final GetPlanningDistributionInput input = request.toGetPlanningDistInput(workflow);
        return ResponseEntity.status(HttpStatus.OK).body(planningDistributionService.getPlanningDistribution(input));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(MetricUnit.class, new MetricUnitEditor());
    }
}
