package eu.diversit.demo.smartcharging.ui.page;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

/**
 * For live reloading of data it is required to add the {@code @Push} annotation
 * on a Vaadin configuration class.
 * See <a href="https://vaadin.com/docs/latest/flow/advanced/server-push">Vaadin Server Push</a>
 * <p>
 * This class can also be used to customize the theme by
 * adding a {@code @Theme} annotation
 * and place the style in the {@code src/main/frontend/<theme>} folder.
 * See <a href="https://vaadin.com/docs/latest/styling/application-theme#applying-a-theme">Applying a theme</a>.
 */
@Push
public class UiConfig implements AppShellConfigurator {
}
