/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a1_music_graph;

import java.awt.*;
import java.awt.color.*;

/**
 *
 * @author kenschiller
 */
public abstract class PhaseGraphics {
    public static Color relativelyTransparent(Color original, float alpha) {
        ColorSpace srbg = ICC_ColorSpace.getInstance(ColorSpace.CS_sRGB);
        double originalAlpha = 1.0 * original.getAlpha() / 255;
        alpha *= originalAlpha;
        return new Color(srbg, original.getColorComponents(null), alpha);
    }
    public static float [] getHSB(Color color) {
        float [] rgb = new float [4];
        float [] hsb = new float [3];
        color.getComponents(rgb);
        int r = (int) (rgb[0] * 256);
        int g = (int) (rgb[1] * 256);
        int b = (int) (rgb[2] * 256);
        Color.RGBtoHSB(r, g, b, hsb);
        return hsb;
    }
    public static Color saturationAdjust(Color original, float saturation) {
        float [] hsb = getHSB(original);
        hsb[1] = saturation;
        if(hsb[1] < 0.0f) hsb[1] = 0.0f;
        else if(hsb[1] > 1.0f) hsb[1] = 1.0f;
        int newRGB = Color.HSBtoRGB(hsb[0], hsb[1], 1.0f);
        return new Color(newRGB);
    }
    public static Color brightnessAdjust(Color original, float brightness) {
        float [] hsb = getHSB(original);
        hsb[2] = brightness;
        if(hsb[2] < 0.0f) hsb[2] = 0.0f;
        else if(hsb[2] > 1.0f) hsb[2] = 1.0f;
        int newRGB = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return new Color(newRGB);
    }
    public static void fillCircle(Graphics g, int x, int y, int radius) {
        int x1 = x - radius;
        int y1 = y - radius;
        g.fillOval(x1, y1, radius * 2 + 1, radius * 2 + 1);
    }
    public static float pulseValue(int t, int period) {
        double value = Math.sin(2.0 * t * Math.PI / period) / 2 + 0.5;
        //double x = (double) period / 2;
        //double value = Math.abs(t - x) / x;
        return (float) value;
    }
    //
    public static Color makePale(Color original, float amount) {
        return saturationAdjust(brightnessAdjust(original, 1.0f), amount);
    }
}
