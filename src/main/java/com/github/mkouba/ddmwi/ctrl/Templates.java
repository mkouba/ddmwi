package com.github.mkouba.ddmwi.ctrl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.CreatureView;
import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.dao.PageResults;
import com.github.mkouba.ddmwi.dao.SortInfo;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@CheckedTemplate
class Templates {

    static native TemplateInstance creatures(PageResults<? extends CreatureView> page, String query, SortInfo sortInfo);

    static native TemplateInstance warbands(PageResults<Warband> page, String query, SortInfo sortInfo);
    
    static native TemplateInstance warband(Warband warband, PageResults<? extends CreatureView> page,
            List<String> errorMessages, String query, SortInfo sortInfo);

    static native TemplateInstance creature(Creature creature, List<String> errorMessages);

    static TemplateInstance creature(Creature creature) {
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

    static native TemplateInstance users(List<User> users);

    static native TemplateInstance user(User user, List<String> errorMessages);

    static native TemplateInstance warbandLink(Warband warband);
    
    static native TemplateInstance dashboard(Dashboard.Info info, List<Map.Entry<String, LocalDateTime>> activeUsers);
    
    static native TemplateInstance dashboard$activeUsers(List<Map.Entry<String, LocalDateTime>> activeUsers);
    
}