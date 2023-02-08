package com.github.mkouba.ddmwi.ctrl;

import com.github.mkouba.ddmwi.CreatureView;
import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.dao.PageResults;
import com.github.mkouba.ddmwi.dao.SortInfo;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@CheckedTemplate(basePath = "tags")
public class Tags {

    static native TemplateInstance creatureCards(Warband warband, PageResults<? extends CreatureView> page, String query,
            SortInfo sortInfo, String hxGet, String hxTarget);

    static native TemplateInstance warbandCards(PageResults<Warband> page, String query,
            SortInfo sortInfo, String hxGet, String hxTarget);
}
