package kanwar.com.uscurrencyreader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 , View.OnTouchListener{

    JavaCameraView mJavaCameraView;
    Mat imageMAt,gray;
    TextToSpeech textTOspeech;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "LOADED: SUCCESS");

                    mJavaCameraView.enableView();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mJavaCameraView = (JavaCameraView) findViewById(R.id.HelloOpencvView);
        mJavaCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mJavaCameraView.setCvCameraViewListener(this);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);

        mJavaCameraView.setOnTouchListener(this);
        textTOspeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textTOspeech.setLanguage(Locale.US);
                }
            }
        });

    }

    @Override  public void onResume()  {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");

        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause()
        {
            super.onPause();
            if (mJavaCameraView != null)
                mJavaCameraView.disableView();
        }


    public void onDestroy()
        {
            super.onDestroy();
            if (mJavaCameraView != null)
                mJavaCameraView.disableView();
        }



    @Override
    public void onCameraViewStopped() { }

    @Override
    public void onCameraViewStarted(int width, int height) { }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
        {
            imageMAt = inputFrame.rgba();
            gray =inputFrame.gray();
            return imageMAt;

        }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        System.out.println("getting inside");
        Mat image01 = gray;
        String toSpeak = "No matches found";

        Mat grayImage01 = new Mat(image01.rows(), image01.cols(), CvType.CV_8UC1);
        Imgproc.pyrDown(gray, image01);
        Core.normalize(image01, grayImage01, 0, 255, Core.NORM_MINMAX);
        MatOfKeyPoint keyPoint01 = new MatOfKeyPoint();

        FeatureDetector orbDetector = FeatureDetector.create(FeatureDetector.ORB);
        orbDetector.detect(image01, keyPoint01);

        KeyPoint[] keypoints = keyPoint01.toArray();
        System.out.println(keypoints);
        MatOfKeyPoint objectDescriptors = new MatOfKeyPoint();

        DescriptorExtractor orbExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        orbExtractor.compute(image01, keyPoint01, objectDescriptors);

        Mat outputImage = new Mat();
        Scalar newKeypointColor = new Scalar(255, 0, 0);
        Features2d.drawKeypoints(image01, keyPoint01, outputImage, newKeypointColor, 0);
        boolean flag=false;
        for (int note = 1; note <= 14; note++) {
            if(flag==true){
                return  false;
            }
            String filename = "us"+note;
            int id = getResources().getIdentifier(filename, "drawable", getPackageName());
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), id);

            System.out.println(bitmap.getHeight() + "  " + bitmap.getWidth());
            Mat m = new Mat();
            Utils.bitmapToMat(bitmap, m); // m is the Mat(RESOURCE).

            MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
            MatOfKeyPoint sceneDescriptors = new MatOfKeyPoint();

            orbDetector.detect(m, sceneKeyPoints);
            orbExtractor.compute(m, sceneKeyPoints, sceneDescriptors);

            List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
            DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
            descriptorMatcher.knnMatch(objectDescriptors, sceneDescriptors, matches, 2);
            System.out.println("Calculating good match list...");
            LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();
            float nndrRatio = 0.7f;
            for (int i = 0; i < matches.size(); i++) {
                MatOfDMatch matofDMatch = matches.get(i);
                DMatch[] dmatcharray = matofDMatch.toArray();
                DMatch m1 = dmatcharray[0];
                DMatch m2 = dmatcharray[1];
                if (m1.distance <= m2.distance * nndrRatio) {
                    goodMatchesList.addLast(m1);
                }
            }
            System.out.println("Total matches:" + goodMatchesList.size());
            if (goodMatchesList.size() >= 10) {
                System.out.println("currecy loop "+note);

                switch (note){
                    case 1:  toSpeak = "O R B result says its 1 Dollar Back";   flag=true;   break;
                    case 2:  toSpeak = "O R B result says its 1 Dollar Front";  flag=true;   break;
                    case 3:  toSpeak = "O R B result says its 2 Dollar Back";   flag=true;   break;
                    case 4:  toSpeak = "O R B result says its 2 Dollar Front";  flag=true;   break;
                    case 5:  toSpeak = "O R B result says its 5 Dollar Back";   flag=true;   break;
                    case 6:  toSpeak = "O R B result says its 5 Dollar Front";  flag=true;   break;
                    case 7:  toSpeak = "O R B result says its 10 Dollar Back";  flag=true;   break;
                    case 8:  toSpeak = "O R B result says its 10 Dollar Front"; flag=true;   break;
                    case 9:  toSpeak = "O R B result says its 20 Dollar Back";  flag=true;   break;
                    case 10: toSpeak = "O R B result says its 20 Dollar Front"; flag=true;   break;
                    case 11: toSpeak = "O R B result says its 50 Dollar Back";  flag=true;   break;
                    case 12: toSpeak = "O R B result says its 50 Dollar Front"; flag=true;   break;
                    case 13: toSpeak = "O R B result says its 100 Dollar Back"; flag=true;   break;
                    case 14: toSpeak = "O R B result says its 100 Dollar Front";flag=true;  break;
                    default: toSpeak = " No matches found. I think you are not focusing on currency or I need more training from by boss"; break;
                }
                System.out.println("Object Found!!!");
                textTOspeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                System.out.println("Currency loop:"+note);
                System.out.println("Object Not Found");
            }
        }
        if(toSpeak.equals("No matches found")){
            textTOspeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
        }
        return false;
    }
}
