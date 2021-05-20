/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.magnus.internal.handler;

import static org.openhab.binding.magnus.internal.MagnusBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.magnus.internal.config.MagnusConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link MagnusFanControllerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Austin Hanson - Initial contribution
 */
@NonNullByDefault
public class MagnusFanControllerHandler extends BaseThingHandler {
    private static final int TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_REFRESH_SECONDS = 3;
    private static final int SPEED_STEPS = 3;
    private static final int OSCILLATE_SPEED_STEPS = 1;

    private final Logger logger = LoggerFactory.getLogger(MagnusFanControllerHandler.class);
    private final HttpClient client = new HttpClient();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Nullable
    private MagnusFanStatus status;

    @Nullable
    private ScheduledFuture<?> pollFuture;
    private int refresh;
    @Nullable
    private String baseUrl;
    private boolean disposed = false;

    private @Nullable MagnusConfiguration config;

    public MagnusFanControllerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Suspend polling while we process the command
        clearPolling();

        logger.info("ChannelUID {}, command {} {}", channelUID.getId(), command.toFullString(),
                command.getClass().getSimpleName());

        if (FanController.Channels.RW_POWER.equals(channelUID.getId())) {
            HandlePowerCommand(command);
        } else if (FanController.Channels.RW_OSCILLATE.equals(channelUID.getId())) {
            HandleOscillateCommand(command);
        } else if (FanController.Channels.RW_SPEED.equals(channelUID.getId())) {
            HandleSpeedCommand(channelUID, command);
        } else if (FanController.Channels.RW_OSCILLATE_SPEED.equals(channelUID.getId())) {
            HandleOscillateSpeedCommand(channelUID, command);
        } else if (FanController.Channels.RW_TIMER.equals(channelUID.getId())) {
            HandleTimerCommand(channelUID, command);
        }

