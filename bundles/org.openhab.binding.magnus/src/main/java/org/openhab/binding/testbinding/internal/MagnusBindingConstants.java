/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.magnus.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link MagnusBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Austin Hanson - Initial contribution
 */
@NonNullByDefault
public class MagnusBindingConstants {

    private static final String BINDING_ID = "magnus";
    public static final String HOST = "host";
    public static final String SERIAL_NUMBER = "serial_number";

    public static class FanController {
        public static final ThingTypeUID TYPE = new ThingTypeUID(BINDING_ID, "fan_controller");

        public static class Channels {
            // RO Channels
            // public static final String CHANNEL_RO_status = "status";

            // RW Channels
            public static final String RW_POWER = "power";
            public static final String RW_SPEED = "speed";
            public static final String RW_TIMER = "timer";
            public static final String RW_OSCILLATION = "oscillation";
        }
    }
}
