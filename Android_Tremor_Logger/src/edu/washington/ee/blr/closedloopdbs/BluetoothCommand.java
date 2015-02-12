package edu.washington.ee.blr.closedloopdbs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/* This class is a wrapper for three static objects: The Bluetooth adapter, device, and socket that must be kept
 * alive through the creation/destruction process of the application's Activities and Fragments.
 * 
 * */

public class BluetoothCommand {
	
	private static BluetoothDevice selectedDevice;
	private static BluetoothSocket socket;
	private static BluetoothAdapter btAdapter;
	
	public static BluetoothSocket getSelectedSocket() {
		return socket;
	}
	public static void setSelectedSocket(BluetoothSocket selectedSocket) {
		BluetoothCommand.socket = selectedSocket;
	}

	
	public static BluetoothAdapter getBtAdapter() {
		return btAdapter;
	}
	public static void setBtAdapter(BluetoothAdapter btAdapter) {
		BluetoothCommand.btAdapter = btAdapter;
	}


	
	public static BluetoothDevice getSelectedDevice() {
		return selectedDevice;
	}
	public static void setSelectedDevice(BluetoothDevice dev) {
		selectedDevice = dev;
	}
	
}
