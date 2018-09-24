/* TeXEnvironment.java
 * =========================================================================
 * This file is originally part of the JMathTeX Library - http://jmathtex.sourceforge.net
 *
 * Copyright (C) 2004-2007 Universiteit Gent
 * Copyright (C) 2009 DENIZET Calixte
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * A copy of the GNU General Public License can be found in the file
 * LICENSE.txt provided with the source distribution of this program (see
 * the META-INF directory in the source jar). This license can also be
 * found on the GNU website at http://www.gnu.org/licenses/gpl.html.
 *
 * If you did not receive a copy of the GNU General Public License along
 * with this program, contact the lead developer, or write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Linking this library statically or dynamically with other modules
 * is making a combined work based on this library. Thus, the terms
 * and conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce
 * an executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under terms
 * of your choice, provided that you also meet, for each linked independent
 * module, the terms and conditions of the license of that module.
 * An independent module is a module which is not derived from or based
 * on this library. If you modify this library, you may extend this exception
 * to your version of the library, but you are not obliged to do so.
 * If you do not wish to do so, delete this exception statement from your
 * version.
 *
 */

/* Modified by Calixte Denizet */

package com.himamis.retex.renderer.share;

import com.himamis.retex.renderer.share.platform.font.Font;
import com.himamis.retex.renderer.share.platform.graphics.Color;

/**
 * Contains the used TeXFont-object, color settings and the current style in
 * which a formula must be drawn. It's used in the createBox-methods. Contains
 * methods that apply the style changing rules for subformula's.
 */
public class TeXEnvironment {

	// colors
	private Color background = null;
	private Color color = null;

	// current style
	private int style = TeXConstants.STYLE_DISPLAY;

	// TeXFont used
	private TeXFont tf;

	// Java Font to use
	private Font javaFont;

	// last used font
	private Font_ID lastFontId;

	private double textwidth = Double.POSITIVE_INFINITY;

	private int textStyle = TextStyle.NONE;
	private boolean smallCap;
	private double scaleFactor = 1.;
	private TeXLength.Unit interlineUnit;
	private double interline;

	public boolean isColored = false;

	public TeXEnvironment(int style, TeXFont tf, int textStyle) {
		this(style, tf, null, null, textStyle);
	}

	public TeXEnvironment(int style, TeXFont tf, TeXLength.Unit widthUnit,
			double textwidth, int textStyle) {
		this(style, tf, null, null, textStyle);
		this.textwidth = textwidth * TeXLength.getFactor(widthUnit, this);
	}

	private TeXEnvironment(int style, TeXFont tf, Color bg, Color c,
			int textStyle) {
		this.style = style;
		this.tf = tf;
		background = bg;
		color = c;
		this.textStyle = textStyle;
		setInterline(TeXLength.Unit.EX, 1.);
	}

	private TeXEnvironment(int style, double scaleFactor, TeXFont tf, Color bg,
			Color c, int textStyle, boolean smallCap, Font javaFont) {
		this.style = style;
		this.scaleFactor = scaleFactor;
		this.tf = tf;
		this.textStyle = textStyle;
		this.smallCap = smallCap;
		this.javaFont = javaFont;
		this.background = bg;
		this.color = c;
		setInterline(TeXLength.Unit.EX, 1.);
	}

	public void setInterline(TeXLength.Unit unit, double len) {
		this.interline = len;
		this.interlineUnit = unit;
	}

	public double getInterline() {
		return interline * TeXLength.getFactor(interlineUnit, this);
	}

	public void setTextwidth(TeXLength.Unit widthUnit, double textwidth) {
		this.textwidth = textwidth * TeXLength.getFactor(widthUnit, this);
	}

	public double getTextwidth() {
		return textwidth;
	}

	public void setScaleFactor(double f) {
		scaleFactor = f;
	}

	public double getScaleFactor() {
		return scaleFactor;
	}

