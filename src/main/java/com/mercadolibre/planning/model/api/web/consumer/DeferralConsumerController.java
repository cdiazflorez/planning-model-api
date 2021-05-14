package com.mercadolibre.planning.model.api.web.consumer;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralCptUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralInput;
import com.mercadolibre.planning.model.api.web.consumer.request.BigQMessageContainer;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
@RequestMapping("/feed/deferral")
public class DeferralConsumerController {

    private final DeferralCptUseCase deferralCptUseCase;

    @PostMapping
    @Trace(dispatcher = true)
    public ResponseEntity consume(@Valid @RequestBody final BigQMessageContainer message) {

        deferralCptUseCase.execute(new DeferralInput(
                message.getMsg().getWarehouseId(),
                Workflow.FBM_WMS_OUTBOUND)
        );

        return ResponseEntity.ok(String.format("Message processed for warehouseId: %s",
                message.getMsg().getWarehouseId())
        );
    }

}
