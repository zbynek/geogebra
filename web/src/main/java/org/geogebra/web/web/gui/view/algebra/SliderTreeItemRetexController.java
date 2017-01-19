package org.geogebra.web.web.gui.view.algebra;

import org.geogebra.common.euclidian.event.AbstractEvent;
import org.geogebra.common.main.Feature;
import org.geogebra.web.html5.event.PointerEvent;
import org.geogebra.web.html5.event.ZeroOffset;
import org.geogebra.web.html5.gui.util.CancelEventTimer;
import org.geogebra.web.html5.util.sliderPanel.SliderWJquery;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Widget;

/**
 * Controller for slider items in AV that use RETEX editor.
 *
 * @author laszlo
 *
 */
public class SliderTreeItemRetexController extends LatexTreeItemController
		implements ValueChangeHandler<Double> {

	private SliderTreeItemRetex slider;

	public SliderTreeItemRetexController(SliderTreeItemRetex item) {
		super(item);
		slider = item;
	}

	@Override
	protected void onPointerUp(AbstractEvent event) {
		selectionCtrl.setSelectHandled(false);
		if (slider.minMaxPanel.isVisible()) {
			return;
		}
		super.onPointerUp(event);
	}

	@Override
	public void onMouseMove(MouseMoveEvent evt) {
		if (slider.sliderPanel == null) {
			evt.stopPropagation();
			return;
		}
		if (CancelEventTimer.cancelMouseEvent()) {
			return;
		}
		PointerEvent wrappedEvent = PointerEvent.wrapEvent(evt,
				ZeroOffset.instance);
		onPointerMove(wrappedEvent);
	}

	// @Override
	// public void onMouseOver(MouseOverEvent event) {
	// return;
	// }

	@Override
	public void onDoubleClick(DoubleClickEvent evt) {
		evt.stopPropagation();

		if ((isWidgetHit(slider.controls.getAnimPanel(), evt)
				|| (slider.minMaxPanel != null
						&& slider.minMaxPanel.isVisible())
				|| isMarbleHit(evt))) {
			return;
		}
		super.onDoubleClick(evt);
	}

	@Override
	protected boolean handleAVItem(int x, int y, boolean rightClick) {

		slider.setForceControls(true);


		boolean minHit = slider.sliderPanel != null
				&& isWidgetHit(slider.getSlider().getWidget(0), x, y);
		boolean maxHit = slider.sliderPanel != null
				&& isWidgetHit(slider.getSlider().getWidget(2), x, y);
		// Min max panel should be closed
		if (isAnotherMinMaxOpen() || isClickedOutMinMax(x, y)) {
			MinMaxPanel.closeMinMaxPanel(!(minHit || maxHit));
		}

		if (isAnotherMinMaxOpen()) {
			slider.selectItem(false);

		}

		if (slider.minMaxPanel != null && slider.minMaxPanel.isVisible()) {
			slider.selectItem(true);
			return false;
		}

		if (slider.sliderPanel != null && slider.sliderPanel.isVisible()
				&& !rightClick) {

			if (minHit || maxHit) {
				stopEdit();
				slider.minMaxPanel.show();
				if (minHit) {
					slider.minMaxPanel.setMinFocus();
				} else if (maxHit) {
					slider.minMaxPanel.setMaxFocus();
				}
				getApp().getKernel().notifyRepaint();
				return true;
			}
		}

		if (!selectionCtrl.isSelectHandled()) {
			slider.selectItem(true);
		}

		return false;

	}

	@Override
	public void onMouseDown(MouseDownEvent event) {
		if (handleAVItem(event)) {
			event.stopPropagation();
			return;
		}
		super.onMouseDown(event);
	}

	@Override
	public void onMouseUp(MouseUpEvent event) {
		SliderWJquery.stopSliders();
		super.onMouseUp(event);
	}

	@Override
	protected boolean canEditStart(MouseEvent<?> event) {

		return super.canEditStart(event)
				&& isWidgetHit(item.getPlainTextItem(), event);
	}

	private static boolean isWidgetHit(Widget w, int x, int y) {
		if (w == null) {
			return false;
		}
		int left = w.getAbsoluteLeft();
		int top = w.getAbsoluteTop();
		int right = left + w.getOffsetWidth();
		int bottom = top + w.getOffsetHeight();

		return (x > left && x < right && y > top && y < bottom);
	}

	/**
	 * @return true if another SliderTreeItem's min/max panel is showing.
	 */
	boolean isAnotherMinMaxOpen() {
		return (MinMaxPanel.getOpenedPanel() != null
				&& MinMaxPanel.getOpenedPanel() != slider.minMaxPanel);
	}

	private boolean isClickedOutMinMax(int x, int y) {
		return (MinMaxPanel.getOpenedPanel() == slider.minMaxPanel
				&& !isWidgetHit(slider.minMaxPanel, x, y));
	}

	@Override
	public void onValueChange(ValueChangeEvent<Double> event) {
		if (getApp().has(Feature.AV_SINGLE_TAP_EDIT) && isEditing()) {
			stopEdit();
		}

		slider.num.setValue(event.getValue());
		slider.geo.updateCascade();

		if (!slider.geo.isAnimating()) {
			if (isAnotherMinMaxOpen()) {
				MinMaxPanel.closeMinMaxPanel();
			}

			slider.selectItem(true);
			updateSelection(false, false);
		}
		// updates other views (e.g. Euclidian)
		getApp().getKernel().notifyRepaint();
	}

}
