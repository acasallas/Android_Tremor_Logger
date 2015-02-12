package edu.washington.ee.blr.closedloopdbs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Environment;
import android.os.Message;

/*
 * HandlerThread to write data on a separate thread.
 * 
 * */

public class WriteHandlerThread extends HandlerThread {
	
	//name of thread
	private static final String TAG = "WRITE_THREAD";
	
	//thread messages
	private static final int WRITE_BYTES = 1;
	public static final int TELL_THREAD_STOP_WRITING = 2;
	public static final int TELL_MAIN_WRITING_DONE = 10;
	public static final int CAN_NO_LONGER_WRITE = 11;
	
	//writing stream
	BufferedOutputStream out;
	
	//threading objects
	Handler mainThreadHandler;
	Handler mHandler;
	
	//call super and save reference to main handler
	public WriteHandlerThread(Handler mainHandler, String proposedFileName,boolean isAppend, Context mContext)
																				throws FileNotFoundException {
		super(TAG);	
		mainThreadHandler = mainHandler;
		
		//if no filename entered, save file as random UUID string.
		if (proposedFileName.equals("")) {
			proposedFileName = UUID.randomUUID().toString();
		}
		
		//Save to DCIM 'External' directory, although Android may decide to save to internal directory instead if
		//it has a lot of room
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
		
		//If filename is a duplicate, add "_1" until filename is unique.
		String[] dirFiles = dir.list();
		for (int n = 0; n < dirFiles.length; n++) {
			if (dirFiles[n].equals(proposedFileName)) {
				proposedFileName = proposedFileName + "_1";
				n=0;
			}	
		}
		
		//create file and open stream. This will throw the FileNotFoundException if it fails
		File fileToWrite = new File(dir, proposedFileName);
		out = new BufferedOutputStream(new FileOutputStream(fileToWrite,isAppend));

	}
	

	
	//thread's main loop
	@SuppressLint("HandlerLeak")
	@Override 
	protected void onLooperPrepared() {
		
		mHandler = new Handler() { 
			@Override 
			public void handleMessage(Message msg) { 
				
				//attempts to write via out.write()
				if (msg.what == WRITE_BYTES) {
					byte[] data = (byte[])msg.obj;
					try {
						out.write(data,0,data.length);
					} catch (IOException e) {
						//if an exception is thrown while trying to write:
						
						//-close stream. Try-catch is mandatory, but there's nothing to do if it actually fails.
						try { out.close();} catch (IOException e2) {}
						
						//-tell main thread we can no longer write
						mainThreadHandler.obtainMessage(CAN_NO_LONGER_WRITE,WriteHandlerThread.this.getThreadId(),0,null).sendToTarget();
					}
					
				} else if (msg.what == TELL_THREAD_STOP_WRITING) {
					//this msg gets called when writing operation is done
					
					//-flush stream. Try-catch is mandatory, but there's nothing to do if it actually fails.
					try {out.flush();} catch (IOException e) {}
					
					//-close stream. Try-catch is mandatory, but there's nothing to do if it actually fails.
					try { out.close();} catch (IOException e) {}
					
					//let main handler know we're done, and then die.
					mainThreadHandler.obtainMessage(TELL_MAIN_WRITING_DONE,WriteHandlerThread.this.getThreadId(),0,null).sendToTarget();
					WriteHandlerThread.this.quit();
				}
			} 
		}; 
	} 
	
	//public command to write bytes, this sends a message to the HandlerThread Loop
	public void writeBytes(byte[] data) {
		mHandler.obtainMessage(WRITE_BYTES,data).sendToTarget();
	}
	
	//public command to stop thread immediatley, flushes out pending messages
	public void stopWritingForcefully() {
		mHandler.removeMessages(WRITE_BYTES);
		mHandler.obtainMessage(TELL_THREAD_STOP_WRITING).sendToTarget();
	}
	
	
	//publi command to stop writing, but to allows pending messages to process
	public void stopWritingPeacefully() {

		mHandler.obtainMessage(TELL_THREAD_STOP_WRITING).sendToTarget();
	}
	

	
	

}
