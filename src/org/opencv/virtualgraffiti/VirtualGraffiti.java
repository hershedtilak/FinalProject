package org.opencv.virtualgraffiti;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.virtualgraffiti.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

public class VirtualGraffiti extends Activity implements CvCameraViewListener2 {
    private static final String    TAG = "OCVSample::Activity";

    private static final int       VIEW_MODE_RGBA     = 0;
    private static final int       VIEW_MODE_COLOR_THRESHOLD     = 1;
    private static final int       VIEW_MODE_ALL_PROCESSING    = 2;

    private static final int	   ERR_NO_COLOR = 0;
    private static final int	   ERR_NO_USER_IMAGE = 1;
    
    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat					   mWarpMask;
    
    private Point[]				   mCamCorners = new Point[4];
    private boolean				   mFoundCorners = false;
    
    private Bitmap				   mBitmap = null;
    private Mat					   mUserImg;
    private Mat					   mccUserImg;
    
    private Size				   mKernelSize = new Size(5,5);
    private Scalar				   mColorData;
    private Point				   mTouchPoint;
    private Boolean				   mTouchEvent = false;

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

    public VirtualGraffiti() {
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
        
        mOpenCvCameraView.setMaxFrameSize(400,400);
        
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        
        mViewMode = VIEW_MODE_RGBA;
        
        // set Touch Listener
        mOpenCvCameraView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event)
            {
            	if (event.getY() > mOpenCvCameraView.getHeight() - 150.0f)
    			{
    				VirtualGraffiti.this.openOptionsMenu();
    				return true;
    			}
            	
            	int cols = mRgba.cols();
                int rows = mRgba.rows();
                
                int x = (int)event.getX() * cols/mOpenCvCameraView.getWidth();
                int y = (int)event.getY() * rows/mOpenCvCameraView.getHeight();
                
                if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
                else {
                	mTouchPoint.x = x;
                	mTouchPoint.y = y;
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
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
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
        mWarpMask = new Mat();
        mTouchPoint = new Point();
        mColorData = new Scalar(0,0,0,0);
        mccUserImg = new Mat();
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mIntermediateMat.release();
        mccUserImg.release();
    }
    
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {  	
    	final int viewMode = mViewMode;
        mRgba = inputFrame.rgba();
        
    	// Set color upon user selection
    	if(mTouchEvent)
    	{
    		double[] vals = mRgba.get((int)mTouchPoint.y, (int)mTouchPoint.x);
    		mColorData.set(vals);
//    		Uncomment to re-enable HSV Contrast Adjustment
//        	if(mUserImg != null)
//        		mccUserImg = contrastAdjustment(mUserImg);
    	}
        
        switch (viewMode) {
        case VIEW_MODE_COLOR_THRESHOLD:

        	if (mColorData == null) {
        		mViewMode = VIEW_MODE_RGBA;
        		break;
        	}
        	
        	// Color threshold
        	double thresh2 = 1.5;
        	colorThreshold(mRgba, mIntermediateMat, thresh2, mColorData);
	
        	// Isolate the object of interest
        	mIntermediateMat = isolateComponent(mIntermediateMat, mTouchPoint);
        	mWarpMask = mIntermediateMat.clone();
        	
        	// Low-pass filter
        	Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, mKernelSize, 20.0);
        	
        	// Get corners
        	getHarrisCorners(true);

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
        	
        	// Color threshold
        	double thresh = 1.5;
        	colorThreshold(mRgba, mIntermediateMat, thresh, mColorData);
	
        	// Isolate the object of interest
        	mIntermediateMat = isolateComponent(mIntermediateMat, mTouchPoint);
        	mWarpMask = mIntermediateMat.clone();
        	
        	// Low-pass filter
        	Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, mKernelSize, 20.0);
        	
        	// Get corners
        	getHarrisCorners(false);
        	
        	// Draw image (change to mccUserImg when Contrast Correction is enabled)
        	drawWarpedImg(mUserImg);
        	
            break;
        }

