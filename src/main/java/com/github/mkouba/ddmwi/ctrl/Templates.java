package com.github.mkouba.ddmwi.ctrl;

import java.util.Collections;
import java.util.List;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.CreatureView;
import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.dao.PageResults;
import com.github.mkouba.ddmwi.dao.SortInfo;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;

@CheckedTemplate
class Templates {

    static native TemplateInstance creatures(Uni<PageResults<? extends CreatureView>> page, String query, SortInfo sortInfo);

    static native TemplateInstance warbands(Uni<PageResults<Warband>> page, String query, SortInfo sortInfo);
    
    static native TemplateInstance warband(Uni<Warband> warband, Uni<PageResults<? extends CreatureView>> page,
            List<String> errorMessages, String query, SortInfo sortInfo);

    static native TemplateInstance creature(Uni<Creature> creature, List<String> errorMessages);

    static TemplateInstance creature(Uni<Creature> creature) {
        return creature(creature, Collections.emptyList());
    }

    static native TemplateInstance error(String message, boolean signIn);

    static TemplateInstance error() {
        return error(null, false);
    }

    static native TemplateInstance login();

    static native TemplateInstance creaturesImport();

    static native TemplateInstance warbandsImport();

    static native TemplateInstance collectionImport();

    static native TemplateInstance users(Uni<List<User>> users);

    static native TemplateInstance user(Uni<User> user, List<String> errorMessages);

    static native TemplateInstance warbandLink(Warband warband);
    
}