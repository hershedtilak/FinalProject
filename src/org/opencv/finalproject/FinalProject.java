package org.opencv.finalproject;

import java.io.FileDescriptor;
import java.io.IOException;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.finalproject.R;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
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
    private static final int       VIEW_MODE_CANNY    = 2;

    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat					   mHarrisCornerMat;
    
    private Bitmap				   mBitmap = null;
    
//    private double[]			   mColorData;
    private Scalar				   mColorData;
    private Point				   mTouchPoint;
    private Boolean				   mTouchEvent = false;

    private int					   mFrameCounter = 0;
    
    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewColorThresholded;
    private MenuItem               mItemPreviewCanny;
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
            public boolean onTouch(View v, MotionEvent event) {
    
            	/*
            	int action = event.getActionMasked();
            	
            	switch(action) {
            		case MotionEvent.ACTION_DOWN:
            			// Bring up Menu if user touches at bottom of screen (left of screen in portrait mode)
            			if (event.getY() > mOpenCvCameraView.getHeight() - 150.0f)
            				FinalProject.this.openOptionsMenu();
            			// Otherwise store value of pixel at touch location (but only if view mode is RGBA)
            			else if (mViewMode == VIEW_MODE_RGBA)
            			{
            				int x = (int)(event.getX()*mRgba.size().width/mOpenCvCameraView.getWidth());
            				int y = (int)(event.getY()*mRgba.size().height/mOpenCvCameraView.getHeight());
            				mColorData = mRgba.get(y,x);
            			}
            			break;
            	}
				*/
            	
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
        mItemPreviewCanny = menu.add("Canny");
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
        mHarrisCornerMat = new Mat(height, width, CvType.CV_32FC1);
        //mColorData = new Scalar(0,0,0,0);        
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mIntermediateMat.release();
        mHarrisCornerMat.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {  	
    	final int viewMode = mViewMode;
        mRgba = inputFrame.rgba();
        
    	// Set color upon user selection
    	if(mTouchEvent)	mColorData = new Scalar(mRgba.get((int)mTouchPoint.y, (int)mTouchPoint.x));
        
        switch (viewMode) {
        case VIEW_MODE_COLOR_THRESHOLD:
        	// if color isn't set, just reset to RGBA mode
        	if (mColorData == null) {
        		mViewMode = VIEW_MODE_RGBA;
        		break;
        	}
        	
        	// Perform color thresholding (right now the threshold is hard-coded)        	
        	double thresh = 1.5;
        	
        	Scalar uThresh = new Scalar(thresh,thresh,thresh,0);
        	Scalar lThresh = new Scalar(1/thresh,1/thresh,1/thresh,0);
        	
        	Scalar uBound = mColorData.mul(uThresh);
        	Scalar lBound = mColorData.mul(lThresh);
        	
        	lBound.val[3] = 0;
        	uBound.val[3] = 255;
        	
        	Core.inRange(mRgba, lBound, uBound, mIntermediateMat);
//        	Imgproc.cvtColor(mIntermediateMat, mIntermediateMat, Imgproc.COLOR_GRAY2RGBA);
        	        	
        	// Low Pass Filter
        	Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, new Size(5,5), 10.0);        	
        	
        	// Harris Corners
        	getHarrisCorners();
        	        	        	
        	// Draw information regarding thresholding
        	boolean drawThreshInfo = false;
        	if(drawThreshInfo) {
	        	Core.putText(mIntermediateMat, "Color = " + mColorData.toString(), new Point(50,mIntermediateMat.rows()-150),
						Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,0,125,255), 2);
	        	Core.putText(mIntermediateMat, "LB = " + lBound.toString(), new Point(50,mIntermediateMat.rows()-100),
						Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,0,125,255), 2);
	        	Core.putText(mIntermediateMat, "UB = " + uBound.toString(), new Point(50,mIntermediateMat.rows()-50),
						Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,0,125,255), 2);
        	}
        	        	
        	mRgba = mIntermediateMat;
            break;
            
        case VIEW_MODE_RGBA:
            // input frame has RBGA format
            break;
            
        case VIEW_MODE_CANNY:
            // input frame has gray scale format
            Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80, 100);
            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            break;
        }

    	// Draw color circle where user touched screen 
        if(mTouchEvent) {        	
        	// Can draw the coordinates to screen for debugging purposes
//        	Core.putText(mRgba, mTouchPoint.y + " x " + mTouchPoint.x, new Point(20,mRgba.rows()-20),
//					Core.FONT_HERSHEY_SIMPLEX, 0.3, new Scalar(255,60,60,255), 2);
        	
        	Core.circle(mRgba, mTouchPoint, 30, mColorData, 5);
    		mTouchEvent = false;
    	}
        
        return mRgba;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewColorThresholded) {
            mViewMode = VIEW_MODE_COLOR_THRESHOLD;
        } else if (item == mItemPreviewCanny) {
            mViewMode = VIEW_MODE_CANNY;
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
    
    private void getHarrisCorners()
    {    	
    	/// Detector parameters
    	int blockSize = 7;
    	int apertureSize = 5;
    	double k = 0.015;
//    	double thresh = 0.3;

    	/// Detect corners
    	Imgproc.cornerHarris(mIntermediateMat, mHarrisCornerMat, blockSize, apertureSize, k, Imgproc.BORDER_DEFAULT);
    
    	/// Draw a circle around corners (for illustration purposes, comment out for performance)
//		Imgproc.cvtColor(mIntermediateMat, mIntermediateMat, Imgproc.COLOR_GRAY2RGBA, 4);
//    	for( int j = 0; j < mHarrisCornerMat.rows() ; j++ ) {
//    		for( int i = 0; i < mHarrisCornerMat.cols(); i++ ) {
//    			if(mHarrisCornerMat.get(j,i)[0] > thresh)
//    	        {
//    				Core.circle(mIntermediateMat, new Point(i,j), 5, new Scalar(0,255,0,255), 1);
//    	        }
//    	    }
//    	}
    	
    	/// Get most prominent 4 corners
    	Point[] corners = new Point[4];
    	for(int i = 0; i < 4; i++) {
    		// Find location of largest peak
    		Core.MinMaxLocResult searchResult = Core.minMaxLoc(mHarrisCornerMat);
    		corners[i] = searchResult.maxLoc;

    		// Zero out the area around the peak so we ignore repeats
    		Core.circle(mHarrisCornerMat, searchResult.maxLoc, 5, new Scalar(0), -1);
    	}
    	
    	/// Draw circles at corners
    	Imgproc.cvtColor(mIntermediateMat, mIntermediateMat, Imgproc.COLOR_GRAY2RGBA, 4);
    	for(int i = 0; i < 4; i++) {
    		Core.circle(mIntermediateMat, corners[i], 5, new Scalar(0,255,0,255), 1);
    	}
    	
    	
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

}
