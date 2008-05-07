/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2007, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * ------------------------------
 * GroupedStackedBarRenderer.java
 * ------------------------------
 * (C) Copyright 2004-2007, by Object Refinery Limited and Contributors.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   -;
 *
 * Changes
 * -------
 * 29-Apr-2004 : Version 1 (DG);
 * 08-Jul-2004 : Added equals() method (DG);
 * 05-Nov-2004 : Modified drawItem() signature (DG);
 * 07-Jan-2005 : Renamed getRangeExtent() --> findRangeBounds (DG);
 * 20-Apr-2005 : Renamed CategoryLabelGenerator
 *               --> CategoryItemLabelGenerator (DG);
 * 22-Sep-2005 : Renamed getMaxBarWidth() --> getMaximumBarWidth() (DG);
 * 20-Dec-2007 : Fix for bug 1848961 (DG);
 *
 */

package org.jfree.chart.renderer.category;

import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.KeyToGroupMap;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.PublicCloneable;

/**
 * A renderer that draws stacked bars within groups.  This will probably be
 * merged with the {@link StackedBarRenderer} class at some point.
 */
public class GroupedStackedBarRenderer extends StackedBarRenderer
        implements Cloneable, PublicCloneable, Serializable {

    /** For serialization. */
    private static final long serialVersionUID = -2725921399005922939L;

    /** A map used to assign each series to a group. */
    private KeyToGroupMap seriesToGroupMap;

    /**
     * Creates a new renderer.
     */
    public GroupedStackedBarRenderer() {
        super();
        this.seriesToGroupMap = new KeyToGroupMap();
    }

    /**
     * Updates the map used to assign each series to a group, and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param map  the map (<code>null</code> not permitted).
     */
    public void setSeriesToGroupMap(KeyToGroupMap map) {
        if (map == null) {
            throw new IllegalArgumentException("Null 'map' argument.");
        }
        this.seriesToGroupMap = map;
        fireChangeEvent();
    }

    /**
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     *
     * @param dataset  the dataset (<code>null</code> permitted).
     *
     * @return The range (or <code>null</code> if the dataset is
     *         <code>null</code> or empty).
     */
    public Range findRangeBounds(CategoryDataset dataset) {
        Range r = DatasetUtilities.findStackedRangeBounds(
                dataset, this.seriesToGroupMap);
        return r;
    }

    /**
     * Calculates the bar width and stores it in the renderer state.  We
     * override the method in the base class to take account of the
     * series-to-group mapping.
     *
     * @param plot  the plot.
     * @param dataArea  the data area.
     * @param rendererIndex  the renderer index.
     * @param state  the renderer state.
     */
    protected void calculateBarWidth(CategoryPlot plot,
                                     Rectangle2D dataArea,
                                     int rendererIndex,
                                     CategoryItemRendererState state) {

        // calculate the bar width
        CategoryAxis xAxis = plot.getDomainAxisForDataset(rendererIndex);
        CategoryDataset data = plot.getDataset(rendererIndex);
        if (data != null) {
            PlotOrientation orientation = plot.getOrientation();
            double space = 0.0;
            if (orientation == PlotOrientation.HORIZONTAL) {
                space = dataArea.getHeight();
            }
            else if (orientation == PlotOrientation.VERTICAL) {
                space = dataArea.getWidth();
            }
            double maxWidth = space * getMaximumBarWidth();
            int groups = this.seriesToGroupMap.getGroupCount();
            int categories = data.getColumnCount();
            int columns = groups * categories;
            double categoryMargin = 0.0;
            double itemMargin = 0.0;
            if (categories > 1) {
                categoryMargin = xAxis.getCategoryMargin();
            }
            if (groups > 1) {
                itemMargin = getItemMargin();
            }

            double used = space * (1 - xAxis.getLowerMargin()
                                     - xAxis.getUpperMargin()
                                     - categoryMargin - itemMargin);
            if (columns > 0) {
                state.setBarWidth(Math.min(used / columns, maxWidth));
            }
            else {
                state.setBarWidth(Math.min(used, maxWidth));
            }
        }

    }

    /**
     * Calculates the coordinate of the first "side" of a bar.  This will be
     * the minimum x-coordinate for a vertical bar, and the minimum
     * y-coordinate for a horizontal bar.
     *
     * @param plot  the plot.
     * @param orientation  the plot orientation.
     * @param dataArea  the data area.
     * @param domainAxis  the domain axis.
     * @param state  the renderer state (has the bar width precalculated).
     * @param row  the row index.
     * @param column  the column index.
     *
     * @return The coordinate.
     */
    protected double calculateBarW0(CategoryPlot plot,
                                    PlotOrientation orientation,
                                    Rectangle2D dataArea,
                                    CategoryAxis domainAxis,
                                    CategoryItemRendererState state,
                                    int row,
                                    int column) {
        // calculate bar width...
        double space = 0.0;
        if (orientation == PlotOrientation.HORIZONTAL) {
            space = dataArea.getHeight();
        }
        else {
            space = dataArea.getWidth();
        }
        double barW0 = domainAxis.getCategoryStart(column, getColumnCount(),
        		dataArea, plot.getDomainAxisEdge());
        int groupCount = this.seriesToGroupMap.getGroupCount();
        int groupIndex = this.seriesToGroupMap.getGroupIndex(
        		this.seriesToGroupMap.getGroup(plot.getDataset(
        				plot.getIndexOf(this)).getRowKey(row)));
        int categoryCount = getColumnCount();
        if (groupCount > 1) {
            double groupGap = space * getItemMargin()
                              / (categoryCount * (groupCount - 1));
            double groupW = calculateSeriesWidth(space, domainAxis,
            		categoryCount, groupCount);
            barW0 = barW0 + groupIndex * (groupW + groupGap)
                          + (groupW / 2.0) - (state.getBarWidth() / 2.0);
        }
        else {
            barW0 = domainAxis.getCategoryMiddle(column, getColumnCount(),
            		dataArea, plot.getDomainAxisEdge())
            		- state.getBarWidth() / 2.0;
        }
        return barW0;
    }

    /**
     * Draws a stacked bar for a specific item.
     *
     * @param g2  the graphics device.
     * @param state  the renderer state.
     * @param dataArea  the plot area.
     * @param plot  the plot.
     * @param domainAxis  the domain (category) axis.
     * @param rangeAxis  the range (value) axis.
     * @param dataset  the data.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param pass  the pass index.
     */
    public void drawItem(Graphics2D g2,
                         CategoryItemRendererState state,
                         Rectangle2D dataArea,
                         CategoryPlot plot,
                         CategoryAxis domainAxis,
                         ValueAxis rangeAxis,
                         CategoryDataset dataset,
                         int row,
                         int column,
                         int pass) {

        // nothing is drawn for null values...
        Number dataValue = dataset.getValue(row, column);
        if (dataValue == null) {
            return;
        }

        double value = dataValue.doubleValue();
        Comparable group = this.seriesToGroupMap.getGroup(
        		dataset.getRowKey(row));
        PlotOrientation orientation = plot.getOrientation();
        double barW0 = calculateBarW0(plot, orientation, dataArea, domainAxis,
                state, row, column);

        double positiveBase = 0.0;
        double negativeBase = 0.0;

        for (int i = 0; i < row; i++) {
            if (group.equals(this.seriesToGroupMap.getGroup(
                    dataset.getRowKey(i)))) {
                Number v = dataset.getValue(i, column);
                if (v != null) {
                    double d = v.doubleValue();
                    if (d > 0) {
                        positiveBase = positiveBase + d;
                    }
                    else {
                        negativeBase = negativeBase + d;
                    }
                }
            }
        }

        double translatedBase;
        double translatedValue;
        RectangleEdge location = plot.getRangeAxisEdge();
        if (value > 0.0) {
            translatedBase = rangeAxis.valueToJava2D(positiveBase, dataArea,
                    location);
            translatedValue = rangeAxis.valueToJava2D(positiveBase + value,
                    dataArea, location);
        }
        else {
            translatedBase = rangeAxis.valueToJava2D(negativeBase, dataArea,
                    location);
            translatedValue = rangeAxis.valueToJava2D(negativeBase + value,
                    dataArea, location);
        }
        double barL0 = Math.min(translatedBase, translatedValue);
        double barLength = Math.max(Math.abs(translatedValue - translatedBase),
                getMinimumBarLength());

        Rectangle2D bar = null;
        if (orientation == PlotOrientation.HORIZONTAL) {
            bar = new Rectangle2D.Double(barL0, barW0, barLength,
                    state.getBarWidth());
        }
        else {
            bar = new Rectangle2D.Double(barW0, barL0, state.getBarWidth(),
                    barLength);
        }
        Paint itemPaint = getItemPaint(row, column);
        if (getGradientPaintTransformer() != null
                && itemPaint instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint) itemPaint;
            itemPaint = getGradientPaintTransformer().transform(gp, bar);
        }
        g2.setPaint(itemPaint);
        g2.fill(bar);
        if (isDrawBarOutline()
                && state.getBarWidth() > BAR_OUTLINE_WIDTH_THRESHOLD) {
            g2.setStroke(getItemStroke(row, column));
            g2.setPaint(getItemOutlinePaint(row, column));
            g2.draw(bar);
        }

        CategoryItemLabelGenerator generator = getItemLabelGenerator(row,
                column);
        if (generator != null && isItemLabelVisible(row, column)) {
            drawItemLabel(g2, dataset, row, column, plot, generator, bar,
                    (value < 0.0));
        }

        // collect entity and tool tip information...
        if (state.getInfo() != null) {
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
            	addItemEntity(entities, dataset, row, column, bar);
            }
        }

    }

    /**
     * Tests this renderer for equality with an arbitrary object.
     *
     * @param obj  the object (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof GroupedStackedBarRenderer)) {
        	return false;
        }
        GroupedStackedBarRenderer that = (GroupedStackedBarRenderer) obj;
        if (!this.seriesToGroupMap.equals(that.seriesToGroupMap)) {
            return false;
        }
        return super.equals(obj);
    }

}
