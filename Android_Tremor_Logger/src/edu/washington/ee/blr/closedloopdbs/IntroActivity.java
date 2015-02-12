package edu.washington.ee.blr.closedloopdbs;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/* This class just displays the introductory page with the UW logo
 * */

public class IntroActivity extends Activity {

	Button mBeginButton; //begin button
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_intro);
		
        //Set up BEGIN button
        mBeginButton = (Button)findViewById(R.id.do_button);
        mBeginButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//check if phone has Bluetooth, only proceed if that is so:
				BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
				if (mAdapter == null) {
					Toast.makeText(IntroActivity.this, "This phone does not have Bluetooth, and cannot use this application!",
							Toast.LENGTH_LONG).show();
				} else {
					//Save adapter in Bluetooth static object and start DataOutputActivity
					BluetoothCommand.setBtAdapter(mAdapter);
					Intent i = new Intent(IntroActivity.this, DataOutputActivity.class);
					startActivity(i);
				}
				
			}
		});
		
	}

}
