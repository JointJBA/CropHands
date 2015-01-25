package com.fuckwits.hackathon.crophands;

import android.util.Log;
import android.util.Pair;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.photo.Photo;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.convexHull;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.minAreaRect;

/**
 * Created by Belal on 1/25/2015.
 */
public class HandRecognizer {

    public double dist(Point x, Point y) {
        return (x.x - y.x) * (x.x - y.x) + (x.y - y.y) * (x.y - y.y);
    }

    Pair<Point, Double> circleFromPoints(Point p1, Point p2, Point p3) {
        double offset = Math.pow(p2.x, 2) + Math.pow(p2.y, 2);
        double bc = (Math.pow(p1.x, 2) + Math.pow(p1.y, 2) - offset) / 2.0;
        double cd = (offset - Math.pow(p3.x, 2) - Math.pow(p3.y, 2)) / 2.0;
        double det = (p1.x - p2.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p2.y);
        double TOL = 0.0000001;
        if (Math.abs(det) < TOL) {
            Log.d("a", "POINTS TOO CLOSE");
            return new Pair<>(new Point(0, 0), new Double(0));
        }

        double idet = 1 / det;
        double centerx = (bc * (p2.y - p3.y) - cd * (p1.y - p2.y)) * idet;
        double centery = (cd * (p1.x - p2.x) - bc * (p2.x - p3.x)) * idet;
        double radius = Math.sqrt(Math.pow(p2.x - centerx, 2) + Math.pow(p2.y - centery, 2));

        return new Pair<>(new Point(centerx, centery), new Double(radius));
    }

    public boolean stop = false;

