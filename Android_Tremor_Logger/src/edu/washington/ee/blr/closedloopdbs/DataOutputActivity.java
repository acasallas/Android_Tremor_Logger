package edu.washington.ee.blr.closedloopdbs;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;

enum ActivityWritingMode {NOT_WRITING, READY, WRITING};

/* This class represents the page that displays all the Bluetooth data and is by far the largest page of the program.
 * The page is divided into the following sections:
 * 
 * Object declarations:
 * UI ELEMENTS: TOP SECTION
 * UI ELEMENTS: BOTTOM (OUTPUT) SECTION
 * LOGIC OBJECTS
 * 
 * Method declarations:
 * onCreate() [this is like the constructor for the class]
 * onActivityResult()
 * METHOD SECTION: BLUETOOTH METHODS [for connecting to Bluetooth]
 * METHOD SECTION: WRITING METHODS
 * METHOD SECTION: DATA STREAM [for streaming Bluetooth]
 * METHOD SECTION: GRAPH OPTIONS METHODS
 * METHOD SECTION: LIVE GRAPH UI ELEMENTS SECTION
 * METHOD SECTION: ACTIVITY DESTRUCTION AND CLEANUP
 * 
 * */

public class DataOutputActivity extends FragmentActivity 
	implements GraphOptionsFragment.Callback, WriteFragment.Callback {
	
	/* UI ELEMENTS: TOP SECTION
	 * */
	Button mBTDeviceSelector;
	Button mWriteToFileButton;
	Button mGraphOptionsButton;
	ToggleButton mToggleButton;
	TextView mWriteModeTextView;
	TextView mWriteQuantityTextView;
	TextView mWriteStatusTextView;
	
	/* UI ELEMENTS: BOTTOM (OUTPUT) SECTION
	 * */
	//For Debug Mode:
	TextView mDebugModeTextView;	
	//For Current State Mode:
	TableLayout mCountersTable;
	TextView mAccelOutputX;
	TextView mAccelOutputY;
	TextView mAccelOutputZ;
	TextView mGyroOutputX;
	TextView mGyroOutputY;
	TextView mGyroOutputZ;
	TextView mCompassOutputX;
	TextView mCompassOutputY;
	TextView mCompassOutputZ;	
	//For Live Graph Mode:
	LiveGraphView mLiveGraph;
	LinearLayout mLiveGraphLayout;
	TextView mAccelMaxValue;
	TextView mGyroMaxValue;
	TextView mCompassMaxValue;
	TextView mLiveGraphTimeMeasure1;
	TextView mLiveGraphTimeMeasure2;
	TextView mLiveGraphTimeMeasure3;
	TextView mLiveGraphTimeMeasure4;
	TextView mLiveGraphTimeMeasure5;
	
	//LOGIC OBJECTS: Thread Related	
	BluetoothThread mBluetoothThread;
	Handler mainThreadHandler;
	WriteHandlerThread mWriteThread = null;
	
	//Write To File Objects
	ActivityWritingMode writingMode;
	WriteOptions writeOptions;
	CountDownTimer timer;
	
	//Live Graph Mode objects:
	private GraphOptions graphOptions;
	private int packetListSkip;
	private int packetListCtr;
	
	//Current State Mode objects:
	ArrayList<DataPacket> currentStatePacketList = new ArrayList<DataPacket>();
	static int INT_COUNTER_AVG_AMNT = 10;
	
	//Activity Request Codes & Fragment ID Strings:
	private final int REQUEST_ENABLE_BT = 1;
	private final int BLUETOOTH_SELECTOR = 5;
	public static String BLUETOOTH_SELECTION ="edu.washington.ee.blr.closedloopdbs.btselection";
	public static String GRAPH_OPTIONS_DIALOG ="edu.washington.ee.blr.closedloopdbs.graphoptions";
	public static String WRITE_FILE_DIALOG ="edu.washington.ee.blr.closedloopdbs.writetofile";
	final String IS_STREAMING_KEY = "isStreamingKey";
	final String IS_WRITING_KEY = "isWritingKey";
	final String GRAPH_OPTIONS_KEY = "graphOptionsKey";
	final String WRITE_OPTIONS_KEY = "writeOptionsKey";
	final String DATA_PACKET_LIST_KEY = "dataPacketListKey";


	//onCreate(), the largest method of this file, is basically a constructor initializing all UI elements
	//and attached callback methods
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data_output);
		
		/* Set up Top Section UI Elements:
		 * */
		
		//SET UP: mBTDeviceSelector
		mBTDeviceSelector = (Button)findViewById(R.id.bt_button_1);
        mBTDeviceSelector.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {	
				stopDataStream(); //stop all streaming and writing.
				
				//Check to see if Bluetooth enabled
				if (!BluetoothCommand.getBtAdapter().isEnabled()) {
					promptUserEnableBluetooth();
				} else {
				//If it is, start Activity to select a new device
					Intent i = new Intent(DataOutputActivity.this, BluetoothListActivity.class);
					startActivityForResult(i,BLUETOOTH_SELECTOR);
				}
			}
		});
        
        //SET UP: mWriteToFileButton
        mWriteToFileButton = (Button) findViewById(R.id.write_text_button);
        mWriteToFileButton.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				//The 'Write To File' button writes to external storage. Thus, before starting, check if external storage
				//is available:
				
				if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					FragmentManager fm = getSupportFragmentManager();
					WriteFragment dialog = new WriteFragment();
					dialog.show(fm, WRITE_FILE_DIALOG);
				} else {
					Toast.makeText(DataOutputActivity.this, "No External Storage Detected!", Toast.LENGTH_LONG).show();
				}	
			}
		});
        //SET UP: write mode TextViews
    	mWriteModeTextView = (TextView) findViewById(R.id.textview_write_type);
    	mWriteQuantityTextView = (TextView) findViewById(R.id.textview_write_quantity);
    	mWriteStatusTextView = (TextView) findViewById(R.id.textview_write_status);
        
        //SET UP: mGraphOptionsButton
        mGraphOptionsButton = (Button) findViewById(R.id.graph_options_button);
        mGraphOptionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//stopDataStream();
				FragmentManager fm = getSupportFragmentManager();
				GraphOptionsFragment dialog = new GraphOptionsFragment(graphOptions);
				dialog.show(fm, GRAPH_OPTIONS_DIALOG);
			}
		});
        
        //SET UP: mToggleButton
        mToggleButton = (ToggleButton) findViewById(R.id.toggleButton1);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if ( ((ToggleButton)v).isChecked() ) {
					startWriteThread(false);
					startDataStream();
				} else {
					
					stopDataStream();
				}
			}
		});
        
        /* Set up: BOTTOM (OUTPUT) RELATED UI:
         * */
        
        //SET UP: mBTOutputTextView
        mDebugModeTextView = (TextView) findViewById(R.id.bt_output_textview);
 
        //SET UP: Accel, Gyro, and Compass TextViews and Table
        mCountersTable = (TableLayout) findViewById(R.id.counters_table);
        mAccelOutputX = (TextView) findViewById(R.id.accel_counter_x);
        mAccelOutputY = (TextView) findViewById(R.id.accel_counter_y);
        mAccelOutputZ = (TextView) findViewById(R.id.accel_counter_z);
        mGyroOutputX = (TextView) findViewById(R.id.gyro_counter_x);
        mGyroOutputY = (TextView) findViewById(R.id.gyro_counter_y);
        mGyroOutputZ = (TextView) findViewById(R.id.gyro_counter_z);
        mCompassOutputX = (TextView) findViewById(R.id.compass_counter_x);
        mCompassOutputY = (TextView) findViewById(R.id.compass_counter_y);
        mCompassOutputZ = (TextView) findViewById(R.id.compass_counter_z);
        
        //SET UP: GET MAX VALUE TEXTVIEWS
        mAccelMaxValue = (TextView) findViewById(R.id.textview_max_accel);
        mGyroMaxValue = (TextView) findViewById(R.id.textview_max_gyro);
        mCompassMaxValue = (TextView) findViewById(R.id.textview_max_compass);
        
        //SET UP: LIVE GRAPH
    	mLiveGraphTimeMeasure1 = (TextView) findViewById(R.id.textview_graphtime_1);
    	mLiveGraphTimeMeasure2 = (TextView) findViewById(R.id.textview_graphtime_2);
    	mLiveGraphTimeMeasure3 = (TextView) findViewById(R.id.textview_graphtime_3);
    	mLiveGraphTimeMeasure4 = (TextView) findViewById(R.id.textview_graphtime_4);
    	mLiveGraphTimeMeasure5 = (TextView) findViewById(R.id.textview_graphtime_5);
        mLiveGraph = (LiveGraphView) findViewById(R.id.live_graph);
        mLiveGraphLayout = (LinearLayout) findViewById(R.id.live_graph_layout);

        
        /* Multi-Threading setup:
         * */
		//SET UP: mainThreadHandler
        /* This block contains the code the main thread uses to handle all messages other threads send it.
         * */
		mainThreadHandler = new Handler(Looper.getMainLooper()) {
			@Override 
			public void handleMessage(Message msg) { 
				//Code block executed for each Bluetooth read:
				if (msg.what == BluetoothThread.MESSAGE_READ) { 
					
					ThreadNugget nugget = (ThreadNugget)msg.obj;
					
					//if writing to a text file is active, get raw data and write to a text file first
					if (writingMode == ActivityWritingMode.WRITING) {
						
						byte[] byteData = nugget.getRawData();
						
						//if BYTES mode is on, keep track of how many bytes you have written
						if (writeOptions.mode == WriteOptions.WriteMode.BYTES) {
							int newBytesLeft = writeOptions.quantity - byteData.length;
							if (newBytesLeft > 0) {
								writeOptions.quantity = newBytesLeft;
								mWriteQuantityTextView.setText(String.valueOf(newBytesLeft));
								mWriteThread.writeBytes(byteData);
							} else {
								byte[] copyBuffer = new byte[byteData.length+newBytesLeft];
								System.arraycopy(byteData, 0, copyBuffer, 0, byteData.length+newBytesLeft);

								mWriteThread.writeBytes(copyBuffer);
								endWritingOperations(false);
							}
						} else {
							mWriteThread.writeBytes(byteData);
						}
						
					}
					
					//now that writing has been done, move to display. Display DataPacket[] data based on 
					//graphOptions mode
					switch(graphOptions.optionType) {
						//in debug mode, simply display rawData in mDebugModeTextView
						case DEBUG_MODE:
							byte[] rawData = nugget.getRawData();
							
					    	String newString = new String();
							try {
								newString = new String(rawData,"UTF8");
							} catch (UnsupportedEncodingException e) {
								Toast.makeText(DataOutputActivity.this, "Having trouble displaying stream as ASCII", Toast.LENGTH_SHORT).show();
							}
							
							mDebugModeTextView.setText( newString );
							break;
						//in current state mode, display an integer that represents the average of all the packets
						//held in currentStatePacketList (currently set to hold max. 10 packets). It's averaged
						//to reduce noise.
						case CURRENT_STATE_MODE:
	
							DataPacket[] dataArray = nugget.getDataPackets();
	
							DataPacket.addArrayToListWithinMax(dataArray, currentStatePacketList, INT_COUNTER_AVG_AMNT);
							
							//get one DataPacket representing the average of currentStatePacketList
							DataPacket avgPacket = DataPacket.findAvgOfDataPackets(currentStatePacketList);
							
							//set DataPacket averages on the nine TextViews used in Current State Mode:
							mAccelOutputX.setText( String.format("%.2f", avgPacket.getAccel().getX()) );
							mAccelOutputY.setText( String.format("%.2f", avgPacket.getAccel().getY()) );
							mAccelOutputZ.setText( String.format("%.2f", avgPacket.getAccel().getZ()) );
							mGyroOutputX.setText( String.valueOf( (int) avgPacket.getGyro().getX()  ) );
							mGyroOutputY.setText( String.valueOf( (int)  avgPacket.getGyro().getY()  ) );
							mGyroOutputZ.setText( String.valueOf( (int) avgPacket.getGyro().getZ() ) );
							mCompassOutputX.setText( String.valueOf( (int)  avgPacket.getComp().getX()  ) );
							mCompassOutputY.setText( String.valueOf( (int)  avgPacket.getComp().getY()  ) );
							mCompassOutputZ.setText( String.valueOf( (int)  avgPacket.getComp().getZ()  ) );
							
							break;
						
						//mode used to display DataPackets in graphical form
						case LIVE_GRAPH_MODE:
						
							//if user has selected to display more packets than the maximum RESOLUTION of the
							//LiveGraph object, then we skip a certain number of packets before updating the
							//packetlist held in the LiveGraph object, so the user still 'sees' the entire lenght
							//of data, but doesn't overload the LiveGraph object. (The RESOLUTION field is in effect
							//a maximum that limits how many packets the LiveGraph object should hold).
							if (packetListSkip > 1) {
								DataPacket[] packetsFromBluetooth = nugget.getDataPackets();
								int newArraySize = packetsFromBluetooth.length/packetListSkip;
								if ((packetsFromBluetooth.length%packetListSkip) >= packetListCtr ) newArraySize++;
								
								DataPacket[] newArray = new DataPacket[newArraySize];
								int i = 0;
								for (int n = 0; n < packetsFromBluetooth.length; n++) {
									packetListCtr--;
									if (packetListCtr == 0) {
										newArray[i] = packetsFromBluetooth[n];
										packetListCtr = packetListSkip;
										i++;
									}
								}
								mLiveGraph.addPacketsToList(newArray);
							} else {
								mLiveGraph.addPacketsToList(nugget.getDataPackets());
							}
							break;
					}
					
				//this message is received if Bluetooth can no longer communicate. The connection will be killed and
				//the user will have to reconnect	
				} else if (msg.what == BluetoothThread.BLUETOOTH_DEAD) {
					cancelBluetoothStream();
					
				//message received when writing thread has finished writing. Main functionality is to remove the
				//'finishing up writing' message from appearing in the UI.
				} else if (msg.what == WriteHandlerThread.TELL_MAIN_WRITING_DONE) {
					//check to see if current writing thread is the one sending this message in case user restarts
					//writing functionality really fast and we in fact have a different thread doing the writing.
					if (mWriteThread != null)
						if (mWriteThread.getThreadId() != msg.arg1)
							return;
					mWriteStatusTextView.setText("");
					
				//this message is received when an exception is thrown in the WriteHandlerThread and writing
				//can no longer be done
				} else if (msg.what == WriteHandlerThread.CAN_NO_LONGER_WRITE) {
					if (mWriteThread != null)
						if (mWriteThread.getThreadId() != msg.arg1)
							return;
					endWritingOperations(true);
					Toast.makeText(DataOutputActivity.this, "Had to stop writing to file! Check space.", Toast.LENGTH_LONG).show();
				}
			}
			
		};
		
		/* Set State: Attempt to restore state from SavedInstance Bundle. If null, then set default settings.
		 * */
        if (savedInstanceState != null) {
        	mLiveGraph.setRestoredPacketList(savedInstanceState.<DataPacket>getParcelableArrayList(DATA_PACKET_LIST_KEY) );
        	//pull graphOptions
        	graphOptions = (GraphOptions)savedInstanceState.getParcelable(GRAPH_OPTIONS_KEY);
        	//pull writeOptions
        	writeOptions = (WriteOptions)savedInstanceState.getParcelable(WRITE_OPTIONS_KEY);
        	writingMode = (ActivityWritingMode)savedInstanceState.getSerializable(IS_WRITING_KEY);
        	switch(writingMode) {
        	case NOT_WRITING:
        		//do nothing
        		break;
        	case READY:
        		setUIWriteElementsToReady();
        		break;
        	case WRITING:
        		setUIWriteElementsToReady();
        		startWriteThread(true);
        		break;
        	}
        	//pull isStreaming
        	if (savedInstanceState.getBoolean(IS_STREAMING_KEY)) {
        		mToggleButton.setChecked(true);
        		startDataStream();
        	}
        } else {
        	//set graphOptions
            graphOptions = new GraphOptions();
            writingMode = ActivityWritingMode.NOT_WRITING;
            writeOptions = new WriteOptions();
            //by default isWriting is false
            //by default, isStreaming is false
        }
        
        
        //Set the display output mode and check what, if any, Bluetooth device has been selected. These methods have
        //UI implications.
        setOutputMode(graphOptions);
        verifyBluetoothStatus();

	}

	@Override 
	protected void onActivityResult( int requestCode, int resultCode, Intent data) { 
		
		 switch(requestCode) {
		 case REQUEST_ENABLE_BT:
			 if (resultCode == RESULT_OK) {
				 displayUISelectedBluetoothDevice();
			 }
			 break;
		 case BLUETOOTH_SELECTOR:
			 if (resultCode == RESULT_OK) {
					Toast.makeText(this, "Connection Established!", Toast.LENGTH_LONG).show();
			 }
			 displayUISelectedBluetoothDevice();
			 break;
		 }
	}
	
	//METHOD SECTION: BLUETOOTH METHODS
	
	//checks to see if Bluetooth enabled and what device is selected.
	void verifyBluetoothStatus() {
		if (!BluetoothCommand.getBtAdapter().isEnabled()) {
			promptUserEnableBluetooth();
		} else {
			displayUISelectedBluetoothDevice();
		}
	}
	
	//opens 'Please enable Bluetooth on your phone' prompt
	private void promptUserEnableBluetooth() {
		mBTDeviceSelector.setText("Enable Bluetooth");
		enableGraphTextAndStreamButtons(false);
	    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	}
	
	//sets relevant UI features to show selected Bleutooth device
	private void displayUISelectedBluetoothDevice() {
		BluetoothDevice selectedDevice = BluetoothCommand.getSelectedDevice();
		if (selectedDevice == null) {
			enableGraphTextAndStreamButtons(false);
			mBTDeviceSelector.setText("Select a Bluetooth Device...");
		} else {
			enableGraphTextAndStreamButtons(true);
			mBTDeviceSelector.setText(selectedDevice.getName());
		}
		
	}
	
	private void enableGraphTextAndStreamButtons(boolean isEnabled) {
		
		if (isEnabled) {
			mGraphOptionsButton.setEnabled(true);
			mWriteToFileButton.setEnabled(true);
			mToggleButton.setEnabled(true);
		} else {
			mGraphOptionsButton.setEnabled(false);
			mWriteToFileButton.setEnabled(false);
			mToggleButton.setEnabled(false);
		}
	}
	
	//method to call when Bluetooth UNEXPECTANTLY ends (signaling a reconnect to Bluetooth device is necessary)
	void cancelBluetoothStream() {
		stopDataStream(); //stops Bluetooth data stream and Write threads if they are running
		Toast.makeText(DataOutputActivity.this, "There was a problem with the Bluetooth stream. Please reconnect", Toast.LENGTH_LONG).show();
		BluetoothCommand.setSelectedSocket(null); //set socket to null to force user to reselect device
		BluetoothCommand.setSelectedDevice(null); //same for device
		verifyBluetoothStatus(); //let this method take care of necessary UI changes
	}
	
	//METHOD SECTION: WRITING METHODS:
	
	void startWriteThread(boolean isAppend) {
		if (writingMode == ActivityWritingMode.NOT_WRITING) return; //nothing to do if user hasn't chosen to write yet
		writingMode = ActivityWritingMode.WRITING;
		
		//creating new Writing Thread attempts file open, which is caught if file open fails.
		try {
			mWriteThread = new WriteHandlerThread(mainThreadHandler,writeOptions.filename, isAppend, this);
		} catch (FileNotFoundException e) {
			mWriteThread = null;
			endWritingOperations(true);
			Toast.makeText(this, "Unable to open the text File!", Toast.LENGTH_LONG).show();
			return;
		}
		
		//if creation of Writing thread was successful, start thread and inform user
		mWriteStatusTextView.setText("Writing to file...");
		
		//if TIME mode is one, create a timer and set up timer callbacks for each second
		if (writeOptions.mode == WriteOptions.WriteMode.TIME) {
			timer = new CountDownTimer(writeOptions.quantity * 1000,1000) {
				@Override
				public void onTick(long millisUntilFinished) {
					writeOptions.quantity = (int)(millisUntilFinished/1000L);
					mWriteQuantityTextView.setText(String.valueOf( millisUntilFinished/1000 ));
				}
				@Override
				public void onFinish() {
					endWritingOperations(true);
				}
			}.start();
		}
		
		mWriteThread.start();
		mWriteThread.getLooper(); //block until looper has been started
	}
	
	void endWritingOperations(boolean forceQuit) {
		writingMode = ActivityWritingMode.NOT_WRITING;
		mWriteModeTextView.setText("");
		mWriteQuantityTextView.setText("");
		
		//set mWriteThread to null once it has quit to indicate writing operation has ceased
		if (mWriteThread != null) {
			if (forceQuit)
				mWriteThread.stopWritingForcefully();
			else
				mWriteThread.stopWritingPeacefully();
			
			mWriteThread = null;
			mWriteStatusTextView.setText("Finishing Writing...");
		} else {
			mWriteStatusTextView.setText("");
		}
		
		//kill timer that may have been started if Write was in TIME mode
		if (timer != null) {
			timer.cancel();
		}
	}
	
	//updates UI Elements to when user has selected writing options but has not yet started writing
	void setUIWriteElementsToReady() {
		mWriteStatusTextView.setText("Press ON to start write...");
		switch(writeOptions.mode) {
		case BYTES:
			mWriteModeTextView.setText("BYTES");
			mWriteQuantityTextView.setText(String.valueOf(writeOptions.quantity));
			break;
		case TIME:
			mWriteQuantityTextView.setText(String.valueOf(writeOptions.quantity));
			mWriteModeTextView.setText("SECONDS");
			break;
		case CONTINUOUS:

			mWriteModeTextView.setText("");
			break;
		}
	}
	
	//called when user presses OK in writing options dialog
	@Override
	public void onWriteDialogOK(WriteOptions options) {
		endWritingOperations(true);
		stopDataStream();
		mToggleButton.setChecked(false);
		
		writeOptions = options;
		writingMode = ActivityWritingMode.READY;
		setUIWriteElementsToReady();
	}

	//called when user presses CANCEL in writing options dialog
	@Override
	public void onWriteDialogCANCEL() {
		endWritingOperations(true);
	}
	
	//METHOD SECTION: DATA STREAM
	//startDataStream() starts Bluetooth streaming only (not writing operations)
	void startDataStream() {
		if (graphOptions.optionType == GraphOptions.GraphOptionType.CURRENT_STATE_MODE)
			currentStatePacketList.clear(); //to avoid having values from last stream entered into averages
		
		if (BluetoothCommand.getSelectedSocket() != null && mBluetoothThread == null) {
			try {
				mBluetoothThread = new BluetoothThread(BluetoothCommand.getSelectedSocket(),mainThreadHandler);
			} catch (IOException e) {
				cancelBluetoothStream();
			}
			
			if (mBluetoothThread!=null)
				mBluetoothThread.start();
		}
	}
	
	//stopDataStream() stops both Bluetooth and writing operations (will not crash if writing was not going on)
	void stopDataStream() {
		if (mToggleButton.isChecked()) mToggleButton.setChecked(false);
		endWritingOperations(true);
		stopBluetoothThread();
	}
	
	//stop BluetoothThread() stops Bluetooth stream only
	void stopBluetoothThread() {
		if (mBluetoothThread != null) {
			mBluetoothThread.stopThread();
			boolean keepTrying = true;
			while (keepTrying) {
				try {
				mBluetoothThread.join();
				keepTrying = false;
				} catch (InterruptedException e) {}
			}
			mBluetoothThread = null;
		}
	}
	
	//METHOD SECTION: GRAPH OPTIONS METHODS

	//method called when user finishes with Graph Options Dialog box
	@Override
	public void onOptionsChosen(GraphOptions chosenOptions) {
		if (chosenOptions.packetsToShow < 3) {
			chosenOptions.packetsToShow = 3;
			Toast.makeText(this, "Packets to show set to 3", Toast.LENGTH_SHORT).show();
		}
		
		graphOptions = chosenOptions;
		
		setOutputMode(graphOptions);
	}
	
	//method to change UI display depending on selected user 'graph' options
	void setOutputMode(GraphOptions gOptions) {

		switch(gOptions.optionType) {
		case DEBUG_MODE :
			mDebugModeTextView.setVisibility(View.VISIBLE);
			mCountersTable.setVisibility(View.INVISIBLE);
			mLiveGraphLayout.setVisibility(View.INVISIBLE);
			break;
		case CURRENT_STATE_MODE:

			mDebugModeTextView.setVisibility(View.INVISIBLE);
			mCountersTable.setVisibility(View.VISIBLE);
			mLiveGraphLayout.setVisibility(View.INVISIBLE);
			break;
		case LIVE_GRAPH_MODE:
			setLiveGraphUIElements(gOptions); //this method does plenty to prepare the UI of the LiveGraph
			mDebugModeTextView.setVisibility(View.INVISIBLE);
			mCountersTable.setVisibility(View.INVISIBLE);
			mLiveGraphLayout.setVisibility(View.VISIBLE);
			
			break;
		}
	}
	
	//METHOD SECTION: LIVE GRAPH UI ELEMENTS SECTION
	
	void setLiveGraphUIElements(GraphOptions gOptions) {
		//meant for Live Display Mode, sets number of packets to skip when sending packets to the Live Graph
		setGraphCtr(gOptions);
		mLiveGraph.setGraphOptions(gOptions); //inform the Live Graph of the newly selected Options
		
		//set measurements at the tops of the Live Graph
		float timeMax = (float)gOptions.packetsToShow / (float)DataPacket.PACKETS_PER_SEC;
		mLiveGraphTimeMeasure5.setText(getGraphTickerString(0f, 4f, timeMax));
		mLiveGraphTimeMeasure4.setText(getGraphTickerString(1f, 4f, timeMax));
		mLiveGraphTimeMeasure3.setText(getGraphTickerString(2f, 4f, timeMax));
		mLiveGraphTimeMeasure2.setText(getGraphTickerString(3f, 4f, timeMax));
		mLiveGraphTimeMeasure1.setText(getGraphTickerString(4f, 4f, timeMax));
		//sets the maximum values for Live Graph Mode
		setTextViewMaxValues(gOptions);
	}
	
	void setTextViewMaxValues(GraphOptions graphOptions) {
		if (graphOptions.isMagnitudeOn) {
			mAccelMaxValue.setText(String.format("Max: %.2f g", DataPacket.getAccelMagMax()));
			mGyroMaxValue.setText(String.format("Max: %.0f deg/sec", DataPacket.getGyroMagMax()));
			mCompassMaxValue.setText(String.format("Max: %.0f uT", DataPacket.getCompassMagMax()));
			
		} else {
			mAccelMaxValue.setText(String.format("Max: %.2f g", DataPacket.ACCEL_MAX));
			mGyroMaxValue.setText(String.format("Max: %.0f deg/sec", DataPacket.GYRO_MAX));
			mCompassMaxValue.setText(String.format("Max: %.0f uT", DataPacket.COMP_MAX));
		}
	}
	
	int setGraphCtr(GraphOptions options) {
		/*The reason we are keeping a packetList is to have a large stream of data to display in Live Graph mode.
		 * Live Graph mode allows the user to select how many packets the want to display (in effect choosing how much 
		 * time to show because 100 packets = 1 second). The problem is if they want to display a long time, like minutes
		 * or even one hour (one hour = 360,000 packets) we'd have an ArrayList of 360,000 items. We'd also be having the
		 * Live Graph draw 360,000 datapoints on each onDraw() call. To mitigate this, we introduce the algorithm below,
		 * which only selects every few datapackets to save to keep the ArrayList at around MAX_PACKETS_TO_SAVE but
		 * also displaying the amount of time the user wants, up to an hour.
		 * */
		packetListSkip = options.packetsToShow/ LiveGraphView.RESOLUTION;
		if (options.packetsToShow%LiveGraphView.RESOLUTION != 0)
			packetListSkip += 1;
		int newPacketListMax = graphOptions.packetsToShow/packetListSkip;
		if (graphOptions.packetsToShow%packetListSkip != 0)
			newPacketListMax += 1;
		packetListCtr = packetListSkip;
		mLiveGraph.setPacketListMax(newPacketListMax);
		return newPacketListMax;
	}

	String getGraphTickerString(float pos, float max, float sec) {
		float time = sec * (pos/max);
		char unit = 's';
		if (time > 60) {
			time = time/60f;
			unit = 'm';
		}
		return String.format(Locale.US, "%.2f %c", time,unit);
	}

	//METHOD SECTION: ACTIVITY DESTRUCTION AND CLEANUP
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		
		savedInstanceState.putBoolean(IS_STREAMING_KEY,mBluetoothThread != null);
		savedInstanceState.putSerializable(IS_WRITING_KEY, writingMode);
		savedInstanceState.putParcelable(GRAPH_OPTIONS_KEY, graphOptions);
		savedInstanceState.putParcelable(WRITE_OPTIONS_KEY, writeOptions);
		savedInstanceState.putParcelableArrayList(DATA_PACKET_LIST_KEY, mLiveGraph.salvagePacketList());
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Live Graph Thread is stopped by LiveGraphView's destroy method
		stopBluetoothThread();	//stop Bluetooth Thread
		endWritingOperations(true); //stops the Writing Thread and the Timer Thread
	}
	

	


	
	
	
	
	
	
	

}
