package com.mercadolibre.planning.model.api.web.consumer;

import static com.mercadolibre.planning.model.api.web.consumer.ConsumerMessageDTO.ProcessingTimeToUpdate;

import com.mercadolibre.planning.model.api.domain.service.configuration.DayAndHourProcessingTime;
import com.mercadolibre.planning.model.api.domain.service.configuration.ProcessingTimeService;
import com.newrelic.api.agent.Trace;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/consumer/processing_time")
public class ProcessingTimeEventController {

  private static final String PROCESSING_TIME_PREFIX = "[processing_time_event] ";

  private static final String START_MESSAGE = "the processing time update is started with the following values: %s";

  private static final String INVALID_EVENT_TYPE_MESSAGE = "Event %s is not valid to run the process, only %s is allowed.";

  private static final String NO_INFORMATION_MESSAGE = "no value returned from the use case for warehouse %s.";

  private static final String SUCCESS_MESSAGE = "Update of warehouse %s successfully performed, new data are %s";

  private static final String EVENT_TYPE_ALLOWED = Arrays.toString(EventType.values());

  private static final int NO_CONTENT_RESPONSE = 204;

  private static final int OK_RESPONSE = 200;

  private ProcessingTimeService outboundProcessingTimeService;

  @PostMapping("/event")
  @Trace(dispatcher = true)
  public ResponseEntity<ProcessingTimeConsumerResponse> saveEvent(@RequestBody @Valid final ConsumerMessageDTO request) {
    final ProcessingTimeConsumerResponse response = updateProcessingTime(request.message());

    return ResponseEntity.status(response.status()).body(response);
  }

  private ProcessingTimeConsumerResponse updateProcessingTime(final ProcessingTimeToUpdate processingTimeToUpdate) {

    log.info(PROCESSING_TIME_PREFIX.concat(START_MESSAGE), processingTimeToUpdate);

    if (!EventType.exists(processingTimeToUpdate.eventType())) {
      log.info(PROCESSING_TIME_PREFIX.concat(INVALID_EVENT_TYPE_MESSAGE), processingTimeToUpdate.eventType(), EVENT_TYPE_ALLOWED);
      return new ProcessingTimeConsumerResponse(
          NO_CONTENT_RESPONSE,
          String.format(PROCESSING_TIME_PREFIX.concat(INVALID_EVENT_TYPE_MESSAGE), processingTimeToUpdate.eventType(), EVENT_TYPE_ALLOWED)
      );
    }

    final List<DayAndHourProcessingTime> outboundProcessingTimes =
        outboundProcessingTimeService.updateProcessingTimeForCptsByLogisticCenter(processingTimeToUpdate.logisticCenterId());

    if (outboundProcessingTimes.isEmpty()) {
      log.info(PROCESSING_TIME_PREFIX.concat(NO_INFORMATION_MESSAGE), processingTimeToUpdate.logisticCenterId());
      return new ProcessingTimeConsumerResponse(
          NO_CONTENT_RESPONSE,
          String.format(PROCESSING_TIME_PREFIX.concat(NO_INFORMATION_MESSAGE), processingTimeToUpdate.logisticCenterId())
      );
    }

    log.info(PROCESSING_TIME_PREFIX.concat(SUCCESS_MESSAGE), processingTimeToUpdate.logisticCenterId(), outboundProcessingTimes);

    return new ProcessingTimeConsumerResponse(
        OK_RESPONSE,
        String.format(PROCESSING_TIME_PREFIX.concat(SUCCESS_MESSAGE), processingTimeToUpdate.logisticCenterId(), outboundProcessingTimes)
    );
  }

  private enum EventType {
    CREATE_ETS,
    DELETE_ETS,
    UPDATE_ETS;

    private static boolean exists(final String value) {
      return Arrays.stream(values())
          .anyMatch(event -> event.name()
              .replace("_", "")
              .toLowerCase(Locale.ROOT)
              .equals(value.toLowerCase(Locale.ROOT))
          );
    }
  }
}
