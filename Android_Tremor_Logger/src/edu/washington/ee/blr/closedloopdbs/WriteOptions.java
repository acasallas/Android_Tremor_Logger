package edu.washington.ee.blr.closedloopdbs;

import android.os.Parcel;
import android.os.Parcelable;

/* This class holds the three options that a user can select for purposes of writing to a text file. It also
 * implements Parcelable, so it can be converted into a parcel and saved after a phone is rotated.
 * */

public class WriteOptions implements Parcelable {

	
	public enum WriteMode {BYTES,TIME,CONTINUOUS};
	
	public String filename;
	public WriteMode mode;
	public int quantity;
	
	public WriteOptions() {
		filename = "";
		mode = WriteMode.CONTINUOUS;
		quantity = 0;
	}
	
	//The rest of the file is dedicated to implementing the Parcelable interface:
	
	private WriteOptions(Parcel in) {
		filename = in.readString();
		mode = (WriteMode)in.readSerializable();
		quantity = in.readInt();
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(filename);
		dest.writeSerializable(mode);
		dest.writeInt(quantity);
		
	}
	
	public static final Parcelable.Creator<WriteOptions> CREATOR =
			new Parcelable.Creator<WriteOptions>() {

				@Override
				public WriteOptions createFromParcel(Parcel source) {
					return new WriteOptions(source);
				}

				@Override
				public WriteOptions[] newArray(int size) {
					return new WriteOptions[size];
				}
			};
}
