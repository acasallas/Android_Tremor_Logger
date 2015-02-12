package edu.washington.ee.blr.closedloopdbs;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

/* DataPacket contains three 3-axis measurements and has utility constructors to parse streams of bytes and
 * build the measurements from this stream.
 * 
 * This objects implements 'Parcelable', the code for most of that is in the bottom of the class. Implementing
 * 'Parcelable' was necessary so DataPoint could be saved in an InstanceBundle for when the phone is rotated.
 * (see Android API for information on phone rotation and savedInstanceBundle).
 * 
 * DataPacket is an immutable object, really once it contains the data for a reading at a given point in time, 
 * there should be no reason to change it.
 * 
 * */

public class DataPacket implements Parcelable  {
	
	private final Point3D mAccel; 
	private final Point3D mGyro;
	private final Point3D mComp;
	
	public static final int PACKET_BODY_LENGHT = 18;
	public static final int PACKET_HEADER_LENGHT = 3;
	public static final int PACKETS_PER_SEC = 100;
	public static final double ACCEL_MAX = 2.0; //Accelerometer ranges from +2g to -2g
	public static final double GYRO_MAX = 250.0; //Gyro ranges from -250 deg/sec to +250 deg/sec
	public static final double COMP_MAX = 1200.0; //Compass ranges from -1200uT to +1200uT
	
	//default constructor fills sensor data with all 0's:
	public DataPacket() {
		mAccel = new Point3D();
		mGyro = new Point3D();
		mComp = new Point3D();
	}
	
	public DataPacket(Point3D accel, Point3D gyro, Point3D compass) {
		mAccel = accel;
		mGyro = gyro;
		mComp = compass;
	}
	
	//This constructor expects to recieve a packet of PACKET_BODY_LENGHT, 
	//otherwise it just populates empty fields.
	public DataPacket(byte[] packet) {
		
		if (packet.length != PACKET_BODY_LENGHT) {
			mAccel = new Point3D();
			mGyro = new Point3D();
			mComp = new Point3D();
		} else {
			double[] buffer = new double[3]; //buffer for data processing. Every two bytes is turned into a double.
			
			//create Accel object
			for (int n = 0; n < 3; n++ ) {
				ByteBuffer rawPair = ByteBuffer.wrap(packet,(n*2),2);
				short rawCoor = rawPair.getShort();
				buffer[n] = ((double)rawCoor/(double)Short.MAX_VALUE) * ACCEL_MAX;
			}
			mAccel = new Point3D(buffer);
			
			//create Gyro object
			for (int n = 0; n < 3; n++ ) {
				ByteBuffer rawPair = ByteBuffer.wrap(packet,6+(n*2),2);
				short rawCoor = rawPair.getShort();
				buffer[n] = ((double)rawCoor/(double)Short.MAX_VALUE) * GYRO_MAX;
			}
			mGyro = new Point3D(buffer);
			
			//create Comp object
			for (int n = 0; n < 3; n++ ) {
				ByteBuffer rawPair = ByteBuffer.wrap(packet,12+(n*2),2);
				short rawCoor = rawPair.getShort();
				buffer[n] = ((double)rawCoor/(double)Short.MAX_VALUE) * COMP_MAX;
			}
			mComp = new Point3D(buffer);	
				
		}
	}
	
	//magnitude getters:
	public static double getAccelMagMax() {
		return Point3D.get3DMagnitude(ACCEL_MAX, ACCEL_MAX, ACCEL_MAX);
	}
	public static double getGyroMagMax() {
		return Point3D.get3DMagnitude(GYRO_MAX, GYRO_MAX, GYRO_MAX);
	}
	public static double getCompassMagMax() {
		return Point3D.get3DMagnitude(COMP_MAX, COMP_MAX, COMP_MAX);
	}
	
	//sensor object getters:
	public Point3D getAccel() {
		return mAccel;
	}
	public Point3D getGyro() {
		return mGyro;
	}
	public Point3D getComp() {
		return mComp;
	}
	
