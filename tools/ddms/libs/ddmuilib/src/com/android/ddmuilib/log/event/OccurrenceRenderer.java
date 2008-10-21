/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ddmuilib.log.event;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * Custom renderer to render event occurrence. This rendered ignores the y value, and simply
 * draws a line from min to max at the time of the item.
 */
public class OccurrenceRenderer extends XYLineAndShapeRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public void drawItem(Graphics2D g2, 
                         XYItemRendererState state,
                         Rectangle2D dataArea,
                         PlotRenderingInfo info,
                         XYPlot plot, 
                         ValueAxis domainAxis, 
                         ValueAxis rangeAxis,
                         XYDataset dataset, 
                         int series, 
                         int item,
                         CrosshairState crosshairState, 
                         int pass) {
        TimeSeriesCollection timeDataSet = (TimeSeriesCollection)dataset;
        
        // get the x value for the series/item.
        double x = timeDataSet.getX(series, item).doubleValue();

        // get the min/max of the range axis
        double yMin = rangeAxis.getLowerBound();
        double yMax = rangeAxis.getUpperBound();

        RectangleEdge domainEdge = plot.getDomainAxisEdge();
        RectangleEdge rangeEdge = plot.getRangeAxisEdge();

        // convert the coordinates to java2d.
        double x2D = domainAxis.valueToJava2D(x, dataArea, domainEdge);
        double yMin2D = rangeAxis.valueToJava2D(yMin, dataArea, rangeEdge);
        double yMax2D = rangeAxis.valueToJava2D(yMax, dataArea, rangeEdge);

        // get the paint information for the series/item
        Paint p = getItemPaint(series, item);
        Stroke s = getItemStroke(series, item);
        
        Line2D line = null;
        PlotOrientation orientation = plot.getOrientation();
        if (orientation == PlotOrientation.HORIZONTAL) {
            line = new Line2D.Double(yMin2D, x2D, yMax2D, x2D);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            line = new Line2D.Double(x2D, yMin2D, x2D, yMax2D);
        }
        g2.setPaint(p);
        g2.setStroke(s);
        g2.draw(line);
    }
}
