package com.nedap.retail.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nedap.retail.example.rest.ApiCaller;
import com.nedap.retail.example.rest.UnauthorizedException;
import com.nedap.retail.example.websocket.client.RenosWebSocketClient;
import com.nedap.retail.renos.api.v2.rest.message.*;
import com.nedap.retail.renos.api.v2.rest.message.Settings.LightAndSoundStatus;
import com.nedap.retail.renos.api.v2.ws.message.EventType;
import com.nedap.retail.renos.api.v2.ws.message.Subscribe;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final int LIGHT = 1;
    private static final int SOUND = 2;

    private static RenosWebSocketClient client;
    private static ApiCaller api;

    public static void main(final String[] args) {
        if (args.length == 0) {
            LOG.info("Please use URL of device as parameter, for example: http://localhost:8081");
            System.exit(0);
        }

        api = new ApiCaller(args[0]);

        LOG.info("Application starting...");
        client = new RenosWebSocketClient(args[0]);

        try (BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(System.in))) {
            client.run();

            String input;
            while (true) {
                printMenu();

                // get choice
                input = inputBuffer.readLine();

                // exit if no choice made
                if (input.isEmpty()) {
                    break;
                }

                // check what choice has been made
                final char keycode = input.charAt(0);
                handleCommand(inputBuffer, keycode);
            }
        } catch (final Exception e) {
            LOG.error("An error has occurred, system will exit", e);
        } finally {
            client.finish();
        }
    }

    private static void printMenu() {
        // print line
        LOG.info("------------------------------------------------------");
        LOG.info("Available commands:");
        LOG.info("    a) authenticate against Renos");
        LOG.info("    h) send heartbeat to Renos");
        LOG.info("    i) get system information from Renos");
        LOG.info("    g) get group information from Renos");
        LOG.info("    s) get system status from Renos");
        LOG.info("    t) get system settings from Renos");
        LOG.info("    u) update system settings");
        LOG.info("    b) send blink request to Renos");
        LOG.info("    e) subscribe to events from Renos");
        LOG.info("Please enter your choice and press Enter, or just Enter to exit.");
        // print line
        LOG.info("------------------------------------------------------");
    }

    private static void handleCommand(final BufferedReader inputBuffer, final char keycode)
            throws IOException, InterruptedException, URISyntaxException {
        try {
            switch (keycode) {
                case 'a':
                    handleBasicAuthentication(inputBuffer);
                    break;
                case 'h':
                    sendHeartbeat(inputBuffer);
                    break;
                case 'i':
                    retrieveSystemInfo();
                    break;
                case 'g':
                    retrieveGroupInfo();
                    break;
                case 's':
                    retrieveSystemStatus();
                    break;
                case 't':
                    retrieveSystemSettings();
                    break;
                case 'u':
                    updateSystemSettings(inputBuffer);
                    break;
                case 'b':
                    sendBlinkRequest(inputBuffer);
                    break;
                case 'e':
                    subscribeToEvents(inputBuffer);
                    break;
                default:
                    LOG.info("Unknown command {}", keycode);
                    printMenu();
                    break;
            }
        } catch (final UnauthorizedException ex) {
            LOG.info("Unauthorized access to Renos API. Please authenticate first before making any further requests.");
        } catch (final Exception e) {
            LOG.error("There was an error in communication with Renos: ", e);
        }
    }

    private static void handleBasicAuthentication(final BufferedReader inputBuffer) throws Exception {
        LOG.info("Please supply credentials needed for authentication against Renos API.");
        LOG.info("Credentials are only needed if basic authentication is enabled for Renos.");
        LOG.info("If that's not the case, feel free to press Enter when asked for username and password");
        LOG.info("");
        LOG.info("Please enter username:");
        final String username = inputBuffer.readLine();
        LOG.info("Please enter password:");
        final String password = inputBuffer.readLine();
        api.setUsername(username);
        api.setPassword(password);
    }

    private static void sendHeartbeat(final BufferedReader inputBuffer) throws Exception {
        LOG.info("Please choose protocol to send heartbeat:");
        LOG.info("  1. REST");
        LOG.info("  2. WebSocket");
        final String input = inputBuffer.readLine();
        // exit if no choice made
        if (input.isEmpty()) {
            return;
        }

        // check what choice has been made
        final char keycode = input.charAt(0);
        switch (keycode) {
            case '1':
                api.heartbeat();
                break;
            case '2':
                client.heartbeat();
                break;
            default:
                LOG.info("Invalid protocol.");
                break;
        }
    }

    private static void retrieveSystemInfo() throws Exception {
        final SystemInfo info = api.retrieveSystemInfo();
        LOG.info("System info");
        LOG.info("System id: {}", info.id);
        LOG.info("Firmware version: {}", info.version);
        LOG.info("System role: {}", info.systemRole);
        LOG.info("System time: {}", info.systemTime);
    }

    private static void retrieveGroupInfo() throws Exception {
        final GroupInfo groupInfo = api.retrieveGroupInfo();
        LOG.info("Group information");
        for (final GroupInfo.Group group : groupInfo.groups) {
            LOG.info("Group {}: '{}'", group.id, group.name);
            for (final GroupInfo.Unit u : group.units) {
                LOG.info("  Unit {}: '{}'", u.id, u.name);
            }
            LOG.info("  Aisles: {}", group.aisles.stream().map((a) -> a.id).collect(Collectors.toList()));
        }
    }

    private static void retrieveSystemStatus() throws Exception {
        final SystemStatus status = api.retrieveSystemStatus();
        LOG.info("System status");
        LOG.info("Unreachable units: {}", status.unreachableUnits != 0);
        LOG.info("Device management connection error: {}", status.deviceManagementConnectionError != 0);
        LOG.info("Rfid reader errors: {}", status.rfidErrors != 0);
        LOG.info("Blocked IR beam sensors: {}", status.blockedIrBeamSensors != 0);
    }

    private static void retrieveSystemSettings() throws Exception {
        final Settings settings = api.retrieveSystemSettings();
        LOG.info("System settings");
        LOG.info("RF enabled {}", settings.enableRf);
        LOG.info("RFID enabled {}", settings.enableRfid);
        LOG.info("RF alarm triggers {}", settings.lightSoundRf);
        LOG.info("RFID alarm triggers {}", settings.lightSoundRfid);
    }

    private static void updateSystemSettings(final BufferedReader inputBuffer) throws Exception {
        LOG.info("Update system settings:");
        LOG.info("Enable RF (y/n/empty for no change):");
        final Boolean enableRf = readBoolean(inputBuffer, null);
        LOG.info("Enable RFID (y/n/empty for no change):");
        final Boolean enableRfid = readBoolean(inputBuffer, null);
        LOG.info("In case of an RF alarm, trigger:");
        final LightAndSoundStatus rfLightAndSound = getLightAndSoundSelection(inputBuffer);
        LOG.info("In case of an RFID alarm, trigger:");
        final LightAndSoundStatus rfidLightAndSound = getLightAndSoundSelection(inputBuffer);

        final Settings settings = new Settings(enableRf, enableRfid, rfLightAndSound, rfidLightAndSound);
        api.updateSettings(settings);
    }

    private static LightAndSoundStatus getLightAndSoundSelection(final BufferedReader inputBuffer) throws IOException {
        LOG.info("1. Light and sound");
        LOG.info("2. Lights only");
        LOG.info("3. None");
        LOG.info("Empty for no change");
        final Integer rfLightAndSound = readInput(inputBuffer, 0);
        switch (rfLightAndSound) {
            case 1:
                return LightAndSoundStatus.ON;
            case 2:
                return LightAndSoundStatus.LIGHTS_ONLY;
            case 3:
                return LightAndSoundStatus.OFF;
            default:
                return null;
        }
    }

    private static void sendBlinkRequest(final BufferedReader inputBuffer) throws Exception {
        Integer soundPeriod = null;
        Integer soundRepeats = null;
        Integer soundVolume = null;
        String audioFileName = null;
        boolean sound = true;
        boolean light = true;
        LedColor rgbValue = null;

        LOG.info("Trigger pattern:");
        LOG.info("1 = light");
        LOG.info("2 = sound");
        LOG.info("3 = light and sound (default)");
        final Integer blinkOptions = readInput(inputBuffer, 3);
        LOG.info("How many times: (default 5)");
        final Integer count = readInput(inputBuffer, 5);
        LOG.info("Time the lamp/buzzer is on (in milliseconds, default 400): ");
        final Integer onTime = readInput(inputBuffer, 400);
        LOG.info("Time the lamp/buzzer is off (in milliseconds, default 50): ");
        final Integer offTime = readInput(inputBuffer, 50);
        LOG.info("Time the lamp is on afterwards (in milliseconds, default 7000): ");
        final Integer lightsHoldTime = readInput(inputBuffer, 7000);
        LOG.info("Does this system contain !Sense Lumen hardware(Y/N)? (default N)");
        final boolean hasLumen = checkLumen(inputBuffer, false);
        if (hasLumen) {
            if (blinkOptions != SOUND) {
                LOG.info("Enter RGB value(0 - 255) with comma separated (default 255,0,0)");
                rgbValue = readRGB(inputBuffer);
            }
            if (blinkOptions != LIGHT) {
                LOG.info("Sound file name (default rf_eas.wav)");
                audioFileName = readString(inputBuffer, "rf_eas.wav");
                LOG.info("Sound period (in milliseconds, default 1000)");
                soundPeriod = readInput(inputBuffer, 1000);
                LOG.info("Number of times sound will be repeated (default 10)");
                soundRepeats = readInput(inputBuffer, 10);
                LOG.info("Sound volume (default 10)");
                soundVolume = readInput(inputBuffer, 10);
            }
        }

        switch (blinkOptions) {
            case LIGHT:
                sound = false;
                break;
            case SOUND:
                light = false;
                break;
            default:
                // both light and sound remain true
                break;
        }

        final BlinkRequest request = new BlinkRequest(onTime, offTime, count, lightsHoldTime, light, sound,
                rgbValue, audioFileName, soundPeriod, soundRepeats, soundVolume);
        api.sendBlink(request);
    }

    private static void subscribeToEvents(final BufferedReader inputBuffer) throws IOException {
        LOG.info("Subscribe to: ");
        LOG.info("1. RF Alarm events");
        LOG.info("2. RFID Alarm events");
        LOG.info("3. IR Direction events");
        LOG.info("4. Metal alarm");
        LOG.info("5. RFID observation events");
        LOG.info("6. RFID move events");
        LOG.info("7. Input events");
        LOG.info("8. SD label detect events");
        LOG.info("Please choose all subscriptions separated by a comma, e.g., 1,3 (default: all)");
        final String selection = inputBuffer.readLine();
        final EventType[] eventTypes;
        if (selection.isEmpty()) {
            // all events
            eventTypes = EventType.values();
        } else {
            eventTypes = parseEventTypes(selection);
        }

        LOG.info("Please enter subscription reference (leave empty for no reference):");
        final String reference = inputBuffer.readLine();

        LOG.info("If you would like to include previous events, please enter date and time since when (max. 2h ago):");

        LOG.info("Please use ISO-8601 representation (e.g. {}), leave empty for real-time events only",
                Instant.now().toString());
        final String includeEventsSince = inputBuffer.readLine();

        final Subscribe subscribe = new Subscribe(reference, includeEventsSince, eventTypes);
        client.sendSubscription(subscribe);
    }

    private static EventType[] parseEventTypes(final String selection) {
        final String[] parsedSelection = selection.split(",");
        final List<EventType> selectedEvents = new ArrayList<>();
        for (final String option : parsedSelection) {
            switch (option) {
                case "1":
                    selectedEvents.add(EventType.RF_ALARM);
                    break;
                case "2":
                    selectedEvents.add(EventType.RFID_ALARM);
                    break;
                case "3":
                    selectedEvents.add(EventType.IR_DIRECTION);
                    break;
                case "4":
                    selectedEvents.add(EventType.METAL_ALARM);
                    break;
                case "5":
                    selectedEvents.add(EventType.RFID_OBSERVATION);
                    break;
                case "6":
                    selectedEvents.add(EventType.RFID_MOVE);
                    break;
                case "7":
                    selectedEvents.add(EventType.INPUT_OBSERVATION);
                    break;
                case "8":
                    selectedEvents.add(EventType.SD_LABEL_DETECT);
                    break;
                default:
                    LOG.info("Unsupported option value {}", option);
            }
        }
        final EventType[] eventTypes = new EventType[selectedEvents.size()];
        selectedEvents.toArray(eventTypes);
        return eventTypes;
    }

    private static Integer readInput(final BufferedReader inputBuffer, final Integer defaultValue) throws IOException {
        final String value = inputBuffer.readLine();
        if (value.trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.valueOf(value);
    }

    private static Boolean readBoolean(final BufferedReader inputBuffer, final Boolean defaultValue)
            throws IOException {
        final String value = inputBuffer.readLine();
        if (value.trim().isEmpty()) {
            return defaultValue;
        }

        return "y".equalsIgnoreCase(value);
    }

    private static String readString(final BufferedReader inputBuffer, final String defaultValue) throws IOException {
        final String value = inputBuffer.readLine();
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    private static LedColor readRGB(final BufferedReader inputBuffer) throws IOException {
        final String value = inputBuffer.readLine();
        if (value.trim().isEmpty()) {
            return new LedColor(255, 0, 0);
        }
        final String[] values = value.split(",");

        final int redValue = Integer.parseInt(values[0]);
        final int greenValue = Integer.parseInt(values[1]);
        final int blueValue = Integer.parseInt(values[2]);

        return new LedColor(redValue, greenValue, blueValue);
    }

    private static Boolean checkLumen(final BufferedReader inputBuffer, final boolean defaultValue)
            throws IOException {
        final String value = inputBuffer.readLine();
        if (value.trim().isEmpty()) {
            return defaultValue;
        } else {
            if ("y".equalsIgnoreCase(value)) {
                return true;
            } else if ("n".equalsIgnoreCase(value)) {
                return false;
            } else {
                LOG.info("You entered an unknown character. (Assumed system has no lumen hardware)");
                return false;
            }
        }
    }


}
