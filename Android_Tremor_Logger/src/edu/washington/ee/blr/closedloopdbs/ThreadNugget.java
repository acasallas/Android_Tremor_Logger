package edu.washington.ee.blr.closedloopdbs;


/* This is just a wrapper class used to hold the two arrays that are sent through inter-thread communication
 * between the Bluetooth thread and the main thread, since inter-thread communication can only handle one object
 * at a time.
 * */

public class ThreadNugget {
	private DataPacket[] mDataPackets;
	private byte[] mRawData;
	
	public ThreadNugget(DataPacket[] dataPacks, byte[] rawData) {
		mDataPackets = dataPacks;
		mRawData = rawData;
	}

	public byte[] getRawData() {
		return mRawData;
	}

	public void setRawData(byte[] rawData) {
		mRawData = rawData;
	}

	public DataPacket[] getDataPackets() {
		return mDataPackets;
	}

	public void setDataPackets(DataPacket[] dataPackets) {
		mDataPackets = dataPackets;
	}


	

	


	
	

}
