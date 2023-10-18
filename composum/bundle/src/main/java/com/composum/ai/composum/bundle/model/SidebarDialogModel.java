package com.composum.ai.composum.bundle.model;

import static com.composum.ai.composum.bundle.model.CreateDialogModel.readJsonFile;

import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.Resource;

import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.composum.ai.backend.slingbase.AIConfigurationServlet;
import com.composum.ai.composum.bundle.AIDialogServlet;
import com.composum.pages.commons.model.Page;
import com.composum.pages.stage.model.edit.FrameModel;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.LinkUtil;

/**
 * Model for the sidebar AI dialog.
 */
public class SidebarDialogModel extends FrameModel {

    public Map<String, String> getPredefinedPrompts() {
        return readJsonFile("sidebar/predefinedprompts.json");
    }

    public Map<String, String> getContentSelectors() {
        return readJsonFile("sidebar/contentselectors.json");
    }

    public String getPageContentResourcePath() {
        Page currentPage = this.getCurrentPage();
        return currentPage != null ? currentPage.getContent().getPath() : null;
    }

    public String getComponentPath() {
        Resource componentResource = getDelegate().getResource();
        return componentResource != null ? ResourceHandle.use(componentResource).getContentResource().getPath() : null;
    }

    public String getHelpUrl() {
        return LinkUtil.getUrl(getContext().getRequest(),
                AIDialogServlet.SERVLET_PATH + ".sidebarDialog.help.html" + getDelegate().getPath());
    }

    public boolean isEnabled() {
        AIConfigurationService aiConfigurationService = getContext().getService(AIConfigurationService.class);
        Set<String> result = aiConfigurationService.allowedServices(getContext().getRequest(), this.getPageContentResourcePath(), getContext().getRequest().getRequestURI());
        return result.contains(AIConfigurationServlet.SERVICE_SIDEPANEL);
    }

}
