package com.mercadolibre.planning.model.api.usecase.strategy;

import com.mercadolibre.planning.model.api.domain.usecase.projection.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.strategy.CalculateProjectionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static com.mercadolibre.planning.model.api.web.controller.request.ProjectionType.CPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CalculateProjectionStrategyTest {

    @InjectMocks
    private CalculateProjectionStrategy strategy;

    @Mock
    private CalculateCptProjectionUseCase cptProjectionUseCase;

    @BeforeEach
    public void setUp() {
        strategy = new CalculateProjectionStrategy(Set.of(cptProjectionUseCase));
    }

    @Test
    public void testCalculateCptProjectionOk() {
        // GIVEN
        when(cptProjectionUseCase.supportsProjectionType(CPT)).thenReturn(true);

        // WHEN - THEN
        assertThat(strategy.getBy(CPT).get()).isInstanceOf(CalculateCptProjectionUseCase.class);
    }
}
