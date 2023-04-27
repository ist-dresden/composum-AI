package com.composum.chatgpt.bundle;

import java.util.List;

import javax.annotation.Nonnull;

import org.osgi.service.component.annotations.Component;

import com.composum.pages.commons.service.PagesPlugin;


/**
 * Declares our relationship to Pages.
 */
@Component(service = PagesPlugin.class, immediate = true)
public class ChatGPTPagesPlugin implements PagesPlugin {

    /**
     * Arbitrary value for the rank, since there is nothing else yet.
     */
    public static final int RANK = 1000;

    /**
     * The component to render for generating the ChatGPT buttons on the widgets.
     */
    public static final List<String> LABELEXTENSIONS = List.of("composum/chatgpt/pagesintegration/widgetextensions/labelextension");

    public int getRank() {
        return RANK;
    }

    @Nonnull
    public List<String> getWidgetLabelExtensions() {
        return LABELEXTENSIONS;
    }

}
