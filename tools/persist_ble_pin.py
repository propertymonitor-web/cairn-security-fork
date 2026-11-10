#!/usr/bin/env python3
"""Bluetooth PIN that survives restarts.
Stock MeshCore picks a NEW random pairing PIN every time a companion radio with a screen starts, so any restart breaks the phone's saved pairing.
This patch generates a random PIN once per device (hardware RNG), stores it in NVS and reuses it at every restart ("set pin" still overrides it). It is NOT derived from the MAC or chip ID.
Usage: python3 tools/persist_ble_pin.py --meshcore <MeshCore checkout>.  Safe to run twice; builds without Bluetooth or a screen are left alone."""
import argparse, os, sys

OLD = ("      StdRNG rng;\n"
       "      the_mesh.setBLEPin(rng.nextInt(100000, 999999)); // random pin each session\n")
NEW = ("      StdRNG rng;\n"
       "#if defined(ESP32)\n"
       "      // LODESTAR: random PIN generated ONCE per device from the hardware RNG and kept in NVS (never derived from the MAC or any public value)\n"
       "      { Preferences ls_p; ls_p.begin(\"ls_ble\", false); uint32_t ls_pin = ls_p.getUInt(\"pin\", 0);\n"
       "        if (ls_pin < 100000 || ls_pin > 999999) { ls_pin = 100000 + esp_random() % 900000; ls_p.putUInt(\"pin\", ls_pin); }\n"
       "        ls_p.end(); the_mesh.setBLEPin(ls_pin); }\n"
       "#else\n"
       "      the_mesh.setBLEPin(rng.nextInt(100000, 999999)); // random pin each session\n"
       "#endif\n")

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--meshcore", required=True); a = ap.parse_args()
    p = os.path.join(a.meshcore, "examples", "companion_radio", "main.cpp")
    if not os.path.isfile(p):
        print("persist_ble_pin: no companion_radio/main.cpp here, nothing to do"); return
    t = open(p).read()
    if "generated ONCE per device" in t:
        print("persist_ble_pin: already applied"); return
    if t.count(OLD) != 1:
        print("persist_ble_pin: WARNING - upstream MeshCore changed the PIN code; the PIN patch did NOT apply. Update tools/persist_ble_pin.py.", file=sys.stderr); sys.exit(1)
    open(p, "w").write(t.replace(OLD, NEW)); print("persist_ble_pin: Bluetooth PIN is now random per device, stored in NVS")

if __name__ == "__main__":
    main()
