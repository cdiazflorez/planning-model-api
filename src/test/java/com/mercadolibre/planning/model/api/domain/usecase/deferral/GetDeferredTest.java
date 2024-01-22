package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetDeferredTest {
  private static final Instant VIEW_DATE = Instant.parse("2023-09-26T10:00:00Z");

  private static final Instant DATE_OUT_1 = Instant.parse("2023-09-27T10:00:00Z");

  private static final Instant DATE_OUT_2 = Instant.parse("2023-09-27T11:00:00Z");

  @InjectMocks
  private GetDeferred getDeferred;

  @Mock
  private GetDeferred.DeferredGateway gateway;

  public static Stream<Arguments> params() {
    return Stream.of(
        Arguments.of(
            List.of(
                new GetDeferred.DeferralStatus(DATE_OUT_1, DeferralType.CASCADE),
                new GetDeferred.DeferralStatus(DATE_OUT_2, DeferralType.CAP_MAX),
                new GetDeferred.DeferralStatus(VIEW_DATE.minus(1, ChronoUnit.HOURS), DeferralType.CAP_MAX)
            ),
            List.of(
                new GetDeferred.DeferralStatus(DATE_OUT_1, DeferralType.CASCADE),
                new GetDeferred.DeferralStatus(DATE_OUT_2, DeferralType.CAP_MAX)
            )
        ),
        Arguments.of(
            emptyList(),
            emptyList()
        )
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  void test(final List<GetDeferred.DeferralStatus> mock, final List<GetDeferred.DeferralStatus> expected) {
    // GIVEN
    when(gateway.getDeferredWithStatus("ARTW01", Workflow.FBM_WMS_OUTBOUND, VIEW_DATE)).thenReturn(mock);

    // WHEN
    final var actual = getDeferred.getDeferred("ARTW01", Workflow.FBM_WMS_OUTBOUND, VIEW_DATE);

    // THEN
    assertEquals(expected, actual);
  }
}
