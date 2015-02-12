package edu.washington.ee.blr.closedloopdbs;

import android.os.Parcel;
import android.os.Parcelable;

/* Class to hold information on display of sensor data.
 * 
 * Implements Parcelable so data can be saved in InstanceBundle when phone is rotated.
 * 
 * */

public class GraphOptions implements Parcelable {

	public enum GraphOptionType { CURRENT_STATE_MODE, DEBUG_MODE, LIVE_GRAPH_MODE };

	public GraphOptionType optionType;
	//following three booleans determine which sensors are displayed in Live Graph mode:
	public boolean isAccelOn;
	public boolean isGyroOn;
	public boolean isCompassOn;
	public boolean isMagnitudeOn; //determines whether only the magnitudes of sensors are displayed
	public int packetsToShow; //number of packets to show in LiveGraph Mode
	
	//class only has default constructor, as it is actually created only once in whole application.
	public GraphOptions() {
		optionType = GraphOptionType.CURRENT_STATE_MODE;
		isAccelOn = false;
		isGyroOn = false;
		isCompassOn = false;
		isMagnitudeOn = false;
		packetsToShow = 100;
	}
	
	//The following constructors, methods, and inner class only exist to implement Parcelable interface
	private GraphOptions(Parcel in) {
		boolean[] booleanArray = new boolean[4];  
		in.readBooleanArray(booleanArray);
		isAccelOn = booleanArray[0];
		isGyroOn = booleanArray[1];
		isCompassOn = booleanArray[2];
		isMagnitudeOn = booleanArray[3];
		
		optionType = (GraphOptionType)in.readSerializable();
		packetsToShow = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		boolean[] booleanArray = new boolean[4]; 
		booleanArray[0] = isAccelOn;
		booleanArray[1] = isGyroOn;
		booleanArray[2] = isCompassOn;
		booleanArray[3] = isMagnitudeOn;
		dest.writeBooleanArray(booleanArray);
		dest.writeSerializable(optionType);
		dest.writeInt(packetsToShow);
		
	}
	
	static final Parcelable.Creator<GraphOptions> CREATOR = new Parcelable.Creator<GraphOptions>() {

		@Override
		public GraphOptions createFromParcel(Parcel source) {
			return new GraphOptions(source);
		}

		@Override
		public GraphOptions[] newArray(int size) {
			return new GraphOptions[size];
		}
	};
	
}
