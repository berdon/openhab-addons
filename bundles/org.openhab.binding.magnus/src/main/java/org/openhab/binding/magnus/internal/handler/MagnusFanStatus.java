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

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Helper class for tracking a Fan status.
 *
 * @author Austin Hanson - Initial contribution
 */
public class MagnusFanStatus {
    @SerializedName("power")
    public Boolean Power;
    @SerializedName("speed")
    public Integer Speed;
    @SerializedName("oscillate")
    public Boolean Oscillate;
    @SerializedName("oscillate_speed")
    public Integer OscillateSpeed;
    @SerializedName("timer")
    public Integer Timer;

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MagnusFanStatus)) {
            return false;
        }
        MagnusFanStatus magnusFanStatus = (MagnusFanStatus) o;
        return Power == magnusFanStatus.Power && Speed.intValue() == magnusFanStatus.Speed.intValue()
                && Oscillate == magnusFanStatus.Oscillate
                && OscillateSpeed.intValue() == magnusFanStatus.OscillateSpeed.intValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(Power, Speed.intValue(), Oscillate, OscillateSpeed.intValue());
    }

    @Override
    public String toString() {
        return "{ \"Power\": " + Power + ", \"Speed\": " + Speed + ", \"Oscillate\": " + Oscillate
                + ", \"OscillateSpeed\": " + OscillateSpeed + " }";
    }
}
