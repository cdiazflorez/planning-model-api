package com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FixedEtsByDayDto {

    private List<DayDto> sunday;

    private List<DayDto> monday;

    private List<DayDto> tuesday;

    private List<DayDto> wednesday;

    private List<DayDto> thursday;

    private List<DayDto> friday;

    private List<DayDto> saturday;
}
