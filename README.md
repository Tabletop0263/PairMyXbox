# PairMyXbox
Xbox BLE controller -> Android phone -> UDP -> Wii (libogc)

1. Push to GitHub; Actions builds `padbridge-apk` and `netpad-wii`.
2. Wii: copy `apps/netpad/` to the SD card, launch from HBC. It prints its IP.
3. Phone: pair the Xbox controller, install the APK, enter the Wii IP, Start.
4. Keep PadBridge in the foreground (Android only delivers gamepad input to the focused app).

Use `wii/source/netpad.{c,h}` in your own app: `netpad_init()` once, `netpad_poll()` per frame.
