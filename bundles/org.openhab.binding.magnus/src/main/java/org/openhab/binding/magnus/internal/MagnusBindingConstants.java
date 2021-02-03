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
package org.openhab.binding.magnus.internal;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link MagnusBinding} class defines common constants, which are used
 * across the whole binding.
 *
 * @author Austin Hanson - Initial contribution
 */
@NonNullByDefault
public class MagnusBindingConstants {
    private static final String BINDING_ID = "magnus";

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(FanController.THING_TYPE_UID).collect(Collectors.toSet()));

    public static final String HOST = "host";
    public static final String SERIAL_NUMBER = "serial_number";

    public static class FanController {
        private static final String TYPE = "fan_controller";
        public static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID(BINDING_ID, TYPE);

        public static class Channels {
            // RO Channels
            // public static final String CHANNEL_RO_status = "status";

            // RW Channels
            public static final String RW_POWER = TYPE + "-power";
            public static final String RW_SPEED = TYPE + "-speed";
            public static final String RW_TIMER = TYPE + "-timer";
            public static final String RW_OSCILLATE = TYPE + "-oscillate";
            public static final String RW_OSCILLATE_SPEED = TYPE + "-oscillate_speed";
        }
    }
}
