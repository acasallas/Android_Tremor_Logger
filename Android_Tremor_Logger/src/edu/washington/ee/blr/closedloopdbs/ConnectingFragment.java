package edu.washington.ee.blr.closedloopdbs;

import java.io.IOException;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;

/*
 * This class represents the dialog that shows up and says 'Connecting...' when a user is trying to connect
 * to a Bluetooth device. The thread class is defined as an inner class.
 * */

public class ConnectingFragment extends DialogFragment {
	
	Activity parent;
	Callbacks parentInterfacer;
	BTConnectTask mBTThread;
	
	public interface Callbacks {
		void onConnectionFailed();
	}
	
	@SuppressLint("InflateParams")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		parent = getActivity();
		parentInterfacer = (Callbacks)parent;
		View v = parent.getLayoutInflater().inflate(R.layout.dialog_connecting, null);
		
		//start AsyncTask to connect to selected Bluetooth device
		mBTThread = new BTConnectTask();
		mBTThread.execute();
		
		//display 'Connecting' dialog while this is happening
		return new AlertDialog.Builder(getActivity()).setTitle("Bluetooth Setup").setView(v)
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mBTThread.cancel(true);
								cancelConnectionAttempt();
							}
						}).create();
		
	}
	


	private void cancelConnectionAttempt() {
		parentInterfacer.onConnectionFailed();
		dismiss();
	}
	

	/* Inner BTConnectTask class inherited from AsyncTask, used to connect to Bluetooth:
	 * 
	 * */
	
	public class BTConnectTask extends AsyncTask<Void, Void, BluetoothSocket> {

		@Override
		protected BluetoothSocket doInBackground(Void...params) {
			
			//Check currently selected Bluetooth Socket. If it is not null, close it. Otherwise, connection
			//attempt will fail.
			BluetoothSocket btSocket = BluetoothCommand.getSelectedSocket();
			if (btSocket != null) {
	        	try {
	                btSocket.close();
	            } catch (IOException closeException) {
	            	//do nothing, eventually user will get a message saying Connection Failed
	            }
	        }

	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            //Use public UUID, although probably any UUID would have worked with the serial board bluetooth of the IMU watch.
	            btSocket = BluetoothCommand.getSelectedDevice().createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
	        } catch (IOException e) {
            	//do nothing, eventually user will get a message saying Connection Failed
	        }
	        
	        // Cancel discovery because it will slow down the connection
	        BluetoothCommand.getBtAdapter().cancelDiscovery();
	        
	        //Begin connection. This is the real blocker and why we run this in a separate thread.
	        try {
	        	btSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	            	btSocket.close();
	            } catch (IOException closeException) { 
	            	//do nothing
	            }
	            //If connection failed, return null. The next part of this class will send user a message
	            //of failure.
	            return null;
	        }
	        
			return btSocket;
		}
		
		
		@Override
		protected void onCancelled(BluetoothSocket mSocket) {
			//called if user clicks 'Cancel' button.
			if (mSocket != null) {
				try {
		             mSocket.close();
		        } catch (IOException e) {
		        	//do nothing, user already knows connection is not going to be established.
		        }
			}
		}
		
		@Override
		protected void onPostExecute(BluetoothSocket mSocket) {
			//if connection failed, display failed message, otherwise save socket in BluetoothCommand static
			//object and return success message!
			if (mSocket == null) {
				cancelConnectionAttempt();
			} else {
				BluetoothCommand.setSelectedSocket(mSocket);
				parent.setResult(Activity.RESULT_OK);
				parent.finish();
			}
		}
		
		

	}
	
	
	

}
