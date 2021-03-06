package org.geogebra.web.full.gui.view.spreadsheet;

import org.geogebra.common.awt.GPoint;
import org.geogebra.common.awt.GRectangle;
import org.geogebra.web.html5.util.Dom;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.client.ui.AbstractNativeScrollbar;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ScrollPanel;

import elemental2.dom.Event;
import elemental2.dom.WheelEvent;

public class TableScroller extends ScrollPanel implements ScrollHandler {

	private MyTableW table;
	private Grid cellTable;
	private SpreadsheetRowHeaderW rowHeader;
	private SpreadsheetColumnHeaderW columnHeader;
	GRectangle contentRect;
	boolean doAdjustScroll = true;

	/**
	 * @param table
	 *            table
	 * @param rowHeader
	 *            row header
	 * @param columnHeader
	 *            column header
	 */
	public TableScroller(MyTableW table, SpreadsheetRowHeaderW rowHeader,
			SpreadsheetColumnHeaderW columnHeader) {
		super(table.getGridPanel());
		this.table = table;
		this.cellTable = table.getGrid();
		this.rowHeader = rowHeader;
		this.columnHeader = columnHeader;

		addScrollHandler(this);
		Dom.addEventListener(getElement(), "wheel", this::onMouseWheel);
	}

	private void onMouseWheel(Event event) {
		event.preventDefault();
		int delta = (int) Math.signum(((WheelEvent) event).deltaY);
		if (((WheelEvent) event).shiftKey) {
			adjustScroll(delta, 0);
		} else {
			adjustScroll(0, delta);
		}
	}

	/**
	 * Used by the scrollRectToVisible method to determine the proper direction
	 * and amount to move by. The integer variables are named width, but this
	 * method is applicable to height also. The code assumes that
	 * parentWidth/childWidth are positive and childAt can be negative.
	 */
	private static int positionAdjustment(int parentWidth, int childWidth,
			int childAt) {

		// App.debug("parent width = " + parentWidth);
		// App.debug("child width = " + childWidth);
		// App.debug("child at = " + childAt);
		// +-----+
		// | --- | No Change
		// +-----+
		if (childAt >= 0 && childWidth + childAt <= parentWidth) {
			return 0;
		}

		// +-----+
		// --------- No Change
		// +-----+
		if (childAt <= 0 && childWidth + childAt >= parentWidth) {
			return 0;
		}

		// +-----+ +-----+
		// | ---- -> | ----|
		// +-----+ +-----+
		if (childAt > 0 && childWidth <= parentWidth) {
			return -childAt + parentWidth - childWidth;
		}

		// +-----+ +-----+
		// | -------- -> |--------
		// +-----+ +-----+
		if (childAt >= 0 && childWidth >= parentWidth) {
			return -childAt;
		}

		// +-----+ +-----+
		// ---- | -> |---- |
		// +-----+ +-----+
		if (childAt <= 0 && childWidth <= parentWidth) {
			return -childAt;
		}

		// +-----+ +-----+
		// -------- | -> --------|
		// +-----+ +-----+
		if (childAt < 0 && childWidth >= parentWidth) {
			return -childAt + parentWidth - childWidth;
		}

		return 0;
	}

	/**
	 * Scroll to make content visible.
	 * 
	 * @param contentRect1
	 *            content rectangle
	 */
	public void scrollRectToVisible(GRectangle contentRect1) {
		this.contentRect = contentRect1;
		Scheduler.get().scheduleDeferred(this::scrollRectToVisibleCommand);
	}