    public HandRecognizer(byte[] image) {
        VideoCapture vc = new VideoCapture(0);
        if (vc.isOpened()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Log.d("cam", "cant sleep");
            }
            Log.d("cam", "open");
            int backgroundFrame = 500;
            Mat frame = new Mat();
            Mat back = new Mat();
            Mat fore = new Mat();
            Vector<Pair<Point, Double>> palm_centers = new Vector<>();
            BackgroundSubtractorMOG2 bg = new BackgroundSubtractorMOG2();
            bg.setDouble("nmixtures", 3);
            bg.setBool("detectShadows", false);
            while (!stop) {
                Vector<Vector<Point>> contours = new Vector<>();
                vc.read(frame);
                if (backgroundFrame > 0) {
                    bg.apply(frame, fore);
                } else {
                    bg.apply(frame, fore, 0);
                }
                erode(fore, fore, new Mat());
                dilate(fore, fore, new Mat());
                findContours(fore, contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);
                for (int i = 0; i < contours.size(); i++)
                    //Ignore all small insignificant areas
                    if (contourArea(contours.get(i)) >= 5000) {
                        //Draw contour
                        Vector<Vector<Point>> tcontours = new Vector<>();
                        tcontours.add(contours[i]);
                        drawContours(frame, tcontours, -1, cv::Scalar (0, 0, 255),2);

                        //Detect Hull in current contour
                        Vector<Vector<Point>> hulls = new Vector<>(1);
                        Vector<Vector<int>> hullsI (1)= new Vector<>(1);
                        convexHull(Mat(tcontours.get(0)), hulls[0], false);
                        convexHull(Mat(tcontours[0]), hullsI[0], false);
                        drawContours(frame, hulls, -1, new Scalar(0, 255, 0),2);

                        //Find minimum area rectangle to enclose hand
                        RotatedRect rect = minAreaRect(new Mat(tcontours.get(0)));

                        //Find Convex Defects
                        Vector<Vec4> defects;
                        if (hullsI[0].size() > 0) {
                            Point2f rect_points[ 4];
                            rect.points(rect_points);
                            for (int j = 0; j < 4; j++)
                                line(frame, rect_points[j], rect_points[(j + 1) % 4], Scalar(255, 0, 0), 1, 8);
                            Point rough_palm_center;
                            convexityDefects(tcontours[0], hullsI[0], defects);
                            if (defects.size() >= 3) {
                                Vector<Point> palm_points;
                                for (int j = 0; j < defects.size(); j++) {
                                    int startidx = defects[j][0];
                                    Point ptStart (tcontours[0][startidx]);
                                    int endidx = defects[j][1];
                                    Point ptEnd = new Point(tcontours[0][endidx]);
                                    int faridx = defects[j][2];
                                    Point ptFar (tcontours[0][faridx]);
                                    //Sum up all the hull and defect points to compute average
                                    rough_palm_center += ptFar + ptStart + ptEnd;
                                    palm_points.add(ptFar);
                                    palm_points.add(ptStart);
                                    palm_points.add(ptEnd);
                                }

                                //Get palm center by 1st getting the average of all defect points, this is the rough palm center,
                                //Then U chose the closest 3 points ang get the circle radius and center formed from them which is the palm center.
                                rough_palm_center.x /= defects.size() * 3;
                                rough_palm_center.y /= defects.size() * 3;
                                Point closest_pt = palm_points[0];
                                Vector<pair<double, int>> distvec;
                                for (int i = 0; i < palm_points.size(); i++)
                                    distvec.add(make_pair(dist(rough_palm_center, palm_points[i]), i));
                                sort(distvec.begin(), distvec.end());

                                //Keep choosing 3 points till you find a circle with a valid radius
                                //As there is a high chance that the closes points might be in a linear line or too close that it forms a very large circle
                                pair<Point, double> soln_circle;
                                for (int i = 0; i + 2 < distvec.size(); i++) {
                                    Point p1 = palm_points[distvec[i + 0].second];
                                    Point p2 = palm_points[distvec[i + 1].second];
                                    Point p3 = palm_points[distvec[i + 2].second];
                                    soln_circle = circleFromPoints(p1, p2, p3);//Final palm center,radius
                                    if (soln_circle.second != 0)
                                        break;
                                }

                                //Find avg palm centers for the last few frames to stabilize its centers, also find the avg radius
                                palm_centers.add(soln_circle);
                                if (palm_centers.size() > 10)
                                    palm_centers.erase(palm_centers.begin());

                                Point palm_center;
                                double radius = 0;
                                for (int i = 0; i < palm_centers.size(); i++) {
                                    palm_center += palm_centers[i].first;
                                    radius += palm_centers[i].second;
                                }
                                palm_center.x /= palm_centers.size();
                                palm_center.y /= palm_centers.size();
                                radius /= palm_centers.size();

                                //Draw the palm center and the palm circle
                                //The size of the palm gives the depth of the hand
                                circle(frame, palm_center, 5, Scalar(144, 144, 255), 3);
                                circle(frame, palm_center, radius, Scalar(144, 144, 255), 2);

                                //Detect fingers by finding points that form an almost isosceles triangle with certain thesholds
                                int no_of_fingers = 0;
                                for (int j = 0; j < defects.size(); j++) {
                                    int startidx = defects[j][0];
                                    Point ptStart (tcontours[0][startidx]);
                                    int endidx = defects[j][1];
                                    Point ptEnd (tcontours[0][endidx]);
                                    int faridx = defects[j][2];
                                    Point ptFar (tcontours[0][faridx]);
                                    //X o--------------------------o Y
                                    double Xdist = sqrt(dist(palm_center, ptFar));
                                    double Ydist = sqrt(dist(palm_center, ptStart));
                                    double length = sqrt(dist(ptFar, ptStart));

                                    double retLength = sqrt(dist(ptEnd, ptFar));
                                    //Play with these thresholds to improve performance
                                    if (length <= 3 * radius && Ydist >= 0.4 * radius && length >= 10 && retLength >= 10 && max(length, retLength) / min(length, retLength) >= 0.8)
                                        if (min(Xdist, Ydist) / max(Xdist, Ydist) <= 0.8) {
                                            if ((Xdist >= 0.1 * radius && Xdist <= 1.3 * radius && Xdist < Ydist) || (Ydist >= 0.1 * radius && Ydist <= 1.3 * radius && Xdist > Ydist))
                                                line(frame, ptEnd, ptFar, Scalar(0, 255, 0), 1), no_of_fingers++;
                                        }


                                }

                                no_of_fingers = min(5, no_of_fingers);
                                cout << "NO OF FINGERS: " << no_of_fingers << endl;
                                mouseTo(palm_center.x, palm_center.y);//Move the cursor corresponding to the palm
                                if (no_of_fingers < 4)//If no of fingers is <4 , click , else release
                                    mouseClick();
                                else
                                    mouseRelease();

                            }
                        }

                    }
                if (backgroundFrame > 0)
                    putText(frame, "Recording Background", cvPoint(30, 30), FONT_HERSHEY_COMPLEX_SMALL, 0.8, cvScalar(200, 200, 250), 1, CV_AA);
                imshow("Frame", frame);
                imshow("Background", back);
                if (waitKey(10) >= 0) break;
            }
        }
    }
}
