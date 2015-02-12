package edu.washington.ee.blr.closedloopdbs;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.os.Process;

/*
 * This class represents the Live Graph. It also contains (and defines first) the inner class which runs
 * the thread that draws the graph.
 * */

public class LiveGraphView extends SurfaceView implements SurfaceHolder.Callback {
	
	//inner class: the thread that draws the graph
	class LiveGraphThread extends Thread {
		
		boolean mustStop = false; //thread kill switch
		
		@Override
		public void run() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
			
			while (!mustStop) {
				Canvas c = null;
				try {
					c = mHolder.lockCanvas();
					if (c!= null) {
						synchronized (mHolder) {
							//drawColor erases canvas so data can be re-drawn anew.
							c.drawColor( getResources().getColor(R.color.live_graph) );
							//must call synchronized on packetList so it is not accessed while being updated
							//by main thread
							synchronized (packetList) {
								if (options != null)
									drawPoints(c);	//Graphs points in packetList
							}
							drawAxes(c); //draws axes and ticks on graph
						}
					}
				} finally {
					if (c!=null) {
						mHolder.unlockCanvasAndPost(c); //sends canvas to actually be displayed on screen
					}
				}
			}
		}
		
		public void stopThread() {
			mustStop = true;
		}
		
		
	}
	

	//Height and Width of Graph
	private float viewHeight;
	private float viewWidth;
	
	//Multi-Threading Objects
	LiveGraphThread mThread;
	SurfaceHolder mHolder;
	
	//PacketList
	ArrayList<DataPacket> packetList;
	private int packetListMax = 0;
	public final static int RESOLUTION = 500;
	
	GraphOptions options;
	
	
	public LiveGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		//create new packetList and initially set the size to be that of RESOLUTION
		packetList = new ArrayList<DataPacket>();
		setPacketListMax(RESOLUTION);
		
		//Prepare SurfaceHolder (actually I'm not sure what this does, just calling it because Android said to).
		mHolder = getHolder();
		mHolder.addCallback(this);
	}
	
	//sets the maximum size for packetList and populates it with 0's.
	public void setPacketListMax(int max) {
		if (packetListMax == max)
			return;
		
		packetListMax = max;
		
		synchronized(packetList) {
			packetList.clear();
			for (int n = 0; n < packetListMax; n++) {
				packetList.add(new DataPacket());
			}
		}
	}
	
	//sets packetList to a passed in list (used to restore the data after a phone has been rotated)
	public void setRestoredPacketList(ArrayList<DataPacket> list) {
		packetListMax = list.size();

			synchronized(packetList) {
				packetList = list;
			}
	}
	
	//saves graph options to the class (because the options are used when drawing points)
	public void setGraphOptions(GraphOptions optionsToSet) {
		options = optionsToSet;
	}
	

	//called when the SurfaceView is created.
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		//Start new thread
		mThread = new LiveGraphThread();
		mThread.start();
	}

	//Just update Canvas measurements
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		viewHeight = height;
		viewWidth = width;
		
	}

	//quit thread to avoid a leak
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		killThread();
	}
	
	//used to save packetList for when the phone is going to be rotated.
	public ArrayList<DataPacket> salvagePacketList() {
		return packetList;
	}
	
	
	private void killThread() {
		if (mThread != null) {
			//pulls the stop thread switch. I don't believe there's anything in the thread that blocks forever, 
			//so the thread should stop within a few milliseconds.
			mThread.stopThread();
			boolean keepTrying = true;
			while (keepTrying) {
				try {
				mThread.join(); //wait for the thread to stop by blocking until it's done.
				keepTrying = false;
				} catch (InterruptedException e) {
					
				}
			}
			mThread = null;
		}
	}
	
	//public method used for adding new data to list, this will be called by the main thread several times per second.
	public void addPacketsToList(DataPacket[] newPackets) {
		synchronized(packetList) {	
			DataPacket.addArrayToListWithinMax(newPackets, packetList, packetListMax);
		}
	}
	
	//converts accel/gyro/compass value to a y-value on the graph
	private float getGraphValuePosition(double value, double max) {
		double difference = max - value;
		float ratio = (float) ((float)difference/(max*2));
		return ratio*viewHeight;
	}
	
	//Axes drawing method, called during each draw after data has been plotted.
	private void drawAxes(Canvas c) {
		//set up brush
		Paint axisPaint = new Paint();
		axisPaint.setColor(Color.rgb(0, 0, 0));
		axisPaint.setStyle(Style.STROKE);
		axisPaint.setStrokeWidth(5);
		axisPaint.setTextSize(20);
		
		//draw horizontal axis
		c.drawLine(0, getGraphValuePosition(0,1), viewWidth, getGraphValuePosition(0,1), axisPaint);
		
		
		//draw vertical markers
		c.drawLine(5, 0, 5, 30, axisPaint);
		c.drawLine(viewWidth*.25f, 0, viewWidth*.25f, 30, axisPaint);
		c.drawLine(viewWidth/2, 0, viewWidth/2, 30, axisPaint);
		c.drawLine(viewWidth*.75f, 0, viewWidth*.75f, 30, axisPaint);
		c.drawLine(viewWidth-5, 0, viewWidth-5, 30, axisPaint);
		
	}
	
	public void drawPoints(Canvas c) {
		
		int size = packetList.size();
		if (size < 2)
			return; //can't draw points if there's only one point
		
		//Prepare Paint object
		Paint axisPaint = new Paint();
		axisPaint.setStyle(Style.STROKE);
		axisPaint.setStrokeWidth(3);
		
		//if 'show Magnitude Only' is on, just draw up to three point sets.
		if (options.isMagnitudeOn) {
			
			float[] accelPts = new float[size];
			float[] gyroPts = new float[size];
			float[] compassPts = new float[size];
			
			//get magnitudes for all three readings
			for (int n = 0; n < size; n++) {
				DataPacket packet = packetList.get(n);
				
				accelPts[n] = (float) packet.getAccel().getMagnitude();
				gyroPts[n] = (float) packet.getGyro().getMagnitude();
				compassPts[n] = (float) packet.getComp().getMagnitude();
			}
			
			//draw lines with different colors for all three magnitudes
			if (options.isAccelOn) {
				axisPaint.setColor(Color.rgb(180, 0, 0));
				drawPointSet(c, accelPts, (float)DataPacket.ACCEL_MAX, axisPaint);
			}
			if (options.isGyroOn) {
				axisPaint.setColor(Color.rgb(0, 180, 0));
				drawPointSet(c, gyroPts,(float) DataPacket.GYRO_MAX, axisPaint);
			}
			if (options.isCompassOn) {
				axisPaint.setColor(Color.rgb(0, 0, 180));
				drawPointSet(c, compassPts, (float)DataPacket.COMP_MAX, axisPaint);
			}
			
		//otherwise, draw up to nine lines for each individual axis of data:	
		} else {
			//three arrays containing three arrays of axis data each (total of 9 arrays)
			float[][] accelPts = new float[][] {new float[size],new float[size],new float[size]};
			float[][] gyroPts = new float[][] {new float[size],new float[size],new float[size]};
			float[][] compassPts = new float[][] {new float[size],new float[size],new float[size]};
			
			//populate the arrays of arrays.
			for (int n = 0; n < size; n++) {
				DataPacket packet = packetList.get(n);
				
				accelPts[0][n] = (float) packet.getAccel().getX();
				accelPts[1][n] = (float) packet.getAccel().getY();
				accelPts[2][n] = (float) packet.getAccel().getZ();
				
				gyroPts[0][n] = (float) packet.getGyro().getX();
				gyroPts[1][n] = (float) packet.getGyro().getY();
				gyroPts[2][n] = (float) packet.getGyro().getZ();
				
				compassPts[0][n] = (float) packet.getComp().getX();
				compassPts[1][n] = (float) packet.getComp().getY();
				compassPts[2][n] = (float) packet.getComp().getZ();
			}
			
			//draw everything
			if (options.isAccelOn) {
				axisPaint.setColor(Color.rgb(100, 0, 0));
				drawPointSet(c, accelPts[0], (float)DataPacket.ACCEL_MAX, axisPaint);
				axisPaint.setColor(Color.rgb(180, 0, 0));
				drawPointSet(c, accelPts[1], (float)DataPacket.ACCEL_MAX, axisPaint);
				axisPaint.setColor(Color.rgb(255, 0, 0));
				drawPointSet(c, accelPts[2], (float)DataPacket.ACCEL_MAX, axisPaint);
			}
			if (options.isGyroOn) {
				axisPaint.setColor(Color.rgb(0, 100, 0));
				drawPointSet(c, gyroPts[0],(float) DataPacket.GYRO_MAX, axisPaint);
				axisPaint.setColor(Color.rgb(0, 180, 0));
				drawPointSet(c, gyroPts[1],(float) DataPacket.GYRO_MAX, axisPaint);
				axisPaint.setColor(Color.rgb(0, 255, 0));
				drawPointSet(c, gyroPts[2],(float) DataPacket.GYRO_MAX, axisPaint);
			}
			if (options.isCompassOn) {
				axisPaint.setColor(Color.rgb(0, 0, 100));
				drawPointSet(c, compassPts[0], (float)DataPacket.COMP_MAX, axisPaint);
				axisPaint.setColor(Color.rgb(0, 0, 180));
				drawPointSet(c, compassPts[1], (float)DataPacket.COMP_MAX, axisPaint);
				axisPaint.setColor(Color.rgb(0, 0, 255));
				drawPointSet(c, compassPts[2], (float)DataPacket.COMP_MAX, axisPaint);
			}
			
		}
	}
	
	//generic method used to take a set of points and draw them as a contiguous line
	void drawPointSet(Canvas c, float[] ptSet, float max, Paint paintBrush) {
		
		if (ptSet.length < 2)
			return; //can't draw a line for only one point!
		
		//calculates total number of x-coordinates needed
		//we need two coordinates for each point, except the endpoints, which need only one
		int coorAmnt = (ptSet.length-1)*4; 
		float[] xyline = new float[coorAmnt];
		
		//set up first point
		xyline[0] = 0;
		xyline[1] = getGraphValuePosition( ptSet[0],max );
		
		//set up middle points
		for (int n = 0; n < ptSet.length-2; n++) {
			xyline[n*4+2] = ((float)n/(float)ptSet.length) * viewWidth;
			xyline[n*4+1+2] = getGraphValuePosition( ptSet[n+1],max );
			xyline[n*4+2+2] = ((float)n/(float)ptSet.length) * viewWidth;
			xyline[n*4+3+2] = getGraphValuePosition( ptSet[n+1],max );
		}
		
		//set up last point
		xyline[coorAmnt-2] = viewWidth;
		xyline[coorAmnt-1] = getGraphValuePosition( ptSet[ptSet.length-1],max );
		
		//draw all points
		c.drawLines(xyline, paintBrush);
	}

}