        // Resume polling
        initPolling(0);
    }

    private void HandleTimerCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            RefreshStatus(true);
        } else if (command instanceof PercentType) {
            double value = ((PercentType) command).doubleValue();
            logger.info("Setting fan {} timer to {}%", thing.getUID(), value);
            ParseStatus(postUrl("/timer/" + value, TIMEOUT_SECONDS));
        } else {
            logger.info("Unknown command");
        }
    }

    private void HandleOscillateSpeedCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            RefreshStatus(true);
        } else if (command instanceof PercentType) {
            @Nullable
            MagnusFanStatus response = null;

            int value = ConvertPercentToOscillateSpeed((PercentType) command);
            logger.info("Setting fan oscillation {} speed to {}%", thing.getUID(), value);
            response = ParseStatus(postUrl("/oscillate_speed/" + value, TIMEOUT_SECONDS));
            if (response == null)
                logger.error("Error parsing command result");
            else if (ConvertSpeedToPercent(response.OscillateSpeed).doubleValue() != ((PercentType) command)
                    .doubleValue())
                updateState(channelUID, ConvertSpeedToPercent(response.OscillateSpeed));
        } else {
            logger.info("Unknown command");
        }
    }

    private void HandleSpeedCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            RefreshStatus(true);
        } else if (command instanceof PercentType) {
            @Nullable
            MagnusFanStatus response = null;

            int value = ConvertPercentToSpeed((PercentType) command);
            logger.info("Setting fan {} speed to {}%", thing.getUID(), value);
            response = ParseStatus(postUrl("/speed/" + value, TIMEOUT_SECONDS));
            if (response == null)
                logger.error("Error parsing command result");
            else if (ConvertSpeedToPercent(response.Speed).doubleValue() != ((PercentType) command).doubleValue())
                updateState(channelUID, ConvertSpeedToPercent(response.Speed));
        } else {
            logger.info("Unknown command");
        }
    }

    private void HandleOscillateCommand(Command command) {
        if (command instanceof RefreshType) {
            RefreshStatus(true);
        } else if (command == OnOffType.ON) {
            logger.info("Turning fan oscillation {} on", thing.getUID());
            postUrl("/oscillate/on", TIMEOUT_SECONDS);
        } else if (command == OnOffType.OFF) {
            logger.info("Turning fan oscillation {} off", thing.getUID());
            postUrl("/oscillate/off", TIMEOUT_SECONDS);
        } else {
            logger.info("Unknown command");
        }
    }

    private void HandlePowerCommand(Command command) {
        if (command instanceof RefreshType) {
            RefreshStatus(true);
        } else if (command == OnOffType.ON) {
            logger.info("Turning fan {} on", thing.getUID());
            String response = postUrl("/power/on", TIMEOUT_SECONDS);
            logger.info("{}", response);
        } else if (command == OnOffType.OFF) {
            logger.info("Turning fan {} off", thing.getUID());
            String response = postUrl("/power/off", TIMEOUT_SECONDS);
            logger.info("{}", response);
        } else {
            logger.info("Unknown command");
        }
    }

    private int ConvertPercentToSpeed(PercentType typeValue) {
        double convertedValue = typeValue.doubleValue() / 100.0 * (float) SPEED_STEPS;
        double rounded = Math.round(convertedValue);
        logger.info("{} => {} => {}", typeValue.doubleValue(), convertedValue, rounded);
        return (int) rounded;
    }

    private PercentType ConvertSpeedToPercent(int speed) {
        return new PercentType((int) Math.round(100.0 * speed / (float) SPEED_STEPS));
    }

    private int ConvertPercentToOscillateSpeed(PercentType typeValue) {
        return (int) Math.round(typeValue.doubleValue() / 100.0 * (float) OSCILLATE_SPEED_STEPS);
    }

    private PercentType ConvertOscillateSpeedToPercent(int speed) {
        return new PercentType((int) Math.round(100.0 * speed / (float) OSCILLATE_SPEED_STEPS));
    }

    @Nullable
    private MagnusFanStatus ParseStatus(@Nullable String json) {
        if (json == null)
            return null;
        return gson.fromJson(json, MagnusFanStatus.class);
    }

    @Nullable
    private MagnusFanStatus GetStatus() {
        logger.info("Grabbing status for {}", thing.getUID());
        String response = getUrl("/status", TIMEOUT_SECONDS);
        logger.info("Response {}", response);
        return gson.fromJson(response, MagnusFanStatus.class);
    }

    @Nullable
    private MagnusFanStatus RefreshStatus() {
        return RefreshStatus(false);
    }

    @Nullable
    private MagnusFanStatus RefreshStatus(boolean forceRefresh) {
        MagnusFanStatus oldStatus = status;
        status = GetStatus();

        if (status != null && !oldStatus.equals(status)) {
            logger.info("Updating status for {}", thing.getUID());
            logger.info("Old Status {}", oldStatus);
            logger.info("New Status {}", status);
            updateState(FanController.Channels.RW_POWER, status.Power ? OnOffType.ON : OnOffType.OFF);
            updateState(FanController.Channels.RW_OSCILLATE, status.Oscillate ? OnOffType.ON : OnOffType.OFF);
            updateState(FanController.Channels.RW_SPEED, ConvertSpeedToPercent(status.Speed));
            updateState(FanController.Channels.RW_OSCILLATE_SPEED,
                    ConvertOscillateSpeedToPercent(status.OscillateSpeed));
            updateState(FanController.Channels.RW_TIMER, new PercentType(status.Timer));
        }

        return status;
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>

            if (!client.isStarted()) {
                try {
                    client.start();
                } catch (Exception e) {
                    logger.error("Could not stop HttpClient", e);
                }
            }

            configure();

            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    }

    @Override
    public synchronized void dispose() {
        disposed = true;

        logger.debug("Handler disposed.");
        clearPolling();

        if (client != null) {
            client.getAuthenticationStore().clearAuthentications();
            client.getAuthenticationStore().clearAuthenticationResults();
            if (client.isStarted()) {
                try {
                    client.stop();
                } catch (Exception e) {
                    logger.error("Could not stop HttpClient", e);
                }
            }
        }
    }

    /**
     * Configures this thing
     */
    private void configure() {
        clearPolling();

        config = getConfigAs(MagnusConfiguration.class);
        String authentication_key = config.authentication_key;
        Integer refreshOrNull = config.refresh;

        if (StringUtils.isBlank(authentication_key)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "authentication_key must not be empty");
            return;
        }

        refresh = DEFAULT_REFRESH_SECONDS;
        if (refreshOrNull != null) {
            refresh = refreshOrNull.intValue();
        }

        String host = this.thing.getProperties().get(HOST);

        baseUrl = "http://" + host + ":80";
        logger.info("Magnus binding configured with base url {} and refresh period of {}", baseUrl, refresh);

        initPolling(0);
    }

    private synchronized void initPolling(int initialDelay) {
        if (disposed)
            return;

        clearPolling();
        pollFuture = scheduler.scheduleWithFixedDelay(() -> {
            try {
                pollFanDevice();
            } catch (Exception e) {
                logger.debug("Exception during poll", e);
            }
        }, initialDelay, DEFAULT_REFRESH_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Stops/clears this thing's polling future
     */
    private synchronized void clearPolling() {
        if (pollFuture != null && !pollFuture.isCancelled()) {
            logger.trace("Canceling future");
            pollFuture.cancel(false);
        }
    }

    private void pollFanDevice() {
        logger.trace("Connecting to {}", baseUrl);
        RefreshStatus();
    }

    /**
     * Simple logic to perform a authenticated GET request
     *
     * @param url
     * @param timeout
     * @return
     */
    @Nullable
    private synchronized String getUrl(String path, int timeout) {
        String url = baseUrl + path;
        logger.debug("Getting URL {} ", url);
        Request request = client.newRequest(url).timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).method(HttpMethod.GET);
        ;
        // request.header(HttpHeader.AUTHORIZATION, basicAuthentication);
        try {
            ContentResponse response = request.send();
            int statusCode = response.getStatus();
            if (statusCode != HttpStatus.OK_200) {
                logger.debug("Method failed: {}", response.getStatus() + " " + response.getReason());
                return null;
            }
            return response.getContentAsString();
        } catch (Exception e) {
            logger.debug("Could not make http connection", e);
        }
        return null;
    }

    /**
     * Simple logic to perform a authenticated GET request
     *
     * @param url
     * @param timeout
     * @return
     */
    @Nullable
    private synchronized String postUrl(String path, int timeout) {
        String url = baseUrl + path;
        logger.info("Posting URL {} ", url);
        Request request = client.newRequest(url).timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).method(HttpMethod.POST);
        // request.header(HttpHeader.AUTHORIZATION, basicAuthentication);
        try {
            ContentResponse response = request.send();
            int statusCode = response.getStatus();
            if (statusCode != HttpStatus.OK_200) {
                logger.info("Method failed: {}", response.getStatus() + " " + response.getReason());
                return null;
            }
            return response.getContentAsString();
        } catch (Exception e) {
            logger.info("Could not make http connection", e);
        }
        return null;
    }
}
