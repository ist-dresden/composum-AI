package com.composum.ai.composum.bundle.model;

import static com.composum.ai.composum.bundle.model.CreateDialogModel.readJsonFile;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.composum.ai.backend.slingbase.model.GPTPermissionInfo;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;
import com.composum.ai.composum.bundle.AIDialogServlet;
import com.composum.pages.commons.model.Page;
import com.composum.pages.stage.model.edit.FrameModel;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.LinkUtil;

/**
 * Model for the sidebar AI dialog.
 */
public class SidebarDialogModel extends FrameModel {

    private static final Logger LOG = LoggerFactory.getLogger(SidebarDialogModel.class);

    protected transient AIConfigurationService aiConfigurationService;

    public Map<String, String> getPredefinedPrompts() {
        GPTPromptLibrary paths = getAIConfigurationService().getGPTPromptLibraryPaths(getContext().getRequest(), getResource().getPath());
        if (paths != null) {
            String path = paths.sidePanelPromptsPath();
            Map<String, String> map = getAIConfigurationService().getGPTConfigurationMap(getContext().getRequest(), path, null);
            return map;
        }
        LOG.error("No paths for predefined prompts found for {}", getPath());
        return null;
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
        GPTPermissionInfo permissionInfo = aiConfigurationService.allowedServices(getContext().getRequest(), this.getPageContentResourcePath(), getContext().getRequest().getRequestURI());
        return permissionInfo != null && permissionInfo.allows(GPTPermissionInfo.SERVICE_SIDEPANEL,
                Resource.RESOURCE_TYPE_NON_EXISTING); // component doesn't make sense here.
    }

    protected AIConfigurationService getAIConfigurationService() {
        if (aiConfigurationService == null) {
            aiConfigurationService = requireNonNull(getContext().getService(AIConfigurationService.class));
        }
        return aiConfigurationService;
    }

}
