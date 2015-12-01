package org.opencv.finalproject;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.finalproject.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class FinalProject extends Activity implements CvCameraViewListener2 {
    private static final String    TAG = "OCVSample::Activity";

    private static final int       VIEW_MODE_RGBA     = 0;
    private static final int       VIEW_MODE_COLOR_THRESHOLD     = 1;
    private static final int       VIEW_MODE_ALL_PROCESSING    = 2;

    private static final int	   ERR_NO_COLOR = 0;
    private static final int	   ERR_NO_USER_IMAGE = 1;
    
    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    
    private Point[]				   mCamCorners = new Point[4];
    private boolean				   mFoundCorners = false;
    
    private Bitmap				   mBitmap = null;
    private Mat					   mUserImg;
    
    private Scalar				   mColorData;
    private Point				   mTouchPoint;
    private Boolean				   mTouchEvent = false;

    private int					   mDroppedFrameCounter = 0;
    private int					   mErr;
    
    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewColorThresholded;
    private MenuItem               mItemPreviewAllProcessing;
    private MenuItem			   mItemChooseImage;

    private Uri 				   selectedImageURI = null;
    
    private CameraBridgeViewBase   mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FinalProject() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.final_project_surface_view);
        
        // uncomment when testing on actual device (creates problem with emulator)
        //mOpenCvCameraView.setMaxFrameSize(mOpenCvCameraView.getMinimumWidth(), mOpenCvCameraView.getMinimumHeight());
        mOpenCvCameraView.setMaxFrameSize(400,400);
        
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        
        mViewMode = VIEW_MODE_RGBA;
        
        // set Touch Listener
        mOpenCvCameraView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event)
            {
            	int cols = mRgba.cols();
                int rows = mRgba.rows();

//                int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
//                int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
                
                int x = (int)event.getX() * cols/mOpenCvCameraView.getWidth();
                int y = (int)event.getY() * rows/mOpenCvCameraView.getHeight();
                
                if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
                else {
                	mTouchPoint = new Point(x,y);
                	mTouchEvent = true;
                	return true;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewColorThresholded = menu.add("Preview Thresholded");
        mItemPreviewAllProcessing = menu.add("All Processing");
        mItemChooseImage = menu.add("Choose Image");
        return true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);      
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mIntermediateMat.release();
    }
    
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {  	
    	final int viewMode = mViewMode;
        mRgba = inputFrame.rgba();
        
        //Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20,20));
        
    	// Set color upon user selection
    	if(mTouchEvent)	mColorData = new Scalar(mRgba.get((int)mTouchPoint.y, (int)mTouchPoint.x));
        
        switch (viewMode) {
        case VIEW_MODE_COLOR_THRESHOLD:
        	/*
        	// if color isn't set, just reset to RGBA mode
        	if (mColorData == null) {
        		mViewMode = VIEW_MODE_RGBA;
        		break;
        	}
        	
        	// Color threshold
        	double thresh = 1.5;
        	colorThreshold(mRgba, mIntermediateMat, thresh);
        	
        	//Imgproc.morphologyEx(mIntermediateMat, mIntermediateMat, Imgproc.MORPH_CLOSE, element);
        	
        	// Isolate the object of interest
        	mIntermediateMat = isolateComponent(mIntermediateMat, mTouchPoint);

        	// Get corners
        	Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, new Size(5,5), 20.0);        	
        	getHarrisCorners(true);
        	*/

        	if (mColorData == null) {
        		mViewMode = VIEW_MODE_RGBA;
        		break;
        	}
        	
        	// Do thresholding
        	Imgproc.GaussianBlur(mRgba, mIntermediateMat, new Size(5,5), 20.0);
        	
        	double thresh = 1.5;
        	colorThreshold(mIntermediateMat, mIntermediateMat, thresh);
        	
        	mIntermediateMat = isolateComponent(mIntermediateMat, mTouchPoint);
        	
        	// Detect edges
        	Imgproc.Canny(mIntermediateMat, mIntermediateMat, 80, 100);
        	
            // SELECT METHOD:
            int METHOD = 1; 
            //	1 : Contour detection + polygonal curve approximation method
            //	2 : Hough lines + intersections bounding box method
            //	3 : Hough lines + weighted intersections method
            //	4 : Hough lines + max X,Y method
            
            if(METHOD == 1) contourCornerDetection(); // this one works best
            if(METHOD == 2) bbCornerDetection(true);
            if(METHOD == 3) weightedIsxCornerDetection(true);
            if(METHOD == 4) bpCornerDetection(true);
            
            if(mFoundCorners) {
		    	// Draw circles at corners (intersections)
            	if(mIntermediateMat.channels() == 1)
            		Imgproc.cvtColor(mIntermediateMat, mIntermediateMat, Imgproc.COLOR_GRAY2RGBA, 4);
		    	for(int i = 0; i < 4; i++) {
		    		Core.circle(mIntermediateMat, mCamCorners[i], 5, new Scalar(0,255,0,255), 1);
		    	}
            }

            mRgba = mIntermediateMat;
            break;
            
        case VIEW_MODE_RGBA:
            // input frame has RBGA format        	
            break;
            
        case VIEW_MODE_ALL_PROCESSING:        	
        	// Make sure thresh color AND user image have been selected
        	if (mColorData == null) {
        		displayPopup(ERR_NO_COLOR);
        		break;
        	}
        	if (mUserImg == null) {
        		displayPopup(ERR_NO_USER_IMAGE);
        		break;
        	}
        	
        	// Pre-process userImg according to lighting in scene
        	Mat ccUserImg = sbmColorCorrection(mUserImg);
        	
        	// Color threshold
        	double thresh2 = 1.5;
        	colorThreshold(mRgba, mIntermediateMat, thresh2);
        	
        	// Small Region Removal
        	//Imgproc.morphologyEx(mIntermediateMat, mIntermediateMat, Imgproc.MORPH_CLOSE, element);
        	
        	// Isolate the object of interest
        	mIntermediateMat = isolateComponent(mIntermediateMat, mTouchPoint);
        	
        	// Low-pass filter
        	Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, new Size(5,5), 20.0);
        	
        	// Get corners
        	getHarrisCorners(false);
        	//contourCornerDetection();
        	
        	// Draw image
        	drawWarpedImg(ccUserImg);
            
            break;
        }

        if(mTouchEvent) {
        	// Draw color circle where user touched screen 
        	Core.circle(mRgba, mTouchPoint, 30, mColorData, 5);
    		mTouchEvent = false;
    	}
        else {
        	// If we've detected the corners of our subject, we can
        	// refine user point selection based on corner locations
        	if (mFoundCorners) updateTouchPoint();
        }
        
        return mRgba;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewColorThresholded) {
            mViewMode = VIEW_MODE_COLOR_THRESHOLD;
        } else if (item == mItemPreviewAllProcessing) {
            mViewMode = VIEW_MODE_ALL_PROCESSING;
        } else if (item == mItemChooseImage) {
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,
                    "Select Picture"), 1);
        }

        return true;
    }
    
    private Mat sbmColorCorrection(Mat img) {
    	// scale-by-max color correction for 4-channel image
    	int rows = img.rows();
    	int cols = img.cols();
    	int chan = img.channels();
    	if(chan != 4) return img; // make sure we have an RGBA color img
    	
    	Mat result = new Mat(new Size(rows,cols), CvType.CV_8UC4);
    		
    	List<Mat> sChannels = new ArrayList<Mat>();
		Core.split(mRgba, sChannels);
		
		// Find max color values in each channel of camImg
		double[] maxColorVal = new double[3];
		for(int c = 0; c < 3; c++) {
    		Core.MinMaxLocResult searchResult = Core.minMaxLoc(sChannels.get(c));
    		maxColorVal[c] = searchResult.maxVal;
		}

		// Balance img accordingly
		Core.multiply(img, new Scalar(255/maxColorVal[0],
									  255/maxColorVal[1],
									  255/maxColorVal[2], 1), result, 1);
		
    	return result;
	}
    
    private void contourCornerDetection()
    {
		// Lowpass filter
		//Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, new Size(5,5), 20.0);
		
		// Find contours
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(mIntermediateMat, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		            	
		// Try to find rectangular contour
		MatOfPoint2f rectApproxCurve = new MatOfPoint2f();
		for(int i = 0; i < contours.size(); i++) {
	    	// For each contour found in the image...
			MatOfPoint contour = contours.get(i);
	    	MatOfPoint2f contour2f = new MatOfPoint2f( contour.toArray() );
	    	
	    	// Get approx curve
	    	MatOfPoint2f approxCurve = new MatOfPoint2f();
	    	Imgproc.approxPolyDP(contour2f, approxCurve, ((int)contour.total())*0.05, true);
	    	
	    	// If the approx curve has 4 points, it's a rectangle
	      	if(approxCurve.total() == 4) {
	  			rectApproxCurve = approxCurve;
	    	}
		}
		
		if(rectApproxCurve.total() == 0) {
			// If no rectangle curve found, give system two frames to recover 
			if(mDroppedFrameCounter > 2) {
				mFoundCorners = false;
			}
			else {
				mDroppedFrameCounter++;
			}
		}
		else {
			// If rectangle curve found, get it's corners
			for(int c = 0; c < 4; c++) {
				double[] p = rectApproxCurve.get(c,0);
				mCamCorners[c] = new Point(p[0],p[1]);
				mFoundCorners = true;
				mDroppedFrameCounter = 0;
			}
		}
	}
    
    private void bbCornerDetection(boolean drawLines)
    {
	 	// Detect Hough lines
	    Mat lines = new Mat();
	    int threshold = 40;
	    int minLineSize = 30;
	    int lineGap = 40;
	
	    Imgproc.HoughLinesP(mIntermediateMat, lines, 1, Math.PI/180, threshold, minLineSize, lineGap);
	

		// Draw lines to screen
	    if(drawLines) {
	    	Imgproc.cvtColor(mIntermediateMat, mIntermediateMat, Imgproc.COLOR_GRAY2RGBA, 4);
			for (int x = 0; x < lines.cols(); x++) {
	              double[] vec = lines.get(0, x);
	              double x1 = vec[0], 
	                     y1 = vec[1],
	                     x2 = vec[2],
	                     y2 = vec[3];
	              Point start = new Point(x1, y1);
	              Point end = new Point(x2, y2);
	              	
	              Core.line(mIntermediateMat, start, end, new Scalar(255,0,0,255), 1);
	        }
	    }
		    
		// Calculate intersections
	    ArrayList<Point> isxPts  = new ArrayList<Point>();
	    for (int i = 0; i < lines.cols(); i++) {
			
	    	double[] vec_1 = lines.get(0, i);
	    	double x1_1 = vec_1[0], 
	    		   y1_1 = vec_1[1],
	               x2_1 = vec_1[2],
	               y2_1 = vec_1[3];
	        Point start_1 = new Point(x1_1, y1_1);
	        Point end_1 = new Point(x2_1, y2_1);
			
	    	for (int j = 0; j < lines.cols(); j++) {
	    		
	        	double[] vec_2 = lines.get(0, j);
	        	double x1_2 = vec_2[0], 
	        		   y1_2 = vec_2[1],
	                   x2_2 = vec_2[2],
	                   y2_2 = vec_2[3];
	            Point start_2 = new Point(x1_2, y1_2);
	            Point end_2 = new Point(x2_2, y2_2);
	            
	            // Calculate intersection point for every pair of lines
	            Point isx = calcIntersection(start_1, end_1, start_2, end_2);
	            
	            // Make sure it's within a reasonable area
	            double tolerance = 100;
	            if (isx.x > -tolerance && isx.x < mIntermediateMat.cols()+tolerance &&
	            		isx.y > -tolerance && isx.y < mIntermediateMat.rows()+tolerance )
	            {
	            	isxPts.add( isx );	// add to vector
	            }
	    	}
	    }
	    
	    if(isxPts.size() >= 4) {
	        // Convert intersection points to a MatOfPoint structure
	        Point[] isxPts_array = isxPts.toArray(new Point[isxPts.size()]);
	        MatOfPoint isxPts_Mat = new MatOfPoint( isxPts_array );
	        
	        // Get bounding rectangle around intersections
	        Rect bRect = Imgproc.boundingRect(isxPts_Mat);
	
	        // Determine corners:
	        // 	For each corner of bounding box, find closest intersection point.
	        // 	Use that intersection point as that corner of our warped perspective rectangle.
	        Point p = new Point();
	        for(int c = 0; c < 4; c++) {
	        	switch(c) {
	        	case(0): p = bRect.tl(); break;
	        	case(1): p = new Point(bRect.tl().x+bRect.width,bRect.tl().y); break;
	        	case(2): p = bRect.br(); break;
	        	case(3): p = new Point(bRect.tl().x,bRect.tl().y+bRect.height); break;
	        	}
	        	
	            double minDist = 10000;
	            int cornerIdx = 0;
	            for(int i = 0; i < isxPts.size(); i++) {
	            	double distToPt = dist(p,isxPts.get(i));
	            	if(distToPt < minDist) {
	            		minDist = distToPt;
	            		cornerIdx = i;
	            	}
	            }
	            mCamCorners[c] = isxPts.get(cornerIdx);
	        }
	        
	        if(drawLines)
	        	Core.rectangle(mIntermediateMat, bRect.tl(), bRect.br(), new Scalar(52,150,192,255));
	        
	        mFoundCorners = true;
	    }
	    else { mFoundCorners = false; }
	}
		
    private void weightedIsxCornerDetection(boolean drawLines)
    {
    	// Detect Hough lines
        Mat lines = new Mat();
        int threshold = 40;
        int minLineSize = 30;
        int lineGap = 40;

        Imgproc.HoughLinesP(mIntermediateMat, lines, 1, Math.PI/180, threshold, minLineSize, lineGap);

    	// Draw lines to screen
        if(drawLines) {
	    	Imgproc.cvtColor(mIntermediateMat, mIntermediateMat, Imgproc.COLOR_GRAY2RGBA, 4);
    		for (int x = 0; x < lines.cols(); x++) {
                  double[] vec = lines.get(0, x);
                  double x1 = vec[0], 
                         y1 = vec[1],
                         x2 = vec[2],
                         y2 = vec[3];
                  Point start = new Point(x1, y1);
                  Point end = new Point(x2, y2);
                  	
                  Core.line(mIntermediateMat, start, end, new Scalar(255,0,0,255), 1);
            }
        }
        
        // Calculate line lengths
        double[] len = new double[lines.cols()];
        for (int i = 0; i < lines.cols(); i++) {
        	double[] vec = lines.get(0, i);
        	double x1 = vec[0], 
        		   y1 = vec[1],
                   x2 = vec[2],
                   y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            
            len[i] = dist(start, end);
        }
        
		// Calculate intersections & weight them based on distance from endpoints and length of lines
        Point[][] isxPts = new Point[lines.cols()][lines.cols()];
        Mat isxWeights = new Mat(lines.cols(), lines.cols(), CvType.CV_32FC1);
		for (int i = 0; i < lines.cols(); i++) {
			
        	double[] vec_1 = lines.get(0, i);
        	double x1_1 = vec_1[0], 
        		   y1_1 = vec_1[1],
                   x2_1 = vec_1[2],
                   y2_1 = vec_1[3];
            Point start_1 = new Point(x1_1, y1_1);
            Point end_1 = new Point(x2_1, y2_1);
			
        	for (int j = 0; j < lines.cols(); j++) {
        		
            	double[] vec_2 = lines.get(0, j);
            	double x1_2 = vec_2[0], 
            		   y1_2 = vec_2[1],
                       x2_2 = vec_2[2],
                       y2_2 = vec_2[3];
                Point start_2 = new Point(x1_2, y1_2);
                Point end_2 = new Point(x2_2, y2_2);
                
                // For every pair of lines...
                
                // Calculate intersection point
                isxPts[i][j] = calcIntersection(start_1, end_1, start_2, end_2);
                
                // Calculate min distance from an endpoint of those lines
                double minDist;
                if(isxPts[i][j].x == Double.NaN || isxPts[i][j].y == Double.NaN) {
                	// If the lines don't intersect, just set minDist as a big number
                	minDist = 10000;
                }
                else {
                	double[] endPtDists = new double[4];
                    endPtDists[0] = dist(start_1, isxPts[i][j]);
                    endPtDists[1] = dist(end_1,   isxPts[i][j]);
                    endPtDists[2] = dist(start_2, isxPts[i][j]);
                    endPtDists[3] = dist(end_2,   isxPts[i][j]);
                    
                    minDist = Math.min( Math.min(endPtDists[0], endPtDists[1]) , Math.min(endPtDists[2], endPtDists[3]) );
                }
                
                // Get angle between lines
                Mat line1 = new Mat(new Size(2,1), CvType.CV_32FC1);
                	line1.put(1,1, end_1.x-start_1.x);
                	line1.put(2,1, end_1.y-start_1.y);
                	
                Mat line2 = new Mat(new Size(2,1), CvType.CV_32FC1);
                    line2.put(1,1, end_2.x-start_2.x);
                	line2.put(2,1, end_2.y-start_2.y);
                		                	
                double cosTheta = line1.dot(line2)/(Core.norm(line1,Core.NORM_L2) * Core.norm(line2,Core.NORM_L2));   
                                    
                // Define weight as:
                //		w = sqrt(length(line1)*length(line2)) * (minimum distance from an endpoint)^-1 * |angleBetweenLines|/PI
                
                //if(cosTheta != Double.NaN)
                //	isxWeights.put(i, j, Math.sqrt(len[i] * len[j]) * (1/minDist) * (1.0 - Math.abs(Math.acos(cosTheta)/(3.14159/2.0)) - 1.0));
                //else
                //	isxWeights.put(i, j, Math.sqrt(len[i] * len[j]) * (1/minDist));
                
                isxWeights.put(i, j, Math.sqrt(len[i] * len[j]) * (1/minDist));
        	}
        }
    
		// find intersection points with max weightings
    	for(int i = 0; i < 4; i++) {
    		Core.MinMaxLocResult searchResult = Core.minMaxLoc(isxWeights);
    		Point maxWeightIdx = searchResult.maxLoc;
    		
    		if( maxWeightIdx.y > 0 && maxWeightIdx.x > 0 ) {
    			mCamCorners[i] = isxPts[(int)maxWeightIdx.y][(int)maxWeightIdx.x];
    		}
    		
    		// Zero out the this peak so we avoid repeats
    		isxWeights.put((int)maxWeightIdx.y, (int)maxWeightIdx.x, 0.0);
    		
    	}

    	/*
		// Try to collect 4 highest weighted *unique* intersections
    	int isxCounter = 0;
    	while (isxCounter < 4) {
    		Core.MinMaxLocResult searchResult = Core.minMaxLoc(isxWeights);
    		Point maxWeightIdx = searchResult.maxLoc;
    		
    		Point isxPt = isxPts[(int)maxWeightIdx.y][(int)maxWeightIdx.x];
    		boolean isClose = false;
    		for(int i = 0; i < isxCounter; i++) {
    			if(Math.abs(isxPt.x - mCamCorners[i].x) <= 10 ||
    					Math.abs(isxPt.y - mCamCorners[i].y) <= 10)
    				isClose = true;
    		}
    		
    		if(!isClose) {
    			mCamCorners[isxCounter] = isxPt;
    			isxCounter++;
    		}
    		
    		// Zero out the this peak so we avoid repeats
    		isxWeights.put((int)maxWeightIdx.y, (int)maxWeightIdx.x, 0.0);
    		
    		if(isxCounter == lines.cols()*lines.cols())
    			break;
    	}
    	*/
    	
    	/*
    	// Draw circles all intersections
    	for(int i = 0; i < lines.cols(); i++) {
    		for(int j = 0; j < lines.cols(); j++) {
	    		Core.circle(mIntermediateMat, isxPts[i][j], 5, new Scalar(0,255,0,255), 1);
    		}
    	}
    	*/
    	
    	mFoundCorners = true;
    }
    
    private void bpCornerDetection(boolean drawLines)
    {
    	// Detect Hough lines
        Mat lines = new Mat();
        int threshold = 40;
        int minLineSize = 30;
        int lineGap = 40;

        Imgproc.HoughLinesP(mIntermediateMat, lines, 1, Math.PI/180, threshold, minLineSize, lineGap);

    	// Draw lines to screen
        if(drawLines) {
	    	Imgproc.cvtColor(mIntermediateMat, mIntermediateMat, Imgproc.COLOR_GRAY2RGBA, 4);
    		for (int x = 0; x < lines.cols(); x++) {
                  double[] vec = lines.get(0, x);
                  double x1 = vec[0], 
                         y1 = vec[1],
                         x2 = vec[2],
                         y2 = vec[3];
                  Point start = new Point(x1, y1);
                  Point end = new Point(x2, y2);
                  	
                  Core.line(mIntermediateMat, start, end, new Scalar(255,0,0,255), 1);
            }
        }
        
		// Calculate intersections
        ArrayList<Point> isxPts  = new ArrayList<Point>();
        for (int i = 0; i < lines.cols(); i++) {
			
        	double[] vec_1 = lines.get(0, i);
        	double x1_1 = vec_1[0], 
        		   y1_1 = vec_1[1],
                   x2_1 = vec_1[2],
                   y2_1 = vec_1[3];
            Point start_1 = new Point(x1_1, y1_1);
            Point end_1 = new Point(x2_1, y2_1);
			
        	for (int j = 0; j < lines.cols(); j++) {
        		
            	double[] vec_2 = lines.get(0, j);
            	double x1_2 = vec_2[0], 
            		   y1_2 = vec_2[1],
                       x2_2 = vec_2[2],
                       y2_2 = vec_2[3];
                Point start_2 = new Point(x1_2, y1_2);
                Point end_2 = new Point(x2_2, y2_2);
                
                // Calculate intersection point for every pair of lines
                Point isx = calcIntersection(start_1, end_1, start_2, end_2);
                
                // Make sure it's within a reasonable area
                double tolerance = 100;
                if (isx.x > -tolerance && isx.x < mIntermediateMat.cols()+tolerance &&
                		isx.y > -tolerance && isx.y < mIntermediateMat.rows()+tolerance )
                {
                	isxPts.add( isx );	// add to vector
                }
        	}
        }
        
        // Find bounding points
        double xMax = 0, yMax = 0, xMin = 10000, yMin = 10000;
        int xMax_ind = 0, yMax_ind = 0, xMin_ind = 0, yMin_ind = 0;
        
        for (int i = 0; i < isxPts.size(); i++) {
        	Point p = isxPts.get(i);
        	if(p.x > xMax) { xMax = p.x; xMax_ind = i; }
        	if(p.x < xMin) { xMin = p.x; xMin_ind = i; }
        	if(p.y > yMax) { yMax = p.y; yMax_ind = i; }
        	if(p.y < yMin) { yMin = p.y; yMin_ind = i; }
        }
    
    	// First separate left and right corners
    	int[] leftPts = new int[2];
    	int[] rightPts = new int[2];

    	leftPts[0] = xMin_ind;
    	rightPts[0] = xMax_ind;
    	
    	if(isxPts.get(yMin_ind).x > isxPts.get(yMax_ind).x) {
    		leftPts[1] = yMax_ind;
    		rightPts[1] = yMin_ind;
    	}
    	else {
    		leftPts[1] = yMin_ind;
    		rightPts[1] = yMax_ind;
    	}

    	// Then separate top and bottom corners
    	if(isxPts.get(leftPts[0]).y > isxPts.get(leftPts[1]).y) {
    		mCamCorners[0] = isxPts.get(leftPts[0]);
    		mCamCorners[3] = isxPts.get(leftPts[1]);
    	}
    	else {
    		mCamCorners[0] = isxPts.get(leftPts[1]);
    		mCamCorners[3] = isxPts.get(leftPts[0]);
    	}
    	
    	if(isxPts.get(rightPts[0]).y > isxPts.get(rightPts[1]).y) {
    		mCamCorners[1] = isxPts.get(rightPts[0]);
    		mCamCorners[2] = isxPts.get(rightPts[1]);
    	}
    	else {
    		mCamCorners[1] = isxPts.get(rightPts[1]);
    		mCamCorners[2] = isxPts.get(rightPts[0]);
    	}
    		
    	// mCamCorners organized: TL, TR, BR, BL
    	
    	mFoundCorners = true;
    }
    
    private void updateTouchPoint()
    {
		double[] xCoords = {mCamCorners[0].x, mCamCorners[1].x, mCamCorners[2].x, mCamCorners[3].x};
		Arrays.sort(xCoords);
		mTouchPoint.x = (xCoords[0] + xCoords[xCoords.length-1])/2;
    	
		double[] yCoords = {mCamCorners[0].y, mCamCorners[1].y, mCamCorners[2].y, mCamCorners[3].y};
		Arrays.sort(yCoords);
		mTouchPoint.y = (yCoords[0] + yCoords[yCoords.length-1])/2;
    }
    
    private double dist(Point p1, Point p2)
    {
    	double distance = Math.sqrt(Math.pow(p2.x-p1.x,2) + Math.pow(p2.y-p1.y,2));
    	return distance;
    }
    
    private Point calcIntersection(Point p1, Point p2, Point p3, Point p4)
    {
    	// p1 and p2 define LINE 1
    	// p3 and p4 define LINE 2
    	
    	double l1_m = (p2.y - p1.y) / (p2.x - p1.x);
    	double l2_m = (p4.y - p3.y) / (p4.x - p3.x);
    	
    	double l1_b = p1.y - l1_m * p1.x;
    	double l2_b = p3.y - l2_m * p3.x;

    	Point isx = new Point();
    	isx.x = (l2_b - l1_b) / (l1_m - l2_m);
    	isx.y = (l1_m*l2_b - l2_m*l1_b) / (l1_m - l2_m);
    	
    	return isx;
    }
    
    private void colorThreshold(Mat inputImg, Mat outputImg, double thresh)
    {
    	// Define 4D threshold scalars
    	Scalar uThresh = new Scalar(thresh,thresh,thresh,0);
    	Scalar lThresh = new Scalar(1/thresh,1/thresh,1/thresh,0);
    	
    	Scalar uBound = mColorData.mul(uThresh);
    	Scalar lBound = mColorData.mul(lThresh);
    	
    	// Make sure opacity thresholds are set to include all
    	lBound.val[3] = 0;
    	uBound.val[3] = 255;
    	
    	// Do color thresholding on image
    	Core.inRange(inputImg, lBound, uBound, outputImg);
    }
    
    private Mat isolateComponent(Mat binaryImg, Point cPoint)
    {
    	// Return only the connected component surrounding where the user has selected
    	// binaryImg	: binary input image
    	// cPoint 		: point within the connected component you want to isolate
    	
    	Mat floodMask = binaryImg.clone();
    	Imgproc.copyMakeBorder(floodMask, floodMask, 1, 1, 1, 1, Imgproc.BORDER_REPLICATE);
    	Core.bitwise_not(floodMask, floodMask);
    	
    	Mat floodOut = Mat.zeros(binaryImg.size(), binaryImg.type());
    	Imgproc.floodFill(floodOut, floodMask, cPoint, new Scalar(255));
    	
    	return floodOut;
    }
    
    private void getHarrisCorners(boolean drawCircles)
    {    	
    	// Detector parameters
    	int blockSize = 7;
    	int apertureSize = 5;
    	double k = 0.015;

    	// Detect corners
    	Mat harrisCornerMat = new Mat();
    	Imgproc.cornerHarris(mIntermediateMat, harrisCornerMat, blockSize, apertureSize, k, Imgproc.BORDER_DEFAULT);
        	
    	// Get most prominent 4 corners
    	for(int i = 0; i < 4; i++) {
    		// Find location of largest peak
    		Core.MinMaxLocResult searchResult = Core.minMaxLoc(harrisCornerMat);
    		mCamCorners[i] = searchResult.maxLoc;

    		// Zero out the area around the peak so we ignore repeats
    		Core.circle(harrisCornerMat, searchResult.maxLoc, 10, new Scalar(0), -1);
    	}
    	
    	// Set flag that says we've found the corners
    	mFoundCorners = true;
    	
    	if(drawCircles) {
	    	// Draw circles at corners
	    	Imgproc.cvtColor(mIntermediateMat, mIntermediateMat, Imgproc.COLOR_GRAY2RGBA, 4);
	    	for(int i = 0; i < 4; i++) {
	    		Core.circle(mIntermediateMat, mCamCorners[i], 5, new Scalar(0,255,0,255), 1);
	    	}
    	}
    }
    
    private void drawWarpedImg(Mat inputImg)
    {
    	// Determine organization of detected corners
    	// First separate left and right corners
    	int[] leftPts = new int[2];
    	int[] rightPts = new int[2];
    	int lCount = 0;
		int rCount = 0;

		// Find median of x coordinates
		double[] xCoords = {mCamCorners[0].x, mCamCorners[1].x, mCamCorners[2].x, mCamCorners[3].x};
		Arrays.sort(xCoords);
		double xMed = (xCoords[xCoords.length/2] + xCoords[xCoords.length/2-1])/2;
		
		// Compare each x coordinate to the median
    	for(int i=0; i<4; i++) {
    		if(mCamCorners[i].x <= xMed && lCount < 2) {
    			leftPts[lCount] = i;
    			lCount += 1;
    		}
    		else {
    			rightPts[rCount] = i;
    			rCount += 1;
    		}
    	}
    	
    	// Then separate top and bottom corners
    	int tl_idx, bl_idx, tr_idx, br_idx;
    	if(mCamCorners[leftPts[0]].y > mCamCorners[leftPts[1]].y) {
    		tl_idx = leftPts[1];
    		bl_idx = leftPts[0];
    	}
    	else {
    		tl_idx = leftPts[0];
    		bl_idx = leftPts[1];
    	}
    	if(mCamCorners[rightPts[0]].y > mCamCorners[rightPts[1]].y) {
    		tr_idx = rightPts[1];
    		br_idx = rightPts[0];
    	}
    	else {
    		tr_idx = rightPts[0];
    		br_idx = rightPts[1];
    	}
    	
    	// Reorganize mCamCorners
    	Point[] orgCorners = {mCamCorners[tl_idx], mCamCorners[tr_idx], mCamCorners[br_idx], mCamCorners[bl_idx]};
    	
    	// Create homography matrix from user img --> camera rectangle
    	MatOfPoint2f dest = new MatOfPoint2f(orgCorners);
    	Point[] imgCorners = {
					new Point(0,0),
					new Point(inputImg.rows(),0),
    				new Point(inputImg.rows(),inputImg.cols()),
					new Point(0,inputImg.cols()) };
    	MatOfPoint2f source = new MatOfPoint2f(imgCorners);
    	
    	Mat H = Calib3d.findHomography(source, dest);

    	// don't do anything if there isn't a valid homography returned by the findHomography function
    	if(H.empty())
    		return;
    	
    	// Warp user img to camera roi
    	Mat warpImg = new Mat();
    	//Imgproc.warpPerspective(inputImg, warpImg, H, mRgba.size());
    	Imgproc.warpPerspective(inputImg, warpImg, H, mRgba.size(), Imgproc.INTER_LINEAR);
    	
    	// Generate corresponding mask
    	Mat imgMask = new Mat();
    	Imgproc.threshold(warpImg, imgMask, 0.1, 255, Imgproc.THRESH_BINARY);
    	
    	// Copy warped user image to correct location in camera img
    	warpImg.copyTo(mRgba, imgMask);
    }
    
    // Functions to select Bitmap from user's photos
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                selectedImageURI = data.getData(); // GET REAL URI
                Bitmap bmp = null;
                Bitmap scaled_bmp = null;
                
                try {
                	bmp = getBitmapFromUri(selectedImageURI);
                	scaled_bmp = Bitmap.createScaledBitmap(bmp, 200, 200, false);  	
                } catch (IOException e) {};
                
                mBitmap = scaled_bmp;
                
                // Convert Bitmap to Mat
                mUserImg = new Mat();
            	Utils.bitmapToMat(mBitmap, mUserImg);
            }                
        }
    }
    
    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }
    
    // Display error popup
    private void displayPopup(final int err)
    {
    	mViewMode = VIEW_MODE_RGBA;
    	mErr = err;
    	
        FinalProject.this.runOnUiThread(new Runnable() {
        	public void run() {
		    	AlertDialog.Builder alertBuilder = new AlertDialog.Builder(FinalProject.this);
		    	alertBuilder.setTitle("Error");
		    	
		    	// Message dependent on specific error
		    	switch(mErr) {
		    	case(ERR_NO_COLOR):
		    		alertBuilder.setMessage("Please select a threshold color...");
		    		break;
		    	case(ERR_NO_USER_IMAGE):
		    		alertBuilder.setMessage("Please select an image...");
	    			break;
		    	}		  
		    	
		    	alertBuilder.setPositiveButton("OK",
		            new DialogInterface.OnClickListener() {
		            	public void onClick(DialogInterface dialog, int id) {
		    		    	
		            		// Result dependent on specific error
		            		switch(mErr) {
		    		    	case(ERR_NO_COLOR):
		    		    		mViewMode = VIEW_MODE_RGBA;
		    		    		break;
		    		    	case(ERR_NO_USER_IMAGE):
		    		    		onOptionsItemSelected(mItemChooseImage);
		    		    		mViewMode = VIEW_MODE_ALL_PROCESSING;
		    	    			break;
		    		    	}	
		            		
		            		dialog.dismiss();
		            	}
		        	});
		    	alertBuilder.setNegativeButton("Cancel", 
					new DialogInterface.OnClickListener() {
		            	public void onClick(DialogInterface dialog, int id) {
		            		mViewMode = VIEW_MODE_RGBA;
		            		dialog.dismiss();
		            	}
		        	});
		        AlertDialog alertBox = alertBuilder.create();
        		alertBox.show();
        	}
        });
    }
    
}
