package com.eyuanchuang.bleespdht11.model
import android.bluetooth.BluetoothDevice

//蓝牙列表类，蓝牙设备列表
data class BleListDevice(var bluetoothDevice: BluetoothDevice, var name: String, var macNumber: String, var rssi: Int)
//data class bleListDeviceTest(var name:String,var macNumber: String,var rssi:Int) 测试用