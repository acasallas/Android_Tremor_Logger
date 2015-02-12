package edu.washington.ee.blr.closedloopdbs;



import java.io.IOException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import android.os.Process;

/*
 * This thread takes care of live Bluetooth communication in the background.
 * While running, it returns a 'Thread Nugget' object to the main thread which contains a list of new Data Packets
 * and the raw data received in each iteration of the read command.
 * */

public class BluetoothThread extends Thread {
	
	//Bluetooth communication objects
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    
    //Thread objects/variables
    private Handler mHandler;
    public static final int MESSAGE_READ = 1;
    public static final int BLUETOOTH_DEAD = 2;
    
    //private StringBuilder dataStringBuilder = new StringBuilder();
	//final private int maxStringLength = 300;
	
    //constants used to parse raw stream
    final byte asciiG = 71;
	final byte asciiE = 69;
	final byte ascii0x15 = 0x15;
		
	boolean mustStopThread = false;
	

	//list used to hold unparsed bytes from last read iteration.
	ArrayList<Byte> leftoverBytes = new ArrayList<Byte>();
    
    public BluetoothThread(BluetoothSocket socket, Handler parentHandle)
    													throws IOException {
       	//initalize objects. It is responsibility of calling thread NOT to pass a null socket to this constructor, otherwise
    	//an exception will be thrown eventually
    	mmSocket = socket;
        mHandler = parentHandle;
 
        //get input stream to read Bluetooth. This will throw the IOException if it fails.
        mmInStream = socket.getInputStream();
    }
 
    public void run() {
    	
    	Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);	//to prevent UI from going too slow
        byte[] buffer = new byte[512];  // buffer store for the stream
        int size = 0; // bytes returned from read()
 
        // Keep listening to the InputStream until an exception occurs or until we set interrupt flag from main thread.
        while (!mustStopThread) {
            try {
                // Read from the InputStream
                size = mmInStream.read(buffer);
                
                //copy data read in this iteration onto rawData
                byte[] rawData = new byte[size];
        		System.arraycopy(buffer, 0, rawData, 0, size);
                
                //data array will contain leftover bytes from last iteration + data from this iteration
        		byte[] data = new byte[size + leftoverBytes.size()];
        		Iterator<Byte> iterator = leftoverBytes.iterator();
                for (int i = 0; i < leftoverBytes.size(); i++)
                {
                    data[i] = iterator.next().byteValue();
                }
        		System.arraycopy(buffer, 0, data, leftoverBytes.size(), size);
        		
        		leftoverBytes.clear(); //clear leftoverBytes as they are no longer needed
        		
        		//process data array and create ThreadNugget to pass back to main thread
        		ThreadNugget nugget = new ThreadNugget(processToDataPackets(data),rawData);
        		
        		//pass the nugget back to the main thread
                mHandler.obtainMessage(MESSAGE_READ, nugget).sendToTarget();
                
            } catch (IOException e) {
            	//if Bluetooth throws an exception at any point, thread will end, so we send a handler to main
            	//thread so it can notify user and take appropriate action
            	
            	try {mmSocket.close();} catch (IOException e1) {}
            	
            	mHandler.obtainMessage(BLUETOOTH_DEAD).sendToTarget();
                break;
            }
        }
        
        
    }
    
    //method that processes raw bytes and turns it into an array of DataPackets
	private DataPacket[] processToDataPackets(byte[] data) {
    	
		//create DataPacket[] that is the maximum lenght we might need for this byte array
    	DataPacket[] arrayForAdding = new DataPacket[ data.length / (DataPacket.PACKET_HEADER_LENGHT + DataPacket.PACKET_BODY_LENGHT)  ];
    	int addingCtr = 0;
    	
    	//look for 'GE0x15' header and if found, create DataPacket from packet.
    	int n = 0;
    	for (; n < data.length - (DataPacket.PACKET_BODY_LENGHT + DataPacket.PACKET_HEADER_LENGHT - 1); n++   ) {
    		
    		if (data[n] == asciiG) {
    			if (data[n+1] == asciiE && data[n+2] == ascii0x15) {
    				byte[] packetArray = new byte[DataPacket.PACKET_BODY_LENGHT];
	        		System.arraycopy(data, n+3, packetArray, 0, DataPacket.PACKET_BODY_LENGHT);
	        		
	        		arrayForAdding[addingCtr] = new DataPacket(packetArray);
	        		addingCtr++;
	        		
					n += (DataPacket.PACKET_BODY_LENGHT + DataPacket.PACKET_HEADER_LENGHT - 1);
    			}
    		}
    	}
    	
    	//now add leftover bytes to leftoverBytes.
    	for (; n < data.length; n++) {
    		leftoverBytes.add(Byte.valueOf(data[n]));
    	}
    	
    	DataPacket[] arrayToReturn = new DataPacket[addingCtr];
    	//Copy array to arrayToReturn
    	System.arraycopy(arrayForAdding, 0, arrayToReturn, 0, addingCtr);
    	
    	return arrayToReturn;
    }
 
    /* Call this from the main activity to shutdown the connection */
    public void stopThread() {
    	mustStopThread = true;

        
    }
}
