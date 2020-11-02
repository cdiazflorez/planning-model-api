package com.mercadolibre.planning.model.api.domain.usecase.projection;

import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.web.controller.request.ProjectionType;

import java.util.List;

public interface CalculateProjectionUseCase
        extends UseCase<ProjectionInput, List<ProjectionOutput>> {

    boolean supportsProjectionType(ProjectionType projectionType);
}
