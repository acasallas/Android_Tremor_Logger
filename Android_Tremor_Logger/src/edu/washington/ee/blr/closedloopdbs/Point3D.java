package edu.washington.ee.blr.closedloopdbs;

/* I honestly couldn't find a class in the Android API that could hold x y and z coordinates, so here we are. If you do 
 * end up finding such a class, you may want to switch to that instead as it will probably have more methods than this.
 * 
 * */

public class Point3D {
	
	private double mX;
	private double mY;
	private double mZ;
	
	public Point3D() {
		mX = 0;
		mY = 0;
		mZ = 0;
	}
	
	public Point3D(double x, double y, double z ) {
		mX = x;
		mY = y;
		mZ = z;
	}
	
	public Point3D(double[] coord) {
		mZ = 0;
		mY = 0;
		mX = 0;
		switch(coord.length) {
		case 3:
			mZ = coord[2];
		case 2:
			mY = coord[1];
		case 1:
			mX = coord[0];
			break;
		}
	}

	//general purpose magnitude static method
	public static double get3DMagnitude(double x, double y, double z) {
		return Math.sqrt((x*x) + (y*y) + (z*z)  );
	}

	//magnitude method used for this instance of the object
	public double getMagnitude() {
		return Point3D.get3DMagnitude(mX, mY, mZ);
	}
	
	//Just a bunch of getters/setters:
	public double getX() {
		return mX;
	}
	public void setX(double x) {
		mX = x;
	}
	public double getY() {
		return mY;
	}
	public void setY(double y) {
		mY = y;
	}
	public double getZ() {
		return mZ;
	}
	public void setZ(double z) {
		mZ = z;
	}
	
	//method created for debugging purposes but worth keeping around:
	@Override
	public String toString() {
		return "XYZ: " + mX + " " + mY + " " + mZ;
	}
	

}