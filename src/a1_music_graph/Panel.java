/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a1_music_graph;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author kenschiller
 */
public class Panel extends JPanel implements Runnable, MouseListener, MouseMotionListener, KeyListener {
    private final int s_ = 2;
    //private java.util.Timer refreshTimer_;
    //private RegularProcess refreshProcess_;
    private boolean paused_;
    public static final int width_ = 12160 * 2;
    public static final int height_ = 840 * 2;
    public static final int toolbarHeight_ = 20;
    public static final int FPS = 30;
    //
    private int tracks_;
    private int measures_;
    private Color [] colors_; //length = tracks_;
    private double [] originalVolume_;
    private double [] displayVolume_;
    private double displayVolumeMax_;
    private String [] measureTimeStrings_;
    private float [] measureTimes_;
    private float currentTime_;
    private float totalTime_;
    private float timeScale_;
    private int [] measureCenters_;
    private int [] [] [] notes_; //length = tracks_, measures_ * 2, (# of notes)
    private final int horizontalBorder_ = 12 * s_; //horizontal border
    private final int verticalBorder_ = 144 * s_; //top and bottom combined
    private int leftMargin_ = 160 * s_; //- width_ * 3 / 4
    //extra details and titles go in the 60 pixels at top and 60 at bottom
    private int x_ = leftMargin_ + horizontalBorder_;
    private final int y_ = (height_ /*- toolbarHeight_*/) / 2 - 1; // COMMENT OUT TOOLBAR HEIGHT WHEN PRINTING TO FILE
    //private double measureLength_;
    private double volumeScalePerTrack_;
    private int weirdWholeNoteVariable_ = 0;
    private final boolean soloistInCenter_ = true;
    private final Color backgroundColor_ = Color.white; //PhaseGraphics.saturationAdjust(Color.cyan, 0.11f);
    public Panel() {
        /*paused_ = true;
        refreshTimer_ = new java.util.Timer();
        refreshProcess_ = new RegularProcess(this);
        refreshTimer_.schedule(refreshProcess_, 0, 1000 / FPS);
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        setFocusable(true);*/
        //initialize member variables
        tracks_ = 5;
        measures_ = 63;
        originalVolume_ = new double [measures_];
        displayVolume_ = new double [measures_];
        measureTimeStrings_ = new String [measures_];
        measureTimes_ = new float [measures_];
        currentTime_ = toSeconds(0, 2, 1);
        totalTime_ = toSeconds(3, 27, 28);
        timeScale_ = 1.0f * (width_ - x_ - horizontalBorder_ * 2) / totalTime_;
        measureCenters_ = new int [measures_];
        colors_ = new Color [tracks_];

        /*colors_[0] = new Color(0, 255, 255);
        colors_[1] = new Color(55, 200, 255);
        colors_[2] = new Color(55, 255, 200);
        colors_[3] = new Color(0, 127, 255);
        colors_[4] = new Color(0, 127, 200);*/

        //automatically moved around if soloistInCenter
        colors_[0] = new Color(0, 127, 255);
        colors_[1] = new Color(55, 200, 255);
        colors_[2] = new Color(0, 255, 255);
        colors_[3] = new Color(55, 255, 200);
        colors_[4] = new Color(0, 80, 180);

        notes_ = new int [tracks_][measures_ * 2] [];
        //measureLength_ = 1.0 * (width_ - x_ * 2) / measures_;
        loadData(); //volume_, notes_
        volumeScalePerTrack_ = 1.0 * (height_ - verticalBorder_) / displayVolumeMax_ / (tracks_ - 1);
        //
        paused_ = true;
    }
    public void run() {
        if(paused_) {
            repaint();
            return;
        }
        repaint();
    }
    private float toSeconds(int min, int sec, int hun) {
        return min * 60 + sec + hun * 0.01f;
    }
    private int [] interpretNotes(String s) {
        if(s.equals("X") || s.equals("x")) {
            s = "63333336666";
        }
        //
        int r;
        int [] notes;
        if(s.length() == 1) {
            switch(s.charAt(0)) {
                case '2': // a half note
                    r = 1;
                    break;
                case 'A': // all 8ths
                case 'a':
                    r = 4;
                    break;
                case 'B': // all 16ths
                case 'b':
                    r = 8;
                    break;
                case 'C': // all 32nds
                case 'c':
                    r = 16;
                    break;
                case 'W': // whole note (1st half)
                case 'w':
                    r = -1;
                    break;
                case 'S': // whole note sustain
                case 's':
                    r = -2;
                    break;
                case '0':
                default:
                    r = 0;
                    break;
            }
            if(r == 0) {
                notes = null;
            }
            else if(r < 0) {
                notes = new int [1];
                notes[0] = r;
            }
            else {
                notes = new int [r];
                for(int i = 0; i < r; i++) {
                    notes[i] = r;
                }
            }
        }
        else {
            notes = new int [s.length()];
            for(int i = 0; i < notes.length; i++) {
                switch(s.charAt(i)) {
                    case 'R':
                    case 'r':
                        notes[i] = 0;
                        break;
                    case '1':
                    case '4':
                        notes[i] = 2; // quarter
                        break;
                    case '8':
                        notes[i] = 4; // 8th
                        break;
                    case '6':
                        notes[i] = 8; // 16th
                        break;
                    case '3':
                        notes[i] = 16; // 32nd
                        break;
                }
            }
        }
        return notes;
    }
    private void loadData() {
        try {
            double minVolume = 0.0;
            double maxVolume = -100.0;
            Scanner sc = new Scanner(new File("volume.txt"));
            for(int j = 0; j < measures_; j++) {
                double volume = Double.parseDouble(sc.next());
                String time = sc.next().substring(1, 5);
                //System.out.println(volume + "[" + time + "]");
                originalVolume_[j] = volume;
                //measureTimeStrings_[j] = time;
                measureTimes_[j] = toSeconds(0, Integer.parseInt(time.substring(0, 1)), Integer.parseInt(time.substring(2, 4)));
                if(volume < minVolume) {
                    minVolume = volume;
                }
                if(volume > maxVolume) {
                    maxVolume = volume;
                }
            }
            for(int j = 0; j < measures_; j++) {
                double range = maxVolume - minVolume;
                displayVolumeMax_ = range * 1.1;
                displayVolume_[j] = originalVolume_[j] - minVolume + range * 0.1;
                //System.out.println(displayVolume_[j]);
            }
            //System.out.println("\n" + "max: " + displayVolumeMax_);
            sc.close();
            sc = new Scanner(new File("anthony.txt"));
            for(int i = 0; i < tracks_; i++) {
                for(int j = 0; j < measures_ * 2; j++) {
                    String noteString = sc.next();
                    notes_[i][j] = interpretNotes(noteString);
                }
            }
            sc.close();
        }
        catch(IOException e) {}
        if(soloistInCenter_) {
            int [] [] temp = notes_[2];
            notes_[2] = notes_[0];
            notes_[0] = temp;
            temp = notes_[3];
            notes_[3] = notes_[0];
            notes_[0] = temp;
            Color tempC = colors_[2];
            colors_[2] = colors_[0];
            colors_[0] = tempC;
            tempC = colors_[3];
            colors_[3] = colors_[0];
            colors_[0] = tempC;
        }
    }
    public void setLeftMargin(int a) {
        leftMargin_ = 160 * s_ - width_ * a / 4;
        x_ = leftMargin_ + horizontalBorder_;
    }
    public void writeImage(String fileName) {
        BufferedImage bi = new BufferedImage(width_ / 4, height_, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        paintComponent(g2);
        try {
            ImageIO.write(bi, "PNG", new File(fileName));
        }
        catch (IOException e) {}
    }
    public void paintComponent(Graphics g) {
        g.setColor(backgroundColor_);
        g.fillRect(0, 0, width_, height_);
        float currentX = x_ + timeScale_ * (currentTime_ - measureTimes_[0] / 2);
        // intro
        double yStep = volumeScalePerTrack_ * displayVolume_[0];
        int xB = (int) (x_ + timeScale_ * currentTime_);
        for(int i = -2; i < tracks_ - 2; i++) {
            g.setColor(PhaseGraphics.makePale(colors_[i + 2], 0.3f));
            g.drawLine(x_, (int) (y_ + yStep * i), xB, (int) (y_ + yStep * i));
        }
        //
        int x2 = 0;
        double y2 = 0;
        double y2Step = 0.0;
        float halfMeasure2 = 0.0f;
        for(int j = -1; j < measures_; j++) {
            float halfMeasure1 = timeScale_ * measureTimes_[j == -1? 0 : j] / 2;
            halfMeasure2 = timeScale_ * measureTimes_[j == measures_ - 1? j : j + 1] / 2;
            //
            int x1 = (int) currentX;
            int xMid = (int) (currentX + halfMeasure1);
            x2 = (int) (currentX + halfMeasure1 + halfMeasure2);
            double y1Step = volumeScalePerTrack_ * displayVolume_[j == -1? 0 : j];
            y2Step = volumeScalePerTrack_ * displayVolume_[j == measures_ - 1? j : j + 1];
            double yMidStep = (y1Step + y2Step) / 2;
            double y1 = y_ - 2 * y1Step; // -2 if there are 5 tracks
            double yMid = y_ - 2 * yMidStep;
            y2 = y_ - 2 * y2Step;
            g.setColor(PhaseGraphics.relativelyTransparent(Color.gray, 0.5f));
            g.drawLine(xMid, (int) yMid, xMid, (int) (yMid + ((tracks_ - 1) * yMidStep)));
            for(int i = 0; i < tracks_; i++) {
                if(j != -1) {
                    g.setColor(PhaseGraphics.makePale(colors_[i], 0.3f));
                    drawThickLine(g, x1, (int) y1, xMid, (int) yMid, 0);
                    if(notes_[i][j * 2 + 1] != null) {
                        musicLine(g, x1, (int) y1, xMid, (int) yMid, i, j * 2 + 1, halfMeasure1);
                    }
                }
                if(j != measures_ - 1) {
                    g.setColor(PhaseGraphics.makePale(colors_[i], 0.3f));
                    drawThickLine(g, xMid, (int) yMid, x2, (int) y2, 0);
                    if(notes_[i][j * 2 + 2] != null) {
                        musicLine(g, xMid, (int) yMid, x2, (int) y2, i, j * 2 + 2, halfMeasure2);
                    }
                }
                //
                if(j != -1) {
                    measureCenters_[j] = x1;
                }
                y1 += y1Step;
                yMid += yMidStep;
                y2 += y2Step;
                //
            }
            g.setColor(Color.gray);
            currentX = x2;
        }
        // coda
        yStep = volumeScalePerTrack_ * displayVolume_[measures_ - 1];
        xB = (int) (x_ + timeScale_ * totalTime_);
        for(int i = -2; i < tracks_ - 2; i++) {
            g.setColor(PhaseGraphics.makePale(colors_[i + 2], 0.3f));
            g.drawLine((int) (currentX - halfMeasure2), (int) (y_ + yStep * i), (int) (xB - halfMeasure2), (int) (y_ + yStep * i));
        }
        //
        g.drawLine((int) x2, (int) y2, (int) x2, (int) (y2 + ((tracks_ - 1) * y2Step)));
        drawTitles(g);
    }
    public void drawTitles(Graphics g) {
        int fontSize = 10 * s_;
        g.setColor(new Color(150, 150, 255));
        g.setFont(new Font("Helvetica", Font.PLAIN, fontSize));
        int y = (int) (height_ / 2 - fontSize * 2.8);
        rightAlignedText(g, "Violino Principale", leftMargin_, y);
        y += fontSize * 1.5;
        rightAlignedText(g, "Violino 1", leftMargin_, y);
        y += fontSize * 1.5;
        rightAlignedText(g, "Violino 2", leftMargin_, y);
        y += fontSize * 1.5;
        rightAlignedText(g, "Viola", leftMargin_, y);
        y += fontSize * 1.5;
        rightAlignedText(g, "Violoncello; Basso continuo", leftMargin_, y);
        //
        double x = x_;
        for(int i = 0; i < totalTime_ / 60; i++) {
            g.drawString("" + i + ":00", (int) x, height_ - 20 * s_);
            g.drawLine((int) x, height_ - 40 * s_, (int) x, height_ - 30 * s_);
            for(int j = 0; j < 6; j++) {
                for(int k = 0; k < 10; k++) {
                    x += timeScale_;
                    if(k < 9) {
                        g.drawRect((int) x - 1 * s_, height_ - 41 * s_, 2 * s_, 2 * s_);
                    }
                }
                if(j < 5) {
                    g.drawString("" + i + ":" + (j + 1) + "0", (int) x, height_ - 20 * s_);
                    g.drawLine((int) x, height_ - 40 * s_, (int) x, height_ - 30 * s_);
                }
            }
        }
        x = x_ + totalTime_ * timeScale_;
        //g.drawString("3:27.28", (int) x, height_ - 20);
        //g.drawLine((int) x, height_ - 40, (int) x, height_ - 30);
        //
        //g.setColor(Color.blue);
        for(int j = 0; j < measures_; j++) {
            String s = "" + originalVolume_[j] + "dB";
            int offset = g.getFontMetrics().stringWidth(s);
            g.drawString(s, measureCenters_[j] - offset / 2, 35 * s_);
        }
    }
    public void rightAlignedText(Graphics g, String s, int xr, int y) {
        int width = g.getFontMetrics().stringWidth(s);
        g.drawString(s, xr - width, y);
    }
    public void drawThickLine(Graphics g, int x1, int y1, int x2, int y2, int thickness) {
        g.drawLine(x1, y1, x2, y2);
        for(int i = 1; i <= thickness; i++) {
            g.drawLine(x1, y1 - i, x2, y2 - i);
            g.drawLine(x1, y1 + i, x2, y2 + i);
        }
    }
    public void musicLine(Graphics g, int x1, int y1, int x2, int y2, int i, int j2, float halfMeasure) {
        int jagLength = i == 2 && soloistInCenter_? 16 : 8;
        double xEnd = x1;
        double yEnd = y1;
        double slope = 1.0 * (y2 - y1) / (x2 - x1);
        for(int k = 0; k < notes_[i][j2].length; k++) {
            double xI = xEnd;
            double yI = yEnd;
            if(notes_[i][j2][k] != 0) {
                /*
                jagLength = notes_[i][j2][k];
                System.out.println(jagLength);
                */
                double xChange = halfMeasure * 1.0 / notes_[i][j2][k];
                if(notes_[i][j2][0] < 0) {
                    xChange = halfMeasure * 2;
                }
                xEnd += xChange;
                yEnd += slope * xChange;
                //
                int x1Int = (int) xI;
                int y1Int = (int) yI;
                int x2Int = (int) (xI + xChange / 2);
                int y2Int = (int) (yI + slope * xChange / 2);
                int x3Int = (int) xEnd;
                int y3Int = (int) yEnd;
                //
                double normalAngle = -Math.abs(Math.atan(-1.0 / slope));
                int sawDX = (int) (jagLength * Math.cos(normalAngle));
                if(slope < 0) {
                    sawDX *= -1;
                }
                int sawDY = (int) (jagLength * Math.sin(normalAngle));
                int sawTopX = x2Int + sawDX;
                int sawTopY = y2Int + sawDY;
                int sawBottomX = x2Int - sawDX;
                int sawBottomY = y2Int - sawDY;
                //g.setColor(PhaseGraphics.relativelyTransparent(colors_[i], 0.5f));
                //drawThickLine(g, x1Int, y1Int, x3Int, y3Int, 0);
                g.setColor(colors_[i]);
                if(notes_[i][j2][0] == -2) {
                    drawThickLine(g, weirdWholeNoteVariable_, sawBottomY, sawTopX, y1Int, 1);
                }
                else {
                    drawThickLine(g, x1Int, y1Int, sawTopX, sawTopY, 1);
                    drawThickLine(g, sawTopX, sawTopY, sawBottomX, sawBottomY, 1);
                    if(notes_[i][j2][0] == -1) {
                        weirdWholeNoteVariable_ = sawBottomX;
                    }
                }
                if(notes_[i][j2][0] >= 0) {
                    drawThickLine(g, sawBottomX, sawBottomY, x3Int, y3Int, 1);
                }
            }
        }
    }
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {
        if(paused_) return;
    }
    public void mouseReleased(MouseEvent e) {
        if(paused_) return;
    }
    public void mouseDragged(MouseEvent e) {}
    public void keyPressed(KeyEvent e) {
        key(e.getKeyCode(), true);
    }
    public void keyReleased(KeyEvent e) {
        key(e.getKeyCode(), false);
    }
    public void keyTyped(KeyEvent e) {}
    public void key(int keyCode, boolean keyDown) {
        if(paused_) {
            switch(keyCode) {
                case KeyEvent.VK_P:
                    if(keyDown) {
                        paused_ = false;
                    }
                    return;
            }
            return;
        }
        switch(keyCode) {
            case KeyEvent.VK_P:
                if(keyDown) {
                    paused_ = true;
                }
                break;
            case KeyEvent.VK_RIGHT:
                break;
            case KeyEvent.VK_UP:
                break;
            case KeyEvent.VK_DOWN:
                break;
            case KeyEvent.VK_SPACE:
                break;
            case KeyEvent.VK_Q:
                break;
            case KeyEvent.VK_W:
                break;
            case KeyEvent.VK_E:
                break;
            case KeyEvent.VK_Z:
                break;
            default:
                break;
        }
    }
}