	/**
	 * Scrolls the view so that <code>Rectangle</code> within the view becomes
	 * visible.
	 * <p>
	 * This attempts to validate the view before scrolling if the view is
	 * currently not valid - <code>isValid</code> returns false. To avoid
	 * excessive validation when the containment hierarchy is being created this
	 * will not validate if one of the ancestors does not have a peer, or there
	 * is no validate root ancestor, or one of the ancestors is not a
	 * <code>Window</code> or <code>Applet</code>.
	 * <p>
	 * Note that this method will not scroll outside of the valid viewport; for
	 * example, if <code>contentRect</code> is larger than the viewport,
	 * scrolling will be confined to the viewport's bounds.
	 * 
	 */
	public void scrollRectToVisibleCommand() {

		Element view = this.getWidget().getElement();

		if (view == null) {
			return;
		}
		int dx, dy;

		int barHeight = AbstractNativeScrollbar.getNativeScrollbarHeight();
		int barWidth = AbstractNativeScrollbar.getNativeScrollbarWidth();

		dx = positionAdjustment(this.getOffsetWidth() - barWidth,
		        (int) contentRect.getWidth(), (int) contentRect.getX()
		                - getAbsoluteLeft());
		dy = positionAdjustment(this.getOffsetHeight() - barHeight,
		        (int) contentRect.getHeight(), (int) contentRect.getY()
		                - getAbsoluteTop());

		// App.debug("-------- dx / dy : " + dx + " / " + dy);
		if (dx != 0 || dy != 0) {
			GPoint viewPosition = getViewPosition();
			Dimension viewSize = new Dimension(view.getOffsetWidth(),
			        view.getOffsetHeight());
			int startX = viewPosition.x;
			int startY = viewPosition.y;
			Dimension extent = new Dimension(this.getOffsetWidth() - barWidth,
			        this.getOffsetHeight() - barHeight);

			viewPosition.x -= dx;
			viewPosition.y -= dy;

			// TODO In the java code a check is made here for component
			// orientation, we may do this on the future?

			if (extent.width > viewSize.width) {
				viewPosition.x = viewSize.width - extent.width;
			} else {
				viewPosition.x = Math
				        .max(0, Math.min(viewSize.width - extent.width,
				                viewPosition.x));
			}

			if (viewPosition.y + extent.height > viewSize.height) {
				viewPosition.y = Math.max(0, viewSize.height - extent.height);
			} else if (viewPosition.y < 0) {
				viewPosition.y = 0;
			}

			// App.debug("viewPosition x / y : " + viewPosition.x + " / " +
			// viewPosition.y);

			if (viewPosition.x != startX || viewPosition.y != startY) {
				doAdjustScroll = false;
				setViewPosition(viewPosition);
				doAdjustScroll = true;
			}
		}
	}

	private void setViewPosition(GPoint viewPosition) {
		this.setHorizontalScrollPosition(viewPosition.x);
		this.setVerticalScrollPosition(viewPosition.y);
		syncHeaders();
	}

	private GPoint getViewPosition() {
		return new GPoint(getHorizontalScrollPosition(),
		        getVerticalScrollPosition());
	}

	private static class Dimension {
		int height;
		int width;

		public Dimension(int width, int height) {
			this.width = width;
			this.height = height;
		}
	}

	protected void adjustScroll(int dx, int dy) {
		if (!doAdjustScroll) {
			return;
		}

		int offH = cellTable.getAbsoluteLeft();
		int offV = cellTable.getAbsoluteTop();

		// get pixel coordinates of the upper left corner
		int x = getHorizontalScrollPosition() + offH;
		int y = getVerticalScrollPosition() + offV;

		// get upper left cell coordinates
		GPoint p = table.getIndexFromPixel(x, y);
		if (p == null) {
			return;
		}

		// get new pixel coordinates to place the upper left cell exactly
		GPoint p2 = table.getPixel(p.x + dx, p.y + dy, true);
		if (p2 == null) {
			return;
		}

		// now scroll to move the upper left cell into position
		int newScrollH = p2.x - offH;
		int newScrollV = p2.y - offV;
		// App.debug("scroll: " + x + " , " + y + "  col,row: " + p.x + " , "
		// + p.y + "  scroll2: " + newScrollH + " , " + newScrollV);

		doAdjustScroll = false;
		setHorizontalScrollPosition(newScrollH);
		setVerticalScrollPosition(newScrollV);
		doAdjustScroll = true;
	}

	@Override
	public void onScroll(ScrollEvent event) {
		adjustScroll(0, 0);
		syncHeaders();
	}
	
	private void syncHeaders() {
		int t = -getVerticalScrollPosition();
		int l = -getHorizontalScrollPosition();
		rowHeader.setTop(t);
		columnHeader.setLeft(l);
	}
	
	/* 
	 * Fits the content of spreadsheet for its header on the left.
	 */
	public void syncTableTop() {
		setVerticalScrollPosition(rowHeader.getTop());
	}

	/**
	 * @param showHScrollBar
	 *            true = hide the horizontal scroll bar
	 */
	public void setShowHScrollBar(boolean showHScrollBar) {
		getScrollableElement().getStyle().setOverflowX(
		        showHScrollBar ? Style.Overflow.AUTO : Style.Overflow.HIDDEN);
	}

	/**
	 * @param showVScrollBar true = hide the vertical scroll bar
	 */
	public void setShowVScrollBar(boolean showVScrollBar) {
		getScrollableElement().getStyle().setOverflowY(
		        showVScrollBar ? Style.Overflow.AUTO : Style.Overflow.HIDDEN);
	}

}
