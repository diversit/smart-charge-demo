package eu.diversit.demo.smartcharging.model.json;

import eu.diversit.demo.smartcharging.model.json.ocpp.*;
import io.vavr.control.Option;
import io.vavr.control.Try;

/**
 * Defines all OCPP actions.
 */
public sealed interface Action {

    static Option<Action> findAction(String action) {
        return ByChargePoint.findAction(action)
                .orElse(ByCentralSystem.findAction(action));
    }

    Class<?> getClazz();

    String name();

    /**
     * Actions triggered by a charge point.
     * <p>
     * The `clazz` property represents the 'call' class type
     * to decode the call payload.
     */
    enum ByChargePoint implements Action {

        AUTHORIZE(Authorize.class),
        BOOTNOTIFICATION(BootNotification.class),
        DATATRANSFER(DataTransfer.class),
        DIAGNOSTICSSTATUSNOTIFICATION(DiagnosticsStatusNotification.class),
        FIRMWARESTATUSNOTIFICATION(FirmwareStatusNotification.class),
        HEARTBEAT(Heartbeat.class),
        METERVALUES(MeterValues.class),
        STARTTRANSACTION(StartTransaction.class),
        STATUSNOTIFICATION(StatusNotification.class),
        STOPTRANSACTION(StopTransaction.class);

        private final Class<?> clazz;

        ByChargePoint(Class<?> clazz) {
            this.clazz = clazz;
        }

        public static Option<Action> findAction(String action) {
            return Try.of(() -> valueOf(action.toUpperCase()))
                    .toOption()
                    .map(v -> v);
        }

        public Class<?> getClazz() {
            return clazz;
        }
    }

    /**
     * Actions triggert by a central system.
     * <p>
     * The `clazz` property is used to set the correct action name in the Ocpp-J message.
     * The `responseclass` property represents the 'callresult' class type to decode a callresult payload.
     */
    enum ByCentralSystem implements Action {

        CANCELRESERVATION(CancelReservation.class, CancelReservationResponse.class),
        CHANGEAVAILABILITY(ChangeAvailability.class, ChangeAvailabilityResponse.class),
        CHANGECONFIGURATION(ChangeConfiguration.class, ChangeConfigurationResponse.class),
        CLEARCACHE(ClearCache.class, ClearCacheResponse.class),
        CLEARCHARGINGPROFILE(ClearChargingProfile.class, ClearChargingProfileResponse.class),
        DATATRANSFER(DataTransfer.class, DataTransferResponse.class),
        GETCOMPOSITESCHEDULE(GetCompositeSchedule.class, GetCompositeScheduleResponse.class),
        GETCONFIGURATION(GetConfiguration.class, GetConfigurationResponse.class),
        GETDIAGNOSTICS(GetDiagnostics.class, GetDiagnosticsResponse.class),
        GETLOCALLISTVERSION(GetLocalListVersion.class, GetLocalListVersionResponse.class),
        REMOTESTARTTRANSACTION(RemoteStartTransaction.class, RemoteStartTransactionResponse.class),
        REMOTESTOPTRANSACTION(RemoteStopTransaction.class, RemoteStopTransactionResponse.class),
        RESERVENOW(ReserveNow.class, ReserveNowResponse.class),
        RESET(Reset.class, ResetResponse.class),
        SENDLOCALLIST(SendLocalList.class, SendLocalListResponse.class),
        SETCHARGINGPROFILE(SetChargingProfile.class, SetChargingProfileResponse.class),
        TRIGGERMESSAGE(TriggerMessage.class, TriggerMessageResponse.class),
        UNLOCKCONNECTOR(UnlockConnector.class, UnlockConnectorResponse.class),
        UPDATEFIRMWARE(UpdateFirmware.class, UpdateFirmwareResponse.class);

        private final Class<?> clazz;
        private final Class<?> responseClass;

        ByCentralSystem(Class<?> clazz, Class<?> responseClass) {
            this.clazz = clazz;
            this.responseClass = responseClass;
        }

        public static Option<Action> findAction(String action) {
            return Try.of(() -> valueOf(action.toUpperCase()))
                    .toOption()
                    .map(v -> v);
        }


        public Class<?> getResponseClazz() {
            return responseClass;
        }

        @Override
        public Class<?> getClazz() {
            return clazz;
        }
    }
}