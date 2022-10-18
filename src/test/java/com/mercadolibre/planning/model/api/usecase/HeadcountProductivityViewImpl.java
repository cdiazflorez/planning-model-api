package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import lombok.AllArgsConstructor;

import java.util.Date;
import org.apache.tomcat.jni.Proc;

@AllArgsConstructor
public class HeadcountProductivityViewImpl implements HeadcountProductivityView {

    private ProcessName processName;

    private long productivity;

    private MetricUnit productivityMetricUnit;

    private Date date;

    private int abilityLevel;

    private ProcessPath processPath;


    @Override
    public ProcessName getProcessName() {
        return processName;
    }

    @Override
    public long getProductivity() {
        return productivity;
    }

    @Override
    public MetricUnit getProductivityMetricUnit() {
        return productivityMetricUnit;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public int getAbilityLevel() {
        return abilityLevel;
    }

    @Override
    public ProcessPath getProcessPath() {
        return processPath;
    }
}
