package com.composum.ai.composum.bundle.service;

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import com.composum.ai.backend.slingbase.AIConfigurationPlugin;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;
import com.composum.pages.commons.model.Text;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.SlingResourceUtil;

/**
 * Implements Composum specific methods of AIConfigurationPlugin.
 */
@Component(
        property = Constants.SERVICE_RANKING + ":Integer=5000"
)
public class ComposumAIConfigurationPlugin implements AIConfigurationPlugin {

    @Nullable
    @Override
    public GPTPromptLibrary getGPTPromptLibraryPathsDefault() {
        return new GPTPromptLibrary() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return GPTPromptLibrary.class;
            }

            @Override
            public String contentCreationPromptsPath() {
                return "/libs/composum/pages/options/ai/dialogs/create/defaultprompts";
            }

            @Override
            public String sidePanelPromptsPath() {
                return "/libs/composum/pages/options/ai/tools/sidebar/defaultprompts";
            }
        };
    }

    @Nullable
    @Override
    public Map<String, String> getGPTConfigurationMap(@Nonnull SlingHttpServletRequest request, @Nullable String mapPath, @Nullable String ignored) {
        if (StringUtils.isBlank(mapPath) || mapPath.toLowerCase().contains(".json")) {
            return null;
        }
        BeanContext.Service context = new BeanContext.Service(request, null);
        Resource resource = context.getResolver().getResource(mapPath);
        if (resource == null) {
            return null;
        }
        if (resource.getChild(JCR_CONTENT) != null) {
            resource = resource.getChild(JCR_CONTENT);
        }
        if (!resource.getName().equals(JCR_CONTENT)) {
            return null;
        }

        // search for the child resource that has the most children - that's the most likely container for the prompts.
        Optional<Pair<Resource, List<Resource>>> promptContainer = SlingResourceUtil.descendantsStream(resource)
                .map(r -> Pair.of(r, IteratorUtils.toList(r.listChildren())))
                .min((a, b) -> Integer.compare(b.getRight().size(), a.getRight().size()));
        if (promptContainer.isEmpty()) {
            return null;
        }

        List<Resource> prompts = promptContainer.get().getRight();
        // we assume that the prompts are standard text components with the prompt name as the title and the prompt as the text.
        Map<String, String> result = new LinkedHashMap<>();
        for (Resource prompt : prompts) {
            Text model = new Text(context, prompt);
            String title = model.getTitle();
            String text = model.getText();
            if (StringUtils.isNoneBlank(title, text)) {
                result.put(title, text);
            }
        }
        return result;
    }

}
