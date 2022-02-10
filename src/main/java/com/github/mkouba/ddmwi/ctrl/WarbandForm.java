package com.github.mkouba.ddmwi.ctrl;

import org.jboss.resteasy.reactive.RestForm;

import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.Warband.PointLimit;

import io.quarkus.qute.Qute;
import io.smallrye.mutiny.Uni;

public class WarbandForm extends Form<Warband> {

    @RestForm
    public String name;

    @RestForm
    public boolean arena;

    @RestForm
    public PointLimit pointLimit;

    @RestForm
    public String note;

    @RestForm
    public boolean publicLink;
    
    @RestForm
    public boolean freestyle;

    @Override
    protected Uni<Warband> apply(Warband warband) {
        warband.name = Controller.decode(name);
        warband.arena = arena;
        warband.pointLimit = pointLimit;
        warband.note = note;
        warband.publicLink = publicLink;
        warband.freestyle = freestyle;
        return Uni.createFrom().item(warband);
    }

    @Override
    protected Uni<Warband> validateForm(Warband warband) {
        if (warband.getTotalCost() > pointLimit.value) {
            throw new FormException(
                    Qute.fmt("Warband {} total points exceeded the selected limit: {}", warband.getTotalCost(),
                            pointLimit.value));
        }
        return super.validateForm(warband);
    }

}