	//public static utility method, finds average of an ArrayList of DataPackets:
	public static DataPacket findAvgOfDataPackets(ArrayList<DataPacket> listToAvg) {
		if (listToAvg.size() == 0)
			return new DataPacket();
		
		double accelSumX = 0;
		double accelSumY = 0;
		double accelSumZ = 0;
		
		double gyroSumX = 0;
		double gyroSumY = 0;
		double gyroSumZ = 0;
		
		double compassSumX = 0;
		double compassSumY = 0;
		double compassSumZ = 0;
		
		for (DataPacket each: listToAvg) {
			accelSumX += each.getAccel().getX();
			accelSumY += each.getAccel().getY();
			accelSumZ += each.getAccel().getZ();
			gyroSumX += each.getGyro().getX();
			gyroSumY += each.getGyro().getY();
			gyroSumZ += each.getGyro().getZ();
			compassSumX += each.getComp().getX();
			compassSumY += each.getComp().getY();
			compassSumZ += each.getComp().getZ();
		}
		
		return new DataPacket( 
				new Point3D(  accelSumX/(double)listToAvg.size()  ,  accelSumY/(double)listToAvg.size()  ,   accelSumZ/(double)listToAvg.size()   ),
				new Point3D( gyroSumX/(double)listToAvg.size()   , gyroSumY/(double)listToAvg.size()  ,  gyroSumZ/(double)listToAvg.size()  )   ,
				new Point3D( compassSumX/(double)listToAvg.size()  ,  compassSumY/(double)listToAvg.size()    ,  compassSumZ/(double)listToAvg.size()    ));
	}
	
	//public static utility method, adds an array of DataPackets to an ArrayList staying within bounds of specified maximum
	public static void addArrayToListWithinMax(DataPacket[] toAdd, ArrayList<DataPacket> packetList, int max) {
		if (toAdd.length >= max) {
			DataPacket[] maxSizeArray = new DataPacket[max];
			System.arraycopy(toAdd, toAdd.length - max, maxSizeArray , 0, max);
			packetList = new ArrayList<DataPacket>(Arrays.asList(maxSizeArray));
		} else {
			int overflow = (packetList.size() + toAdd.length ) - max;
			if (overflow > 0) {
				for (int n = 0; n < overflow; n++) {
					packetList.remove(0);
				}
			}
			packetList.addAll(Arrays.asList(toAdd));	
		}
	}

	//Designed for debugging purposes, but a useful method to keep intact:
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Accel ");
		builder.append(mAccel.toString());
		builder.append(" Gyro ");
		builder.append(mGyro.toString());
		builder.append(" Compass ");
		builder.append(mComp.toString());
		return builder.toString();
	}
	
	//constructor for Parcelable purposes:
	DataPacket(Parcel in) {
		double[] coordinates = new double[9];
		in.readDoubleArray(coordinates);
		mAccel = new Point3D(coordinates[0],coordinates[1],coordinates[2]);
		mGyro = new Point3D(coordinates[3],coordinates[4],coordinates[5]);
		mComp = new Point3D(coordinates[6], coordinates[7], coordinates[8]);
	}

	//the remaining methods and inner class are for purposes of implementing the Parcelable interface:
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		double[] coordinates = new double[9];
		coordinates[0] = mAccel.getX();
		coordinates[1] = mAccel.getY();
		coordinates[2] = mAccel.getZ();
		coordinates[3] = mGyro.getX();
		coordinates[4] = mGyro.getY();
		coordinates[5] = mGyro.getZ();
		coordinates[6] = mComp.getX();
		coordinates[7] = mComp.getY();
		coordinates[8] = mComp.getZ();
		dest.writeDoubleArray(coordinates);
	}
	
	static final Parcelable.Creator<DataPacket> CREATOR = new Parcelable.Creator<DataPacket>() {

		@Override
		public DataPacket createFromParcel(Parcel source) {
			return new DataPacket(source);
		}

		@Override
		public DataPacket[] newArray(int size) {
			return new DataPacket[size];
		}
	};


	

	
	
}