package kanwar.com.uscurrencyreader;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

//imports for Google Cloud Vision API
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 , View.OnTouchListener{

    JavaCameraView mJavaCameraView;
    Mat imageMAt,gray;
    TextToSpeech textTOspeech;
    TextToSpeech CLOUDSPEECH;
    Button mOrbButton;
    Button mCloudButton;
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final String TAG = "LogTAG";

    Vision vision;

    int count = 0;
    int COUNT_GOOLGE_CLOUD_TRIGGER = 300;

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

        CLOUDSPEECH = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    CLOUDSPEECH.setLanguage(Locale.US);
                }
            }
        });

        mCloudButton = (Button)findViewById(R.id.cloudButton);
        mOrbButton = (Button) findViewById(R.id.orbButton);
        mOrbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orbWay();
            }
        });

        mCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cloudWay();
            }
        });

        Vision.Builder visionBuilder = new Vision.Builder(
                new NetHttpTransport(),
                new AndroidJsonFactory(),
                null);

        visionBuilder.setVisionRequestInitializer(
                new VisionRequestInitializer("AIzaSyCHIQB_S72SeNsriObra5SyV9SIM9pj0n8"));


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

            System.out.print(bitmap);
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
            } else
                {
                System.out.println("Currency loop:"+note);
                System.out.println("Object Not Found");
                     }
        }
        if(toSpeak.equals("No matches found")){
            textTOspeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
        }
        return false;
    }

    public  Boolean orbWay(){

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

            System.out.print(bitmap);
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
            } else
            {
                System.out.println("Currency loop:"+note);
                System.out.println("Object Not Found");
            }
        }
        if(toSpeak.equals("No matches found")){
            textTOspeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
        }
        return false;
    }
    public void cloudWay(){

        Bitmap bitmap = Bitmap.createBitmap(imageMAt.width(),  imageMAt.height(),Bitmap.Config.ARGB_8888);;
        Utils.matToBitmap(imageMAt,bitmap);
        Bitmap smallerBitmap = this.scaleBitmapDown(bitmap, 640);


        try {

            callCloudVision(bitmap, imageMAt);

        }catch(IOException i){Log.e(TAG, "it is failing"+i.getMessage());}

    }

    /**
     * this method uses the classes vision object to make a request to Google Cloud Vision API
     * @param bitmap  input Bitmap image
     * @param imageMat  output Mat to be displayed via OpenCV
     */
    public void callCloudVision(final Bitmap bitmap, final Mat imageMat) throws IOException {



        Log.i("TEST", "before cloud api call");

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer("AIzaSyCHIQB_S72SeNsriObra5SyV9SIM9pj0n8") {
                                /**
                                 * We override this so we can inject important identifying fields into the HTTP
                                 * headers. This enables use of a restricted cloud platform API key.
                                 */
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                    String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("LABEL_DETECTION");
                            labelDetection.setMaxResults(10);
                            add(labelDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    String uhuh = convertResponseToString(response);
                    CLOUDSPEECH.speak(uhuh, TextToSpeech.QUEUE_FLUSH, null, null);

                    return convertResponseToString(response);


                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {


              //  CLOUDSPEECH.speak(result, TextToSpeech.QUEUE_FLUSH, null, null);

            }
        }.execute();
    }

    /**
     * method to create a String from the response of a Google Vision Label Detection on an image
     * @param response
     * @return
     */
    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        String message = "I found these things:\n\n";

        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                message += String.format(Locale.US, "%.3f: %s", label.getScore(), label.getDescription());
                message += "\n";
            }
        } else {
            message += "nothing";
        }

        return message;
    }



    /**
     * method to resize the original bitmap to produce a bitMap with maximum dimension of maxDimension on widht or height
     * but, also keep aspect ration the same as original
     * @param bitmap  input original bitmap
     * @param maxDimension   maximum or either Width or Height of new rescaled image keeping original aspect ratio
     * @return rescaled image with same aspect ratio
     */

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

}
