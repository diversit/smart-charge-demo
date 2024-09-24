package eu.diversit.demo.smartcharging.ui.page;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.diversit.demo.smartcharging.model.*;
import eu.diversit.demo.smartcharging.model.json.ocpp.BootNotification;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main dashboard
 */
@PageTitle("Dashboard")
@Route(value = "", layout = MainLayout.class)
public class Dashboard extends VerticalLayout {

    private final ChargePoint chargePoint;
    private final Broadcaster broadcaster;
    private Component stateContent = null;
    private Broadcaster.Registration registration;

    public Dashboard(ChargePoint chargePoint, Broadcaster broadcaster) {
        this.chargePoint = chargePoint;
        this.broadcaster = broadcaster;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        add(new H2("Smart charge demo"));

        registration = broadcaster.<ChargePointState>register(Broadcaster.UpdateType.CHARGE_POINT_STATE, newState -> {
            attachEvent.getUI().access(() -> showState(newState));
        });

        showState(chargePoint.getState());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (registration != null) {
            registration.unregister();
        }
        super.onDetach(detachEvent);
    }

    private void showState(ChargePointState state) {

        var content = new FormLayout();
        content.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("500px", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE)
        );

        // show charge box id
        content.addFormItem(new TextField("", state.chargeBoxId().map(ChargeBoxId::value).getOrElse("Not connected")), "ChargeBox ID");

        // show boot notification
        var bootNotification = new TextArea("", state.bootNotification().map(BootNotification::toString).getOrElse(""));
        bootNotification.setWidthFull();
        content.addFormItem(bootNotification, "BootNotification");

        // show connector states
        state.connectorStatuses().forEach((connector, statuses) -> {
            var lastStatus = statuses.head().getStatus();
            content.addFormItem(new TextField("", lastStatus.value()), "Connector " + connector.value());
        });

        // show transactions
        content.add(new Text("Transactions"));
        var transactionsGrid = new Grid<Transaction>();
        transactionsGrid.setWidthFull();
        transactionsGrid.setAllRowsVisible(true);
        transactionsGrid.addColumn(t -> t.id().value()).setHeader("Id");
        transactionsGrid.addColumn(t -> t.connector().value()).setHeader("Connector");
        transactionsGrid.addColumn(t -> t.meterStart().value()).setHeader("Start value");
        transactionsGrid.addColumn(t -> formatTimestamp(t.startTimestamp())).setHeader("Start on");
        transactionsGrid.addColumn(t -> t.meterStop().map(MeterValue::value).getOrNull()).setHeader("Stop value");
        transactionsGrid.addColumn(t -> t.stopTimestamp().map(this::formatTimestamp).getOrNull()).setHeader("Stop timestamp");
        transactionsGrid.addColumn(Transaction::totalCharged).setHeader("Total charged");
        transactionsGrid.getColumns().forEach(col -> {
            col.setAutoWidth(true);
        });
        transactionsGrid.setItems(state.transactions().asJava());
        content.add(transactionsGrid);

        content.addFormItem(new TextField("", LocalDateTime.now().toString()), "Last status update");

        Notification.show("Updated", 500, Notification.Position.BOTTOM_STRETCH);

        replace(stateContent, content);
        stateContent = content;
    }

    private String formatTimestamp(ZonedDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }
}
