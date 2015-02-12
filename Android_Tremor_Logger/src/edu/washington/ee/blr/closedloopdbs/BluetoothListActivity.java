package edu.washington.ee.blr.closedloopdbs;


import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/*
 * This class is the page that displays the list of paired Bluetooth devices to connect with.
 * It assumes the Bluetooth Adapter was already set. (It will always be set if phone has Bluetooth, and
 * application is designed not to go past first page if phone does not have Bluetooth).
 * 
 * This class contains an inner class which defines a custom adapter for the included ListView.
 * 
 * */

public class BluetoothListActivity extends FragmentActivity implements ConnectingFragment.Callbacks {

	//Fragment IDs
	private final String CONNECTING_DIALOG = "connecting_dialog";

	//UI Elements
	private ListView mPairedDeviceListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);
		
		//Get a list of paired Devices using Bluetooth Adapter.
		ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>(BluetoothCommand.getBtAdapter().getBondedDevices());
		
		//set up ListView (requires use of a 'List View Adapter' which I custom define, see Android docs for more
		//info on the use of ListView
		mPairedDeviceListView = (ListView) findViewById(R.id.paired_dev_list_view);
		BTListAdapter listViewAdapter = new BTListAdapter(pairedDevices);
		mPairedDeviceListView.setAdapter(listViewAdapter);
		mPairedDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
				//Get selected BluetoothDevice
				BluetoothDevice btDev = (BluetoothDevice) mPairedDeviceListView.getAdapter().getItem(position);
				//save it to Static Object for use in dialog box which is to be opened.
				BluetoothCommand.setSelectedDevice(btDev);
				//open dialog box
				FragmentManager fm = getSupportFragmentManager();
				ConnectingFragment dialog = new ConnectingFragment();
				dialog.show(fm, CONNECTING_DIALOG);
			}
		});
			
	}
	
	//method called by dialog box when unable to connected to selected Bluetooth device
	@Override
	public void onConnectionFailed() {
		BluetoothCommand.setSelectedDevice(null);
		Toast.makeText(this, "Could not establish connection.", Toast.LENGTH_LONG).show();
		
	}
	
	
	
	//Inner class to define custom ListView adapter
	private class BTListAdapter extends ArrayAdapter<BluetoothDevice> {
		
		public BTListAdapter(ArrayList<BluetoothDevice> devices) {
			super(BluetoothListActivity.this, 0, devices);
		}
		
		@SuppressLint("InflateParams")
		@Override 
		public View getView( int position, View convertView, ViewGroup parent) {
			// If we weren't given a view, inflate one 
			if (convertView == null) { 
				convertView = getLayoutInflater().inflate( R.layout.list_item, null); 
				} 
			
			//get the bluetooth item for this getView() call.
			BluetoothDevice btDevice = getItem(position);
			
			//set name
			TextView nameTextView = (TextView) convertView.findViewById(R.id.list_item_textview);
			nameTextView.setText(btDevice.getName() + "\n" + btDevice.getAddress());
			
			return convertView;
		}
		
	}






	
	
	
	
	
}
