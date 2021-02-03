# Magnus Devices

Random assortment of custom devices.

This binding supports:

* Customer Fan controller using an ESP8266

## Auto Discovery

The binding will automatically discover Magnus controllers using UPnP.

## Binding Configuration

The binding requires no special configuration

## Manual Thing Configuration

The Magnus binding requires an authentication key to protect writable controls.

## Channels

### Fan Controller

The fan controller supports the following channels:

| Channel Type ID                | Item Type |
|--------------------------------|-----------|
| fan_controller-power           | Switch    |
| fan_controller-speed           | Number    |
| fan_controller-oscillate       | Switch    |
| fan_controller-oscillate_speed | Number    |
| fan_controller-timer           | Number    |
|--------------------------------|-----------|