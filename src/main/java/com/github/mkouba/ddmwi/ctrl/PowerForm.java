package com.github.mkouba.ddmwi.ctrl;

import org.jboss.resteasy.reactive.RestForm;

import com.github.mkouba.ddmwi.CreaturePower;
import com.github.mkouba.ddmwi.CreaturePower.PowerType;

import io.smallrye.mutiny.Uni;

public class PowerForm extends Form<CreaturePower> {

    @RestForm
    public PowerType type;

    @RestForm
    public String text;

    @RestForm
    public String limit;

    protected Uni<CreaturePower> apply(CreaturePower power) {
        power.type = type;
        power.text = text;
        power.usageLimit = limit != null && !limit.isEmpty() ? Integer.parseInt(limit) : null;
        return Uni.createFrom().item(power);
    }

}
