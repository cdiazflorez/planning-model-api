package com.mercadolibre.planning.model.api.web.controller.unitsdistribution;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.metrics.UnitsDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.UnitsDistributionService;
import com.mercadolibre.planning.model.api.web.controller.unitsdistibution.UnitsDistributionController;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = UnitsDistributionController.class)
public class UnitsDistributionControllerTest {

  private static final String URL = "/planning/model/workflows/FBM_WMS_OUTBOUND/units_distribution";

  private static final String AREA = "MZ-01";

  private static final String WH = "ARBA01";

  private static final Double QUANTITY = 0.2;

  private static final ZonedDateTime DATE = ZonedDateTime.of(2022, 4, 3, 10, 15, 30, 0, ZoneOffset.UTC);

  private static final ZonedDateTime DATE2 = ZonedDateTime.of(2022, 4, 3, 11, 15, 30, 0, ZoneOffset.UTC);

  @Autowired
  private MockMvc mvc;

  @MockBean
  private UnitsDistributionService unitsDistributionService;

  @Test
  public void saveTest() throws Exception {

    //GIVEN

    when(unitsDistributionService.save(any())).thenReturn(mockUnitsDistributions());

    //WHEN
    final ResultActions result = mvc.perform(
        post(URL )
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_units_distribution_request.json"))
    );

    // THEN
    result.andExpect(status().isOk());
  }

  @Test
  public void getTest() throws Exception {

    //GIVEN
    when(unitsDistributionService.get(any())).thenReturn(mockUnitsDistributions());

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL )
            .contentType(APPLICATION_JSON)
            .param("warehouse_id", "ARBA01")
            .param("date_from", DATE.toString())
            .param("date_to", DATE2.toString())

    );

    // THEN
    result.andExpect(status().isOk());
  }

  private List<UnitsDistribution> mockUnitsDistributions() {
    return List.of(new UnitsDistribution(null, WH, DATE2, ProcessName.PICKING, AREA, QUANTITY, MetricUnit.PERCENTAGE, Workflow.FBM_WMS_OUTBOUND));
  }

}
