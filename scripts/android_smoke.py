#!/usr/bin/env python3
"""Run only on an expendable emulator: installs fixtures and enables the test service."""
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

OUT = pathlib.Path('smoke-results')
OUT.mkdir(exist_ok=True)

def adb(*args, binary=False):
    result = subprocess.run(['adb', *args], check=True, capture_output=True)
    return result.stdout if binary else result.stdout.decode('utf-8', errors='replace')

def shell(*args):
    return adb('shell', *args)

def ui():
    shell('uiautomator', 'dump', '/sdcard/drivemod-ui.xml')
    return ET.fromstring(shell('cat', '/sdcard/drivemod-ui.xml'))

def find_node(tree, value, attr='content-desc'):
    return next((n for n in tree.iter('node') if value in n.attrib.get(attr, '') and n.attrib.get('bounds', '') != '[0,0][0,0]'), None)

def tap(node):
    assert node is not None, 'UI element not found'
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.attrib['bounds']))
    shell('input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def choose_app(card, label):
    tap(find_node(ui(), card))
    time.sleep(1)
    tree = ui()
    node = find_node(tree, label)
    if node is None:
        search = next(n for n in tree.iter('node') if n.attrib.get('class') == 'android.widget.EditText')
        tap(search)
        shell('input', 'text', 'DriveMod')
        time.sleep(.5)
        tree = ui()
        node = find_node(tree, label)
    tap(node)
    time.sleep(.5)

try:
    adb('wait-for-device')
    for apk in ['app/build/outputs/apk/debug/app-debug.apk', 'testnav/build/outputs/apk/debug/testnav-debug.apk', 'testmusic/build/outputs/apk/debug/testmusic-debug.apk']:
        adb('install', '-r', apk)
    shell('settings', 'put', 'system', 'accelerometer_rotation', '0')
    shell('settings', 'put', 'system', 'user_rotation', '0')
    shell('am', 'start', '-W', '-n', 'ru.drivemod/.MainActivity')
    time.sleep(1)
    shell('uiautomator', 'dump', '/sdcard/drivemod-ui.xml')
    initial = shell('cat', '/sdcard/drivemod-ui.xml')
    assert 'DriveMod' in initial and 'Выбрать навигацию' in initial and 'Выбрать музыку' in initial, initial
    (OUT / 'home.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    choose_app('Выбрать навигацию', 'DriveMod Test Navigation')
    choose_app('Выбрать музыку', 'DriveMod Test Music')
    tree = ui()
    button = find_node(tree, 'Запустить выбранную пару приложений')
    if button is None:
        shell('input', 'swipe', '540', '1450', '540', '650', '350')
        tree = ui()
        button = find_node(tree, 'Запустить выбранную пару приложений')
    assert button is not None, 'Start-pair button not visible'
    shell('settings', 'put', 'secure', 'enabled_accessibility_services', 'ru.drivemod/ru.drivemod.DriveAccessibilityService')
    shell('settings', 'put', 'secure', 'accessibility_enabled', '1')
    time.sleep(2)
    # Do not run uiautomator again after enabling the service: its temporary UIAutomation
    # would suppress other accessibility services. Tap the button recorded above instead.
    tap(button)
    dump = ''
    for _ in range(30):
        dump = shell('dumpsys', 'activity', 'service', 'ru.drivemod/.DriveAccessibilityService')
        if 'phase=ACTIVE' in dump and 'pairVisible=true' in dump:
            break
        time.sleep(.6)
    (OUT / 'service.txt').write_text(dump)
    assert 'phase=ACTIVE' in dump and 'pairVisible=true' in dump, dump
    height = re.search(r'dockHeight=(\d+)', dump)
    assert height and int(height[1]) > 0 and 'dockSlots=4' in dump, dump
    (OUT / 'split.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    print('PASS: native home screen, installed-app picker for both roles, two separately installed applications in split-screen, accessibility service and four-slot dock.')
finally:
    (OUT / 'logcat.txt').write_text(shell('logcat', '-d', '-t', '1500'))
