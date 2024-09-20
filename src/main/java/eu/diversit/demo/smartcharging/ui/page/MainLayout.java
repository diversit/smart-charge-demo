package eu.diversit.demo.smartcharging.ui.page;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout {

    public MainLayout() {

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContext();
    }

    private void addDrawerContent() {
        var image = new Image("https://devoxx.be/wp-content/uploads/2019/05/Devoxx.png", "Devoxx");
        image.addClickListener(_ -> getUI().ifPresent(ui -> ui.access(() -> ui.navigate("/"))));

        var header = new Header(image);
        header.getStyle().set("background", "black");

        var sideNavs = new VerticalLayout();
        sideNavs.setPadding(false);
        sideNavs.setMargin(false);
        var scroller = new Scroller(sideNavs);

        addToDrawer(header, scroller);
    }

    private void addHeaderContext() {

        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        var viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
    }
}
