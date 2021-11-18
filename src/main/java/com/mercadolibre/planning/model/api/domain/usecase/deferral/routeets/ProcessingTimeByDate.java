package com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets;

import lombok.Value;

import java.time.DayOfWeek;

@Value
public class ProcessingTimeByDate {

    private DayOfWeek etDay;

    private int hour;

    private int minutes;

    private int processingTime;

    private String type;
}
