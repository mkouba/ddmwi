package com.github.mkouba.ddmwi.ctrl;

import com.github.mkouba.ddmwi.CreatureView;
import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.dao.PageResults;
import com.github.mkouba.ddmwi.dao.SortInfo;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;

@CheckedTemplate(basePath = "tags")
public class Tags {

    static native TemplateInstance creatureCards(Warband warband, Uni<PageResults<? extends CreatureView>> page, String query,
            SortInfo sortInfo, String hxGet, String hxTarget);

    static native TemplateInstance warbandCards(Uni<PageResults<Warband>> page, String query,
            SortInfo sortInfo, String hxGet, String hxTarget);
}
