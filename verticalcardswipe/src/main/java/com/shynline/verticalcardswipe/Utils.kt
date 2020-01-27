package com.shynline.verticalcardswipe

import android.content.Context

object Utils {


    fun toPx(context: Context, dp: Float): Float {
        val scale = context.resources.displayMetrics.density
        return dp * scale + 0.5f
    }


    fun getRadian(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        val width = x2 - x1
        val height = y1 - y2
        return Math.atan((Math.abs(height) / Math.abs(width)).toDouble())
    }

    //    public static Point getTargetPoint(float x1, float y1, float x2, float y2) {
    //        float radius = 2000f;
    //        double radian = Util.getRadian(x1, y1, x2, y2);
    //
    //        Quadrant quadrant = getQuadrant(x1, y1, x2, y2);
    //        if (quadrant == Quadrant.TopLeft) {
    //            double degree = Math.toDegrees(radian);
    //            degree = 180 - degree;
    //            radian = Math.toRadians(degree);
    //        } else if (quadrant == Quadrant.BottomLeft) {
    //            double degree = Math.toDegrees(radian);
    //            degree = 180 + degree;
    //            radian = Math.toRadians(degree);
    //        } else if (quadrant == Quadrant.BottomRight) {
    //            double degree = Math.toDegrees(radian);
    //            degree = 360 - degree;
    //            radian = Math.toRadians(degree);
    //        } else {
    //            double degree = Math.toDegrees(radian);
    //            radian = Math.toRadians(degree);
    //        }
    //
    //        double x = radius * Math.cos(radian);
    //        double y = radius * Math.sin(radian);
    //
    //        return new Point((int) x, (int) y);
    //    }
    //
    //    public static Quadrant getQuadrant(float x1, float y1, float x2, float y2) {
    //        if (x2 > x1) { // Right
    //            if (y2 > y1) { // Bottom
    //                return Quadrant.BottomRight;
    //            } else { // Top
    //                return Quadrant.TopRight;
    //            }
    //        } else { // Left
    //            if (y2 > y1) { // Bottom
    //                return Quadrant.BottomLeft;
    //            } else { // Top
    //                return Quadrant.TopLeft;
    //            }
    //        }
    //    }

}
