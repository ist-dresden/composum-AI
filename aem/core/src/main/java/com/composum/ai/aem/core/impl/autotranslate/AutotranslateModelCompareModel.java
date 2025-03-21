package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class AutotranslateModelCompareModel {

    @OSGiService
    protected com.composum.ai.backend.base.service.chat.GPTBackendsService gptBackendsService;

    private List<String> models;

    @PostConstruct
    protected void init() {
        if (gptBackendsService != null) {
            models = gptBackendsService.getAllModels();
        }
    }

    public List<String> getModels() {
        return models;
    }
}
