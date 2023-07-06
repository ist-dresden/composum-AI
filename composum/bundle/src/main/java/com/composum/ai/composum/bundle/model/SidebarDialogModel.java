package com.composum.ai.composum.bundle.model;

import static com.composum.ai.composum.bundle.model.CreateDialogModel.readJsonFile;

import java.util.Map;

import org.apache.sling.api.resource.Resource;

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
        return readJsonFile("create/predefinedprompts.json");
    }

    public Map<String, String> getContentSelectors() {
        return readJsonFile("create/contentselectors.json");
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
        //             /bin/cpm/platform/ai/dialog.creationDialog.html/content/ist/composum/home/platform/setup/_jcr_content/main/row_1608866902/column-0/section/text/textcreationDialog.help.html/content/ist/composum/home/platform/setup/jcr:content/main/row_1608866902/column-0/section/text/text
        //            /bin/cpm/pages/edit.contextTools.html/content/ist/composum/home/platform/setup/_jcr_content/main/row_1608866902/column-0/section/text.help.html/content/ist/composum/home/platform/setup/jcr:content/main/row_1608866902/column-0/section/text
    }

}