        if(mTouchEvent) {
        	// Draw color circle where user touched screen 
        	Imgproc.circle(mRgba, mTouchPoint, 30, mColorData, 5);
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
    
    private Mat contrastAdjustment(Mat img)
    {
    	Mat HSVimg = new Mat();
    	Mat HSVmrgba = new Mat();
    	
    	int chan = img.channels();
    	if(chan != 4) return img; // make sure we have an RGBA color img
    	
    	// convert images to HSV color space
    	Imgproc.cvtColor(img, HSVimg, Imgproc.COLOR_RGBA2RGB);
    	Imgproc.cvtColor(HSVimg, HSVimg, Imgproc.COLOR_RGB2HSV);

    	Imgproc.cvtColor(mRgba, HSVmrgba, Imgproc.COLOR_RGBA2RGB);
    	Imgproc.cvtColor(HSVmrgba, HSVmrgba, Imgproc.COLOR_RGB2HSV);   	
    	    		
    	List<Mat> HSVimgChannels = new ArrayList<Mat>();
    	List<Mat> HSVrgbaChannels = new ArrayList<Mat>();
		Core.split(HSVimg, HSVimgChannels);
    	Core.split(HSVmrgba, HSVrgbaChannels);
		
    	// find maximum and minimum Value and Saturation of input scene
		double maxSatmrgba=0, minSatmrgba=0, maxValmrgba=0, minValmrgba=0;
		for(int c = 1; c < 3; c++) {
    		Core.MinMaxLocResult searchResult = Core.minMaxLoc(HSVrgbaChannels.get(c));
    		if(c==1) {
    			maxValmrgba = searchResult.maxVal;
    			minValmrgba = searchResult.minVal;
    		}
    		else if(c==2) {
    			maxSatmrgba = searchResult.maxVal;
    			minSatmrgba = searchResult.minVal;
    		}
		}
		
		// find maximum and minimum Value and Saturation of user image
		double maxSatimg=0, minSatimg=0, maxValimg=0, minValimg=0;
		for(int c = 1; c < 3; c++) {
    		Core.MinMaxLocResult searchResult = Core.minMaxLoc(HSVimgChannels.get(c));
    		if(c==1) {
    			maxValimg = searchResult.maxVal;
    			minValimg = searchResult.minVal;
    		}
    		else if(c==2) {
    			maxSatimg = searchResult.maxVal;
    			minSatimg = searchResult.minVal;
    		}
		}
		
		// Shift image Saturation to have same minimum as scene
		// Scale image Saturation to cover same range as Saturation of scene
		Mat newS = new Mat();
		Mat imgS = HSVimgChannels.get(1).clone();
		imgS.convertTo(newS, -1, 1, minSatmrgba-minSatimg);
		newS.convertTo(newS, -1, (maxSatimg-minSatimg)/(maxSatmrgba-minSatmrgba), 0);

		// Shift image Value to have same minimum as scene
		// Scale image Value to cover same range as Saturation of scene
		Mat newV = new Mat();
		Mat imgV = HSVimgChannels.get(2).clone();
		imgV.convertTo(newV, -1, 1, minValmrgba-minValimg);
		newV.convertTo(newV, -1, (maxValimg-minValimg)/(maxValmrgba-minValmrgba), 0);
		
		HSVimgChannels.remove(2);
		HSVimgChannels.remove(1);
		HSVimgChannels.add(newS);
		HSVimgChannels.add(newV);
		
    	Mat mergeResult = new Mat();
		Core.merge(HSVimgChannels, mergeResult);

		Mat result = new Mat();
		Imgproc.cvtColor(mergeResult, result, Imgproc.COLOR_HSV2RGB);
		Imgproc.cvtColor(result, result, Imgproc.COLOR_RGB2RGBA);
    	
		HSVmrgba.release();
		HSVmrgba.release();
		newS.release();
		newV.release();
		mergeResult.release();
		
		HSVimgChannels.clear();
		HSVrgbaChannels.clear();
		
    	return result;
    }
     
    private void updateTouchPoint()
    {
		double[] xCoords = {mCamCorners[0].x, mCamCorners[1].x, mCamCorners[2].x, mCamCorners[3].x};
		Arrays.sort(xCoords);
		double xMed = (xCoords[0] + xCoords[xCoords.length-1])/2;
		double xDist = xCoords[xCoords.length-1] - xCoords[0];
		
		// update surface x position to center of x coordinates of Harris Points
		// only update if distance moved is less than one times the width of the detected surface
		if(xMed - mTouchPoint.x < xDist && xMed - mTouchPoint.x > -1*xDist)
			mTouchPoint.x = (xCoords[0] + xCoords[xCoords.length-1])/2;
    	
		double[] yCoords = {mCamCorners[0].y, mCamCorners[1].y, mCamCorners[2].y, mCamCorners[3].y};
		Arrays.sort(yCoords);
		double yMed = (yCoords[0] + yCoords[yCoords.length-1])/2;
		double yDist = yCoords[yCoords.length-1] - yCoords[0];
		
		// update surface y position to center of y coordinates of Harris Points
		// only update if distance moved is less than one times the height of the detected surface
		if(yMed - mTouchPoint.y < yDist && yMed - mTouchPoint.y > -1*yDist)
			mTouchPoint.y = (yCoords[0] + yCoords[yCoords.length-1])/2;
    }
    
    private void colorThreshold(Mat inputImg, Mat outputImg, double thresh, Scalar color)
    {
    	// Define 4D threshold scalars
    	Scalar uThresh = new Scalar(thresh,thresh,thresh,0);
    	Scalar lThresh = new Scalar(thresh-1,thresh-1,thresh-1,0);
    	
    	Scalar uBound = color.mul(uThresh);
    	Scalar lBound = color.mul(lThresh);
    	
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
    	Core.copyMakeBorder(floodMask, floodMask, 1, 1, 1, 1, Core.BORDER_REPLICATE);
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
    	Imgproc.cornerHarris(mIntermediateMat, harrisCornerMat, blockSize, apertureSize, k, Core.BORDER_DEFAULT);
    	
    	// Get most prominent 4 corners
    	for(int i = 0; i < 4; i++) {
	    	// Find location of largest peak
	    	Core.MinMaxLocResult searchResult = Core.minMaxLoc(harrisCornerMat);
	    	mCamCorners[i] = searchResult.maxLoc;
	    	
	    	// Zero out the area around the peak so we ignore repeats
	    	Imgproc.circle(harrisCornerMat, searchResult.maxLoc, 10, new Scalar(0), -1);
    	}
    	
    	// Set flag that says we've found the corners
    	mFoundCorners = true;
    	
    	if(drawCircles) {
	    	// Draw circles at corners
	    	Imgproc.cvtColor(mIntermediateMat, mIntermediateMat, Imgproc.COLOR_GRAY2RGBA, 4);
	    	for(int i = 0; i < 4; i++) {
	    		Imgproc.circle(mIntermediateMat, mCamCorners[i], 5, new Scalar(0,255,0,255), 1);
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
    	Imgproc.warpPerspective(inputImg, warpImg, H, mRgba.size(), Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, mColorData);

    	// Copy warped user image to correct location in camera img
    	warpImg.copyTo(mRgba, mWarpMask);
    	
    	warpImg.release();
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
                	scaled_bmp = Bitmap.createScaledBitmap(bmp, 1000, 1000, false);  	
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
    	
        VirtualGraffiti.this.runOnUiThread(new Runnable() {
        	public void run() {
		    	AlertDialog.Builder alertBuilder = new AlertDialog.Builder(VirtualGraffiti.this);
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