	protected TeXEnvironment copy() {
		return new TeXEnvironment(style, scaleFactor, tf, background, color,
				textStyle, smallCap, javaFont);
	}

	protected TeXEnvironment copy(TeXFont tf) {
		TeXEnvironment te = new TeXEnvironment(style, scaleFactor, tf,
				background, color, textStyle, smallCap, javaFont);
		te.textwidth = textwidth;
		te.interline = interline;
		te.interlineUnit = interlineUnit;
		return te;
	}

	/**
	 * @return a copy of the environment, but in a cramped style.
	 */
	public TeXEnvironment crampStyle() {
		TeXEnvironment s = copy();
		s.style = style | 1;
		return s;
	}

	/**
	 *
	 * @return a copy of the environment, but in denominator style.
	 */
	public TeXEnvironment denomStyle() {
		TeXEnvironment s = copy();
		s.style = style <= 3 ? ((style & 2) + 3) : 7;
		return s;
	}

	/**
	 *
	 * @return a copy of the environment, but in numerator style.
	 */
	public TeXEnvironment numStyle() {
		TeXEnvironment s = copy();
		s.style = (style <= 5 ? 2 : 0) + style;
		return s;
	}

	/**
	 *
	 * @return a copy of the environment, but in subscript style.
	 */
	public TeXEnvironment subStyle() {
		TeXEnvironment s = copy();
		s.style = style <= 3 ? 5 : 7;
		return s;
	}

	/**
	 *
	 * @return a copy of the environment, but in superscript style.
	 */
	public TeXEnvironment supStyle() {
		TeXEnvironment s = copy();
		s.style = (style <= 3 ? 4 : 6) + (style & 1);
		return s;
	}

	/**
	 *
	 * @return the background color setting
	 */
	public Color getBackground() {
		return background;
	}

	/**
	 *
	 * @return the foreground color setting
	 */
	public Color getColor() {
		return color;
	}

	/**
	 *
	 * @return the point size of the TeXFont
	 */
	public double getSize() {
		return tf.getSize();
	}

	/**
	 *
	 * @return the current style
	 */
	public int getStyle() {
		return style;
	}

	public void setStyle(int style) {
		this.style = style;
	}

	/**
	 * @return the current textStyle
	 */
	public int getTextStyle() {
		return textStyle;
	}

	public void setTextStyle(int textStyle) {
		this.textStyle = textStyle;
	}

	/**
	 * @return the current java Font
	 */
	public Font getJavaFont() {
		return javaFont;
	}

	public void setJavaFont(Font javaFont) {
		this.javaFont = javaFont;
	}

	/**
	 * @return the current textStyle
	 */
	public boolean getSmallCap() {
		return smallCap;
	}

	public void setSmallCap(boolean smallCap) {
		this.smallCap = smallCap;
	}

	/**
	 *
	 * @return the TeXFont to be used
	 */
	public TeXFont getTeXFont() {
		return tf;
	}

	/**
	 * Resets the color settings.
	 *
	 */
	public void resetColors() {
		color = null;
		background = null;
	}

	/**
	 *
	 * @return a copy of the environment, but with the style changed for roots
	 */
	public TeXEnvironment rootStyle() {
		TeXEnvironment s = copy();
		s.style = TeXConstants.STYLE_SCRIPT_SCRIPT;
		return s;
	}

	/**
	 *
	 * @param c
	 *            the background color to be set
	 */
	public void setBackground(Color c) {
		background = c;
	}

	/**
	 *
	 * @param c
	 *            the foreground color to be set
	 */
	public void setColor(Color c) {
		color = c;
	}

	public double getSpace() {
		return tf.getSpace(style) * tf.getScaleFactor();
	}

	public void setLastFontId(Font_ID id) {
		lastFontId = id;
	}

	public Font_ID getLastFontId() {
		// if there was no last font id (whitespace boxes only), use default "mu
		// font"
		return (lastFontId == null ? TeXFont.MUFONT : lastFontId);
	}
}
