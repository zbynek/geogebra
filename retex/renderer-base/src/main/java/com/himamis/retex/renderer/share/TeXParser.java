/* TeXParser.java
 * =========================================================================
 * This file is part of the JLaTeXMath Library - http://forge.scilab.org/jlatexmath
 *
 * Copyright (C) 2018 DENIZET Calixte
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

package com.himamis.retex.renderer.share;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.himamis.retex.renderer.share.character.Character;
import com.himamis.retex.renderer.share.exception.ParseException;
import com.himamis.retex.renderer.share.platform.FactoryProvider;
import com.himamis.retex.renderer.share.platform.graphics.Color;

/**
 * This class implements a parser for LaTeX formulas.
 */
public class TeXParser {

	protected ArrayDeque<AtomConsumer> stack;
	protected ArrayDeque<ParsedString> stringStack;
	protected ArrayDeque<Boolean> modeStack;
	protected String parseString;
	protected int cpos = -1;
	protected int pos;
	protected int prevpos;
	protected int line;
	protected int col;
	protected int len;
	protected int stopPos = -1;
	protected boolean ignoreWhiteSpace = true;
	protected CharMapping charMapping = CharMapping.getDefault();
	private Configuration configuration = Configuration.get();
	private Map<String, String> xmlMap;

	// TODO: handle correctly partial stuff
	protected boolean isPartial;

	// 00 01 02 03 04 05 06 07 08 09
	private static final int[] HEX_ARRAY = { 16, 16, 16, 16, 16, 16, 16, 16, 16,
			16, // 00
			16, 16, 16, 16, 16, 16, 16, 16, 16, 16, // 10
			16, 16, 16, 16, 16, 16, 16, 16, 16, 16, // 20
			16, 16, 16, 16, 16, 16, 16, 16, 16, 16, // 30
			16, 16, 16, 16, 16, 16, 16, 16, 0, 1, // 40
			2, 3, 4, 5, 6, 7, 8, 9, 16, 16, // 50
			16, 16, 16, 16, 16, 10, 11, 12, 13, 14, // 60
			15, 16, 16, 16, 16, 16, 16, 16, 16, 16, // 70
			16, 16, 16, 16, 16, 16, 16, 16, 16, 16, // 80
			16, 16, 16, 16, 16, 16, 16, 10, 11, 12, // 90
			13, 14, 15 }; // 100

	public static final int MAX_DEC = 6;
	public static final boolean MATH_MODE = true;
	public static final boolean TEXT_MODE = false;

	private static final double[] POWTEN = { 1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6,
			1e7 };

	private static final class DoubleOrInt {

		final int i;
		final double f;
		final boolean isdouble;

		public DoubleOrInt(final int i) {
			this.i = i;
			this.f = 0.;
			isdouble = false;
		}

		public DoubleOrInt(final double f) {
			this.f = f;
			this.i = 0;
			isdouble = true;
		}

		public double getDouble() {
			return isdouble ? f : (double) i;
		}

		public int getInt() {
			return isdouble ? (int) f : i;
		}

		@Override
		public String toString() {
			return "number: " + (isdouble ? f : i);
		}
	}

	public TeXParser(boolean isPartial, String parseString) {
		initStack();
		this.stringStack = new ArrayDeque<>();
		this.isPartial = isPartial;
		if (parseString != null) {
			this.parseString = parseString;
			this.len = parseString.length();
		} else {
			this.parseString = null;
			this.len = 0;
		}
		this.line = 1;
		this.pos = 0;
		this.col = -1;
	}

	public TeXParser(String parseString) {
		this(false, parseString);
	}

	public TeXParser(boolean withStack) {
		if (withStack) {
			initStack();
			this.stringStack = new ArrayDeque<>();
		}
		this.line = 1;
		this.pos = 0;
		this.col = -1;
	}

	public TeXParser(String parseString, int pos, int line, int col) {
		stack = new ArrayDeque<>();
		this.stringStack = new ArrayDeque<>();
		this.parseString = parseString;
		this.len = parseString.length();
		this.line = line;
		this.pos = pos;
		this.col = col;
	}

	public TeXParser() {
		this(false);
	}

	public static double parseDouble(final String s) {
		final TeXParser tp = new TeXParser(s);
		return tp.getDecimal();
	}

	private final void initStack() {
		stack = new ArrayDeque<>();
		addConsumer(new GroupConsumer(TeXConstants.Opener.NONE));
	}

	void setXMLMap(final Map<String, String> map) {
		this.xmlMap = map;
	}

	public Map<String, String> getXMLMap() {
		return xmlMap;
	}

	public boolean isPartial() {
		return this.isPartial;
	}

	public void setParseString(final String parseString) {
		this.parseString = parseString;
		this.len = parseString.length();
		this.line = 1;
		this.pos = 0;
		this.col = -1;
	}

	public void reset(final String parseString) {
		this.stack.clear();
		this.stringStack.clear();
		this.isPartial = isPartial;
		if (parseString != null) {
			this.parseString = parseString;
			this.len = parseString.length();
		} else {
			this.parseString = null;
			this.len = 0;
		}
		this.line = 1;
		this.pos = 0;
		this.col = -1;
	}

	public void setLine(final int line) {
		this.line = line;
	}

	/**
	 * Get the number of the current line
	 */
	public final int getLine() {
		return line;
	}

	/**
	 * Get the number of the current column
	 */
	public final int getCol() {
		return pos - col;
	}

	/**
	 * Get the number of the current column
	 */
	public final int getPrevCol() {
		return prevpos - col;
	}

	/**
	 * Get the position in the parsed string
	 */
	public final int getPos() {
		return pos;
	}

	public final int[] getStopInfo() {
		return new int[] { stopPos, line, col };
	}

	public final void setStopPos(int stopPos) {
		this.stopPos = stopPos;
	}

	public void stop() {
		setStopPos(pos);
		pos = len;
	}

	public CharMapping getCharMapping() {
		return charMapping;
	}

	public void setCharMapping(CharMapping cm) {
		this.charMapping = cm;
	}

	/**
	 * Get the position in the parsed string
	 */
	public String getParsedString() {
		return parseString;
	}

	public void addString(final String s) {
		addString(s, false);
	}

	public void addString(final String s, final boolean stop) {
		if (stringStack.size() >= 4096) {
			throw new ParseException(this, "Recursion level too high");
		}
		stringStack
				.push(new ParsedString(parseString, len, pos, line, col, stop));
		parseString = s;
		len = s.length();
		pos = 0;
		line = 1;
		col = -1;
	}

	public void popString() {
		final ParsedString ps = stringStack.pop();
		parseString = ps.getString();
		len = ps.getLen();
		pos = ps.getPos();
	}

	public boolean removeString() {
		while (!stringStack.isEmpty()) {
			final ParsedString ps = stringStack.pop();
			parseString = ps.getString();
			len = ps.getLen();
			pos = ps.getPos();
			if (ps.getStop()) {
				// we stop the parsing
				// and go back where parse() has been called
				close();
				return false;
			}
			if (pos < len) {
				line = ps.getLine();
				col = ps.getCol();
				return true;
			}
		}
		return false;
	}

	public void pushMode(final boolean mode) {
		if (modeStack == null) {
			modeStack = new ArrayDeque<>();
		}
		modeStack.push(ignoreWhiteSpace);
		ignoreWhiteSpace = mode;
	}

	public void popMode() {
		ignoreWhiteSpace = modeStack.pop().booleanValue();
	}

	public boolean isHandlingArg() {
		return stack.peek().isHandlingArg();
	}

	public boolean isArrayMode() {
		return stack.peek().isArray();
	}

	public boolean setMathMode() {
		final boolean b = ignoreWhiteSpace;
		ignoreWhiteSpace = true;
		return b;
	}

	public boolean setTextMode() {
		final boolean b = ignoreWhiteSpace;
		ignoreWhiteSpace = false;
		return b;
	}

	public void setMathMode(boolean b) {
		ignoreWhiteSpace = b;
	}

	public boolean isMathMode() {
		return ignoreWhiteSpace;
	}

	public boolean isTextMode() {
		return !ignoreWhiteSpace;
	}

	public boolean isAmpersandAllowed() {
		return stack.peek().isAmpersandAllowed();
	}

	public char getChar() {
		return pos < len ? parseString.charAt(pos) : '\0';
	}

	public void closeConsumer(Atom a) throws ParseException {
		stack.pop();
		if (stack.isEmpty()) {
			addConsumer(new GroupConsumer(TeXConstants.Opener.NONE, a));
		} else {
			addToConsumer(a);
		}
	}

	public AtomConsumer pop() {
		if (!stack.isEmpty()) {
			return stack.pop();
		}
		return null;
	}

	public AtomConsumer peek() {
		if (!stack.isEmpty()) {
			return stack.peek();
		}
		return null;
	}

	public void addConsumer(final AtomConsumer ac) {
		stack.push(ac);
	}

	public void addToConsumer(Atom a) {
		stack.peek().add(this, a);
	}

	public Atom get() {
		if (stack.isEmpty()) {
			return EmptyAtom.get();
		}
		return flush();
	}

	public void cancelPrevPos() {
		prevpos = -1;
	}

	public int getPrevPos() {
		return prevpos;
	}

	public void parse() throws ParseException {
		while (pos < len || removeString()) {
			final char c = parseString.charAt(pos);
			switch (c) {
			case '\u0000':
			case '\u0001':
			case '\u0002':
			case '\u0003':
			case '\u0004':
			case '\u0005':
			case '\u0006':
			case '\u0007':
			case '\u0008':
			case '\u0009':
				++pos;
				break;
			case '\n':
				newLine();
				skipWhites();
				break;
			case '\u000B':
			case '\u000C':
			case '\r':
			case '\u000E':
			case '\u000F':
			case '\u0010':
			case '\u0011':
			case '\u0012':
			case '\u0013':
			case '\u0014':
			case '\u0015':
			case '\u0016':
			case '\u0017':
			case '\u0018':
			case '\u0019':
			case '\u001A':
			case '\u001B':
			case '\u001C':
			case '\u001D':
			case '\u001E':
			case '\u001F':
				++pos;
				break;
			case ' ':
				++pos;
				if (isTextMode()) { // We are in a mbox
					// TODO: erreur ici: il faut ajouter l'un puis l'autre (meme
					// flux)
					// faire attention a bien avoir un rowatom ou truc du
					// genre...
					addToConsumer(
							new RowAtom(new SpaceAtom(), BreakMarkAtom.get()));
					skipPureWhites();
				}
				break;
			case '$':
				++pos;
				processDollar();
				break;
			case '%':
				++line;
				col = pos++;

				// We've a comment
				++pos;
				skipUntilCr();
				break;
			case '&':
				close();
				if (isAmpersandAllowed()) {
					++pos;
					addToConsumer(EnvArray.ColSep.get());
				} else {
					throw new ParseException(this,
							"Character '&' is only available in array mode !");
				}
				break;
			case '!':
			case '(':
			case ')':
			case '*':
			case '+':
			case ',':
			case '-':
			case '.':
			case '/':
			case ':':
			case ';':
			case '<':
			case '=':
			case '>':
			case '?':
			case '`':
			case '#':
			case '@':
				++pos;
				charMapping.replaceUnsafe(c, this);
				break;
			case '\'':
				++pos;
				if (isTextMode()) {
					charMapping.replaceUnsafe('\'', this);
				} else {
					cumSupSymbols(Symbols.PRIME);
				}
				break;
			case '\"':
				++pos;
				charMapping.replaceUnsafe('\'', this);
				charMapping.replaceUnsafe('\'', this);
				break;
			case '[':
				++pos;
				charMapping.replaceUnsafe('[', this);
				break;
			case '\\':
				cpos = pos;
				prevpos = pos;
				final String command = getCommand();
				if (!command.isEmpty()) {
					processCommand(command);
				}
				cpos = -1;
				break;
			case ']':
				++pos;
				if (!processRSqBracket()) {
					charMapping.replaceUnsafe(']', this);
				}
				break;
			case '^':
				++pos;
				processSubSup('^');
				break;
			case '_':
				++pos;
				processSubSup('_');
				break;
			case 'a':
			case 'b':
			case 'c':
			case 'd':
			case 'e':
			case 'f':
			case 'g':
			case 'h':
			case 'i':
			case 'j':
			case 'k':
			case 'l':
			case 'm':
			case 'n':
			case 'o':
			case 'p':
			case 'q':
			case 'r':
			case 's':
			case 't':
			case 'u':
			case 'v':
			case 'w':
			case 'x':
			case 'y':
			case 'z':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'G':
			case 'H':
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'O':
			case 'P':
			case 'Q':
			case 'R':
			case 'S':
			case 'T':
			case 'U':
			case 'V':
			case 'W':
			case 'X':
			case 'Y':
			case 'Z':
				++pos;
				convertASCIIChar(c, false);
				break;
			case '{':
				++pos;
				processLBrace();
				break;
			case '|':
				++pos;
				charMapping.replaceUnsafe('|', this);
				break;
			case '}':
				++pos;
				processRBrace();
				break;
			case '~':
				++pos;
				addToConsumer(new SpaceAtom());
				break;
			default:
				++pos;
				convertCharacter(c, false);
				break;
			}
		}
	}

	public static Atom getAtomForLatinStr(final String s,
			final boolean mathMode) {
		final ExternalFontManager.FontSSSF f = ExternalFontManager.get()
				.getFont(Character.UnicodeBlock.BASIC_LATIN);
		if (f != null) {
			return new JavaFontRenderingAtom(s, f);
		}

		final int N = s.length();
		final RowAtom ra = new RowAtom(N);
		for (int i = 0; i < N; ++i) {
			final char c = s.charAt(i);
			ra.add(new CharAtom(c, mathMode));
		}

		return ra.simplify();
	}

	public static RowAtom getAtomForLatinStr(final String s, final RowAtom ra,
			final boolean mathMode) {
		final ExternalFontManager.FontSSSF f = ExternalFontManager.get()
				.getFont(Character.UnicodeBlock.BASIC_LATIN);
		if (f != null) {
			ra.add(new JavaFontRenderingAtom(s, f));
			return ra;
		}

		for (int i = 0; i < s.length(); ++i) {
			final char c = s.charAt(i);
			ra.add(new CharAtom(c, mathMode));
		}

		return ra;
	}

	public static Atom getAtomForLatinStr(final String s, final int textStyle,
			final boolean math) {
		final RowAtom ra = new RowAtom(s.length());
		for (int i = 0; i < s.length(); ++i) {
			final char c = s.charAt(i);
			ra.add(new CharAtom(c, textStyle, math));
		}

		return ra.simplify();
	}

	public int getNumberOf(final char c) {
		int n = 1;
		while (++pos < len) {
			final char cc = parseString.charAt(pos);
			if (cc != c) {
				return n;
			}
			++n;
		}
		return n;
	}

	public char getArgAsCharFromCode() {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				skipPureWhites();
				final char r = getCharFromCode();
				skipPureWhites();
				if (pos < len) {
					c = parseString.charAt(pos);
					if (c == '}') {
						++pos;
						return r;
					} else {
						throw new ParseException(this,
								"A closing '}' expected");
					}
				}
				return r;
			}

			final char r = getCharFromCode();
			skipPureWhites();
			return r;
		}
		throw new ParseException(this, "A char code expected");
	}

	public char getCharFromCode() {
		if (pos < len) {
			prevpos = pos;
			char c = parseString.charAt(pos);
			if (c == '0') {
				++pos;
				if (pos < len) {
					c = parseString.charAt(pos);
					if (c == 'x' || c == 'X') {
						++pos;
						return getCharHex();
					}
					return getCharOct();
				}
			} else if (c == 'x' || c == 'X') {
				++pos;
				return getCharHex();
			} else {
				return getCharDec();
			}
		}
		throw new ParseException(this, "Invalid char code");
	}

	private char getCharHex() {
		if (pos < len) {
			char c = parseString.charAt(pos);
			int acc = getHex(c);
			if (acc == 16) {
				throw new ParseException(this,
						"An hexadecimal number expected");
			}
			int ncomp = 1;
			++pos;
			while (pos < len) {
				c = parseString.charAt(pos);
				final int n = getHex(c);
				if (n == 16) {
					cancelPrevPos();
					return (char) acc;
				}
				acc = (acc << 4) | n;
				++pos;
				if (ncomp == 3) {
					cancelPrevPos();
					return (char) acc;
				}
				++ncomp;
			}
			cancelPrevPos();
			return (char) acc;
		}
		cancelPrevPos();
		return 0;
	}

	public TeXLength[] getDimensions() {
		// we are looking for keywords: width, height, depth followed by a
		// length
		TeXLength w = null;
		TeXLength h = null;
		TeXLength d = null;
		skipPureWhites();
		while (pos + 4 < len && (w == null || h == null || d == null)) {
			final char c = parseString.charAt(pos);
			if (c == 'w' && parseString.charAt(pos + 1) == 'i'
					&& parseString.charAt(pos + 2) == 'd'
					&& parseString.charAt(pos + 3) == 't'
					&& parseString.charAt(pos + 4) == 'h') {
				pos += 5;
				skipPureWhites();
				w = getLength();
			} else if (c == 'h' && pos + 5 < len
					&& parseString.charAt(pos + 1) == 'e'
					&& parseString.charAt(pos + 2) == 'i'
					&& parseString.charAt(pos + 3) == 'g'
					&& parseString.charAt(pos + 4) == 'h'
					&& parseString.charAt(pos + 5) == 't') {
				pos += 6;
				skipPureWhites();
				h = getLength();
			} else if (c == 'd' && parseString.charAt(pos + 1) == 'e'
					&& parseString.charAt(pos + 2) == 'p'
					&& parseString.charAt(pos + 3) == 't'
					&& parseString.charAt(pos + 4) == 'h') {
				pos += 5;
				skipPureWhites();
				d = getLength();
			} else {
				break;
			}
			skipPureWhites();
		}
		return new TeXLength[] { w, h, d };
	}

	private char getCharDec() {
		// 65535 = 0xFFFF
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c >= '1' && c <= '9') {
				int acc = c - '0';
				int ncomp = 1;
				++pos;
				while (pos < len) {
					c = parseString.charAt(pos);
					if (c < '0' || c > '9') {
						cancelPrevPos();
						return (char) acc;
					}
					if (ncomp == 4) {
						if (acc > 6553) {
							cancelPrevPos();
							return (char) acc;
						}
						if (acc == 6553) {
							if (c >= '0' && c <= '5') {
								++pos;
								cancelPrevPos();
								return (char) (10 * acc + c - '0');
							}
							cancelPrevPos();
							return 6553;
						}
						++pos;
						cancelPrevPos();
						return (char) (10 * acc + c - '0');
					}
					++pos;
					acc = 10 * acc + c - '0';
					++ncomp;
				}
				cancelPrevPos();
				return (char) acc;
			}
		}
		cancelPrevPos();
		return 0;
	}

	private char getCharOct() {
		// 0177777 = 0xFFFF
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c >= '0' && c <= '7') {
				int acc = c - '0';
				boolean one = acc == 1;
				int ncomp = 1;
				++pos;
				while (pos < len) {
					c = parseString.charAt(pos);
					if (c < '0' || c > '7') {
						cancelPrevPos();
						return (char) acc;
					}
					acc = 8 * acc + c - '0';
					if (ncomp == 4) {
						++pos;
						if (one && pos < len) {
							c = parseString.charAt(pos);
							if (c >= '0' && c <= '7') {
								++pos;
								cancelPrevPos();
								return (char) (8 * acc + c - '0');
							}
						}
						cancelPrevPos();
						return (char) acc;
					}
					++pos;
					++ncomp;
				}
				cancelPrevPos();
				return (char) acc;
			}
		}
		cancelPrevPos();
		return 0;
	}

	public RowAtom steal() {
		return stack.peek().steal(this);
	}

	public Atom getLastAtom() {
		return stack.peek().getLastAtom();
	}

	public void cumSupSymbols(Atom... syms) {
		final RowAtom ra = new RowAtom(syms);
		while (pos < len) {
			final char c = parseString.charAt(pos);
			if (convSup(c, ra)) {
				++pos;
			} else {
				break;
			}
		}
		processSubSup('^');
		addToConsumer(new CumulativeScriptsAtom(ra));
	}

	public void cumSubSymbols(Atom... syms) {
		final RowAtom ra = new RowAtom(syms);
		while (pos < len) {
			final char c = parseString.charAt(pos);
			if (convSub(c, ra)) {
				++pos;
			} else {
				break;
			}
		}
		processSubSup('_');
		addToConsumer(new CumulativeScriptsAtom(ra));
	}

	private boolean convSup(final char c, final RowAtom ra) {
		switch (c) {
		case CharMapping.APOSTROPHE:
			ra.add(Symbols.PRIME);
			break;
		case CharMapping.SUPZERO:
			ra.add(new CharAtom('0', isMathMode()));
			break;
		case CharMapping.SUPONE:
			ra.add(new CharAtom('1', isMathMode()));
			break;
		case CharMapping.SUPTWO:
			ra.add(new CharAtom('2', isMathMode()));
			break;
		case CharMapping.SUPTHREE:
			ra.add(new CharAtom('3', isMathMode()));
			break;
		case CharMapping.SUPFOUR:
			ra.add(new CharAtom('4', isMathMode()));
			break;
		case CharMapping.SUPFIVE:
			ra.add(new CharAtom('5', isMathMode()));
			break;
		case CharMapping.SUPSIX:
			ra.add(new CharAtom('6', isMathMode()));
			break;
		case CharMapping.SUPSEVEN:
			ra.add(new CharAtom('7', isMathMode()));
			break;
		case CharMapping.SUPEIGHT:
			ra.add(new CharAtom('8', isMathMode()));
			break;
		case CharMapping.SUPNINE:
			ra.add(new CharAtom('9', isMathMode()));
			break;
		case CharMapping.SUPPLUS:
			ra.add(charMapping.get('+', this));
			break;
		case CharMapping.SUPMINUS:
			ra.add(charMapping.get('-', this));
			break;
		case CharMapping.SUPEQUAL:
			ra.add(charMapping.get('=', this));
			break;
		case CharMapping.SUPLPAR:
			ra.add(charMapping.get('(', this));
			break;
		case CharMapping.SUPRPAR:
			ra.add(charMapping.get(')', this));
			break;
		case CharMapping.SUPN:
			ra.add(new CharAtom('n', isMathMode()));
			break;
		default:
			return false;
		}
		return true;
	}

	private boolean convSub(final char c, final RowAtom ra) {
		switch (c) {
		case CharMapping.SUBZERO:
			ra.add(new CharAtom('0', isMathMode()));
			break;
		case CharMapping.SUBONE:
			ra.add(new CharAtom('1', isMathMode()));
			break;
		case CharMapping.SUBTWO:
			ra.add(new CharAtom('2', isMathMode()));
			break;
		case CharMapping.SUBTHREE:
			ra.add(new CharAtom('3', isMathMode()));
			break;
		case CharMapping.SUBFOUR:
			ra.add(new CharAtom('4', isMathMode()));
			break;
		case CharMapping.SUBFIVE:
			ra.add(new CharAtom('5', isMathMode()));
			break;
		case CharMapping.SUBSIX:
			ra.add(new CharAtom('6', isMathMode()));
			break;
		case CharMapping.SUBSEVEN:
			ra.add(new CharAtom('7', isMathMode()));
			break;
		case CharMapping.SUBEIGHT:
			ra.add(new CharAtom('8', isMathMode()));
			break;
		case CharMapping.SUBNINE:
			ra.add(new CharAtom('9', isMathMode()));
			break;
		case CharMapping.SUBPLUS:
			ra.add(charMapping.get('+', this));
			break;
		case CharMapping.SUBMINUS:
			ra.add(charMapping.get('-', this));
			break;
		case CharMapping.SUBEQUAL:
			ra.add(charMapping.get('=', this));
			break;
		case CharMapping.SUBLPAR:
			ra.add(charMapping.get('(', this));
			break;
		case CharMapping.SUBRPAR:
			ra.add(charMapping.get(')', this));
			break;
		default:
			return false;
		}
		return true;
	}

	protected static final boolean isRomanLetter(final char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	public String getCommand() {
		++pos;
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (isRomanLetter(c)) {
				final int spos = pos++;
				while (pos < len) {
					c = parseString.charAt(pos);
					if (!isRomanLetter(c)) {
						final String com = parseString.substring(spos, pos);
						if (c == ' ') {
							// skip the white just after the command
							++pos;
						}
						return com;
					}
					++pos;
				}
				return parseString.substring(spos);
			}
			++pos;
			return Character.toString(c);
		}
		return "";
	}

	public String getCommandWithBackslash() {
		final int spos = pos++;
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (isRomanLetter(c)) {
				++pos;
				while (pos < len) {
					c = parseString.charAt(pos);
					if (!isRomanLetter(c)) {
						return parseString.substring(spos, pos);
					}
					++pos;
				}
				return parseString.substring(spos);
			}
			++pos;
			return parseString.substring(spos, pos);
		}
		return "";
	}

	public boolean hasOption() {
		skipPureWhites();
		if (pos < len && parseString.charAt(pos) == '[') {
			++pos;
			return true;
		}
		return false;
	}

	public boolean hasOptionNoWhites() {
		if (pos < len && parseString.charAt(pos) == '[') {
			++pos;
			return true;
		}
		return false;
	}

	public void skipWhites() {
		while (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '\n') {
				newLine();
			} else if (c > ' ') {
				return;
			} else {
				++pos;
			}
		}
	}

	private static final boolean isWhite(final char c) {
		return c == ' ' || c == '\t' || c == '\r';
	}

	public void skipSeparator(final char ch) {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == ch) {
				++pos;
				skipPureWhites();
				return;
			}
		}
		throw new ParseException(this, "Expect a '" + ch + "'");
	}

	public void skipSeparators(final String seps) {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			for (int i = 0; i < seps.length(); ++i) {
				if (seps.charAt(i) == c) {
					++pos;
					skipPureWhites();
					return;
				}
			}
		}
		throw new ParseException(this, "Expect one of \"" + seps + "\"");
	}

	protected final void newLine() {
		++line;
		col = pos++;
	}

	public void skipPureWhites() {
		while (pos < len) {
			final char c = parseString.charAt(pos);
			if (!isWhite(c)) {
				return;
			}
			++pos;
		}
	}

	public void skipUntilCr() {
		while (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '\n') {
				newLine();
				return;
			}
			++pos;
		}
	}

	public String getString(final char stop) {
		final int spos = pos++;
		while (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == stop) {
				final String o = parseString.substring(spos, pos);
				++pos;
				return o;
			}
			++pos;
		}
		return parseString.substring(spos);
	}

	public Map<String, String> getOptionAsMap() {
		final Map<String, String> map = new HashMap<>();
		skipPureWhites();
		if (pos < len) {
			if (parseString.charAt(pos) == '[') {
				++pos;
				skipPureWhites();
				String key = null;
				int spos = pos;
				int epos = -1;
				while (pos < len) {
					final char c = parseString.charAt(pos);
					switch (c) {
					case '=':
						if (key != null) {
							throw new ParseException(this,
									"Invalid '=' in options");
						}
						key = getStringForKV(spos, epos);
						++pos;
						skipPureWhites();
						spos = pos;
						break;
					case ',':
					case ';':
						final String k = getStringForKV(spos, epos);
						if (key == null) {
							if (!k.isEmpty()) {
								map.put(k, null);
							}
						} else {
							map.put(key, k);
						}
						key = null;
						++pos;
						skipPureWhites();
						spos = pos;
						break;
					case ']':
						if (key == null) {
							if (spos == pos) {
								++pos;
								return map;
							}
							map.put(getStringForKV(spos, epos), null);
						} else {
							map.put(key, getStringForKV(spos, epos));
						}
						++pos;
						return map;
					case ' ':
					case '\t':
						epos = pos;
						++pos;
						skipPureWhites();
						break;
					default:
						epos = -1;
						++pos;
					}
				}
			}
		}
		return map;
	}

	private String getStringForKV(final int spos, final int epos) {
		return parseString.substring(spos, epos != -1 ? epos : pos);
	}

	public String getOptionAsString() {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '[') {
				++pos;
				return getString(']');
			}
		}
		return "";
	}

	public String getArgAsString() {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				return getString('}');
			}
		}
		throw new ParseException(this,
				"An argument expected between curly braces");
	}

	public String getArgAsCommandName() {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				skipPureWhites();
				final int spos = pos;
				c = parseString.charAt(pos);
				if (isRomanLetter(c)) {
					while (pos < len) {
						c = parseString.charAt(pos);
						if (!isRomanLetter(c)) {
							if (c == '}') {
								return parseString.substring(spos, pos++);
							}
							throw new ParseException(this,
									"A closing '}' expected");
						}
						++pos;
					}
					return parseString.substring(spos);
				}
			}
		}
		throw new ParseException(this, "A name expected between curly braces");
	}

	public int getPositiveInteger(final char stop) {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c >= '0' && c <= '9') {
				++pos;
				int acc = c - '0';
				while (pos < len) {
					c = parseString.charAt(pos);
					if (c >= '0' && c <= '9') {
						++pos;
						acc = 10 * acc + c - '0';
					} else if (c == stop) {
						++pos;
						return acc;
					} else if (isWhite(c)) {
						++pos;
						skipPureWhites();
					} else {
						throw new ParseException(this,
								"A positive integer expected");
					}
				}

				return acc;
			} else if (c == stop) {
				++pos;
			}
		}
		return -1;
	}

	public int getPositiveInteger() {
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c >= '0' && c <= '9') {
				++pos;
				int acc = c - '0';
				while (pos < len) {
					c = parseString.charAt(pos);
					if (c >= '0' && c <= '9') {
						++pos;
						acc = 10 * acc + c - '0';
					} else {
						return acc;
					}
				}

				return acc;
			}
		}
		throw new ParseException(this, "Expect a positive integer");
	}

	public int getOptionAsPositiveInteger(int def) {
		final int r = getOptionAsPositiveInteger();
		return r == -1 ? def : r;
	}

	public int getOptionAsPositiveInteger() {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '[') {
				++pos;
				return getPositiveInteger(']');
			}
		}
		return -1;
	}

	public int getArgAsPositiveInteger() {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				return getPositiveInteger('}');
			} else if (c >= '0' && c <= '9') {
				++pos;
				return c - '0';
			}
		}
		throw new ParseException(this,
				"A positive integer expected as argument");
	}

	public String getArgAsCommand() {
		return getArgAsCommand(false);
	}

	public String getArgAsCommand(boolean isLength) {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				if (pos < len) {
					c = parseString.charAt(pos);
					if (c == '\\') {
						final String command = getCommand();
						skipPureWhites();
						if (pos < len) {
							c = parseString.charAt(pos);
							if (c == '}') {
								++pos;
								return command;
							}
							throw new ParseException(this,
									"A closing '}' expected");
						}
						return command;
					}
				}
				throw new ParseException(this,
						"Not a " + (isLength ? "length" : "command") + "name");
			} else if (c == '\\') {
				return getCommand();
			}
		}
		throw new ParseException(this,
				"Not a " + (isLength ? "length" : "command") + "name");
	}

	public Color getColor(final char stop) {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == stop) {
				++pos;
				return ColorUtil.BLACK;
			}
			prevpos = pos;
			if (c == '#') {
				++pos;
				cancelPrevPos();
				return getHexColor(stop);
			} else if ((c >= '0' && c <= '9') || (c == '.') || (c == '-')
					|| (c == '+')) {
				ArrayList<DoubleOrInt> arr = new ArrayList<>(4);
				arr.add(getDoubleOrInteger());
				skipPureWhites();
				while (pos < len) {
					c = parseString.charAt(pos++);
					if (c == ',' || c == ';') {
						skipPureWhites();
						arr.add(getDoubleOrInteger());
						skipPureWhites();
					} else if (c == stop) {
						switch (arr.size()) {
						case 1:
							final DoubleOrInt n = arr.get(0);
							if (n.isdouble) {
								final int g = (int) (255. * clamp(n.f) + 0.5)
										* 0x010101;
								cancelPrevPos();
								return FactoryProvider.getInstance()
										.getGraphicsFactory().createColor(g);
							}

							cancelPrevPos();
							// we've 123456 which could be #123456
							// 123 => 3 + 2 * 16 + 1 * 16^2
							return FactoryProvider.getInstance()
									.getGraphicsFactory()
									.createColor(convertIntToHex(n.i));
						case 3:
							final DoubleOrInt R = arr.get(0);
							final DoubleOrInt G = arr.get(1);
							final DoubleOrInt B = arr.get(2);
							if (!R.isdouble && !G.isdouble && !B.isdouble) {
								final int Ri = clamp(R.i);
								final int Gi = clamp(G.i);
								final int Bi = clamp(B.i);

								cancelPrevPos();
								return FactoryProvider.getInstance()
										.getGraphicsFactory().createColor(
												(Ri << 16) | (Gi << 8) | Bi);
							}

							final int Rf = (int) (255. * clamp(R.getDouble())
									+ 0.5);
							final int Gf = (int) (255. * clamp(G.getDouble())
									+ 0.5);
							final int Bf = (int) (255. * clamp(B.getDouble())
									+ 0.5);

							cancelPrevPos();
							return FactoryProvider.getInstance()
									.getGraphicsFactory()
									.createColor((Rf << 16) | (Gf << 8) | Bf);
						case 4:
							final double C = clamp(arr.get(0).getDouble());
							final double M = clamp(arr.get(1).getDouble());
							final double Y = clamp(arr.get(2).getDouble());
							final double K = clamp(arr.get(3).getDouble());

							cancelPrevPos();
							return Colors.conv(C, M, Y, K);
						default:
							throw new ParseException(this,
									"Invalid color definition");
						}
					} else {
						// We have a number followed by a character (not a stop)
						// if the number is an int and the character is a letter
						// a-f then we have an hex number
						final DoubleOrInt n = arr.get(0);
						if (n.isdouble) {
							throw new ParseException(this,
									"Invalid character in color definition: "
											+ c);
						}

						int h = getHex(c);
						if (h == 16) {
							throw new ParseException(this,
									"Invalid character in color definition: "
											+ c);
						}
						int x = convertIntToHex(n.i) * 16 + h;
						while (pos < len) {
							c = parseString.charAt(pos);
							h = getHex(c);
							if (h == 16) {
								if (c == stop) {
									++pos;
									cancelPrevPos();
									return FactoryProvider.getInstance()
											.getGraphicsFactory()
											.createColor(x);
								}
								throw new ParseException(this,
										"Invalid character in color definition: "
												+ c);
							}
							x = (x << 4) | h;
							++pos;
						}
						cancelPrevPos();
						return FactoryProvider.getInstance()
								.getGraphicsFactory().createColor(x);
					}
				}
			}

			if (c == 'r') {
				// maybe rgb(...) or rgba(...)
				final int spos = pos;
				final Color color = getRGB(stop);
				if (color != null) {
					cancelPrevPos();
					return color;
				}
				pos = spos;
			} else if (c == 'h') {
				// maybe hsl(...) or hsla(...)
				final int spos = pos;
				final Color color = getHSL(stop);
				if (color != null) {
					cancelPrevPos();
					return color;
				}
				pos = spos;
			}

			// We don't have a number or a dot
			// So we probably have a color name
			final int spos = pos;
			final String name = getString(stop).trim();
			final Color color = Colors.getFromName(name);
			if (color == null) {
				try {
					final Color ret = ColorUtil.decode("#" + name);
					cancelPrevPos();
					return ret;
				} catch (NumberFormatException e) {
					throw new ParseException(this, "Invalid color: " + name);
				}
			}
			cancelPrevPos();
			return color;
		}
		throw new ParseException(this, "Invalid color: " + parseString);
	}

	private Color getHexColor(final char stop) {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			int acc = getHex(c);
			if (acc == 16) {
				throw new ParseException(this,
						"An hexadecimal number expected");
			}
			int ncomp = 1;
			++pos;
			while (pos < len) {
				c = parseString.charAt(pos);
				final int n = getHex(c);
				if (n == 16) {
					if (c == stop) {
						if (ncomp == 3) {
							++pos;
							// we've #RGB and we want #RRGGBB
							// RRGGBB = R0G0B << 4 | R0G0B
							final int R = acc & 0xF00;
							final int G = acc & 0x0F0;
							final int B = acc & 0x00F;
							final int R0G0B = (R << 8) | (G << 4) | B;
							return FactoryProvider.getInstance()
									.getGraphicsFactory()
									.createColor((R0G0B << 4) | R0G0B);
						} else if (ncomp == 4) {
							++pos;
							// we've #RGBA and we want #RRGGBBAA
							// AARRGGBB = 0AR0G0B << 4 | 0AR0G0B
							final int R = acc & 0xF000;
							final int G = acc & 0x0F00;
							final int B = acc & 0x00F0;
							final int A = acc & 0x000F;
							final int OAOROGOB = (A << 24) | (R << 4) | G
									| (B >> 4);
							return FactoryProvider.getInstance()
									.getGraphicsFactory().createColor(
											(OAOROGOB << 4) | OAOROGOB, true);
						}
						throw new ParseException(this,
								"An hexadecimal number #RGB or #RRGGBB expected");
					}
					throw new ParseException(this,
							"An hexadecimal number expected");
				}
				acc = (acc << 4) | n;
				if (ncomp == 5) {
					// we already have 6 digits
					++pos;
					if (pos < len) {
						c = parseString.charAt(pos);
						if (c == stop) {
							++pos;
							return FactoryProvider.getInstance()
									.getGraphicsFactory().createColor(acc);
						} else {
							// check for 8 digits and return color with alpha
							if (pos + 2 < len) {
								final int m = getHex(c);
								if (m < 16) {
									c = parseString.charAt(pos + 1);
									final int p = getHex(c);
									if (p < 16) {
										pos += 2;
										c = parseString.charAt(pos);
										if (c == stop) {
											++pos;
											acc = acc | (m << 28) | (p << 24);
											return FactoryProvider.getInstance()
													.getGraphicsFactory()
													.createColor(acc, true);
										}
									}
								}
							}
						}
					}
					throw new ParseException(this,
							"An hexadecimal number #RGB or #RRGGBB expected");
				}
				++ncomp;
				++pos;
			}
		}
		throw new ParseException(this,
				"An hexadecimal number #RGB or #RRGGBB expected");
	}

	private ArrayList<DoubleOrInt> getComponentsAsNum(final int min,
			final int max, final char stop) {
		skipPureWhites();
		final ArrayList<DoubleOrInt> arr = new ArrayList<>(max);
		int ncomp = 0;
		while (pos < len) {
			arr.add(getDoubleOrInteger());
			++ncomp;
			skipPureWhites();
			if (pos < len) {
				char c = parseString.charAt(pos);
				if (c == ',' || c == ';') {
					++pos;
					skipPureWhites();
				} else if (c == ')') {
					if (ncomp < min) {
						throw new ParseException(this,
								"Invalid number of components");
					} else {
						++pos;
						skipPureWhites();
						if (pos < len) {
							c = parseString.charAt(pos);
							if (c == stop) {
								++pos;
								break;
							} else {
								throw new ParseException(this,
										"Invalid char " + c);
							}
						}
						throw new ParseException(this,
								"A '" + stop + "' expected");
					}
				}
			}
			if (ncomp == max) {
				throw new ParseException(this, "Invalid number of components");
			}
		}
		return arr;
	}

	private Color getRGB(final char stop) {
		// gb(0,0,0).length = 9
		if (pos + 9 < len) {
			if (parseString.charAt(pos + 1) == 'g'
					&& parseString.charAt(pos + 2) == 'b') {
				final boolean rgba = parseString.charAt(pos + 3) == 'a';
				final int ncomp = rgba ? 4 : 3;
				pos += ncomp;
				skipPureWhites();
				if (pos < len) {
					final char c = parseString.charAt(pos);
					if (c == '(') {
						++pos;
						final ArrayList<DoubleOrInt> arr = getComponentsAsNum(
								ncomp, ncomp, stop);
						final DoubleOrInt R = arr.get(0);
						final DoubleOrInt G = arr.get(1);
						final DoubleOrInt B = arr.get(2);
						int RGB;
						if (!R.isdouble && !G.isdouble && !B.isdouble) {
							final int Ri = clamp(R.i);
							final int Gi = clamp(G.i);
							final int Bi = clamp(B.i);
							RGB = (Ri << 16) | (Gi << 8) | Bi;
						} else {
							final int Rf = (int) (255. * clamp(R.getDouble())
									+ 0.5);
							final int Gf = (int) (255. * clamp(G.getDouble())
									+ 0.5);
							final int Bf = (int) (255. * clamp(B.getDouble())
									+ 0.5);
							RGB = (Rf << 16) | (Gf << 8) | Bf;
						}

						if (rgba) {
							final int A = (int) (255.
									* clamp(arr.get(3).getDouble()) + 0.5);
							return FactoryProvider.getInstance()
									.getGraphicsFactory()
									.createColor((A << 24) | RGB, true);
						} else {
							return FactoryProvider.getInstance()
									.getGraphicsFactory().createColor(RGB);
						}
					}
				}
			}
		}
		return null;
	}

	private Color getHSL(final char stop) {
		// sl(0,0,0).length = 9
		if (pos + 9 < len) {
			if (parseString.charAt(pos + 1) == 's'
					&& parseString.charAt(pos + 2) == 'l') {
				final boolean hsla = parseString.charAt(pos + 3) == 'a';
				final int ncomp = hsla ? 4 : 3;
				pos += ncomp;
				skipPureWhites();
				if (pos < len) {
					final char c = parseString.charAt(pos);
					if (c == '(') {
						++pos;
						final ArrayList<DoubleOrInt> arr = getComponentsAsNum(
								ncomp, ncomp, stop);
						final double H = arr.get(0).getDouble();
						final double S = clamp(arr.get(1).getDouble());
						final double L = clamp(arr.get(2).getDouble());

						if (ncomp == 3) {
							return Colors.convHSL(H, S, L);
						} else {
							final double A = clamp(arr.get(3).getDouble());
							return Colors.convHSL(H, S, L, A);
						}
					}
				}
			}
		}
		return null;
	}

	public Color getArgAsColor() {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				return getColor('}');
			}
		}
		throw new ParseException(this, "A color expected as argument");
	}

	public Color getOptionAsColor() {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '[') {
				++pos;
				return getColor(']');
			}
		}
		return null;
	}

	/**
	 * Convert an integer like 1234 into 0x1234 0x1234 = 4 + 3*16 + 2 * 16^2 + 1
	 * * 16^3
	 */
	public static int convertIntToHex(int x) {
		int p = 16;
		int acc = x % 10;
		while (x > 10) {
			x /= 10;
			acc += p * (x % 10);
			p <<= 4;
		}

		return acc;
	}

	private static int clamp(final int n) {
		return Math.min(255, Math.max(n, 0));
	}

	private static double clamp(final double n) {
		return Math.min(1., Math.max(n, 0.));
	}

	private static int getHex(final char c) {
		return (c <= 'f') ? HEX_ARRAY[c] : 16;
	}

	private int getMacroArgNumber() {
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c >= '1' && c <= '9') {
				++pos;
				int acc = c - '0';
				while (pos < len) {
					c = parseString.charAt(pos);
					if (c >= '0' && c <= '9') {
						++pos;
						acc = 10 * acc + c - '0';
					} else {
						break;
					}
				}
				return acc;
			}
		}
		return 0;
	}

	public void processDollar() {
		AtomConsumer com;
		if (pos < len) {
			final char cc = parseString.charAt(pos);
			if (cc == '$') {
				++pos;
				com = Commands.getDollarDollar();
			} else {
				com = Commands.getDollar();
			}
		} else {
			com = Commands.getDollar();
		}
		if (com.init(this)) {
			addConsumer(com);
		}
	}

	public void processCommand(final String command) throws ParseException {
		// We must begin with Commands because some commands overwrite
		// a symbol (e.g. \int overwrites int symbol)
		if (!Commands.exec(this, command) && !SymbolAtom.put(this, command)
				&& !NewCommandMacro.exec(this, command)
				&& !JLMPackage.exec(this, command)) {
			if (command.length() == 1) {
				// UnicodeMapping.get(command.charAt(0));
				if (SymbolAtom.put(this, command)) {
					return;
				}
			} else if (setLength(command)) {
				return;
			}

			throw new ParseException(this, "Unknown command: " + command,
					"\\backslash{" + command + "}");
		}
	}

	public static List<String> getAllCommands() {
		// AlphabetManager.get().addBlock(Character.UnicodeBlock.CYRILLIC);
		final List<String> all = new ArrayList<String>();
		Commands.getAll(all);
		SymbolAtom.getAll(all);

		return all;
	}

	public void processSubSup(final char f) throws ParseException {
		if (!stack.isEmpty()) {
			final AtomConsumer ac = stack.peek();
			if (ac instanceof SubSupCom) {
				((SubSupCom) ac).setState(this, f);
				return;
			}
		}
		final SubSupCom ssc = new SubSupCom(f);
		ssc.init(this);
		addConsumer(ssc);
	}

	public boolean isWaitingForSup() {
		if (!stack.isEmpty()) {
			final AtomConsumer ac = stack.peek();
			if (ac instanceof SubSupCom) {
				final SubSupCom ssc = (SubSupCom) ac;
				return ssc.isWaitingForSup();
			} else if (ac instanceof GroupConsumer) {
				stack.pop();
				final AtomConsumer ac1 = stack.peek();
				boolean r = false;
				if (ac1 instanceof SubSupCom) {
					final SubSupCom ssc = (SubSupCom) ac1;
					r = ssc.isWaitingForSup();
				}
				stack.push(ac);
				return r;
			}
		}
		return false;
	}

	public void processLBrace() {
		if (isHandlingArg()) {
			stack.peek().lbrace(this);
		} else {
			addConsumer(new GroupConsumer(TeXConstants.Opener.LBRACE));
		}
	}

	public void processRBrace() {
		close();
		if (stack.isEmpty()) {
			throw new ParseException(this,
					"Closing '}' doesn't match any opening '{'");
		} else if (isHandlingArg()) {
			stack.peek().rbrace(this);
		} else {
			final AtomConsumer ac = stack.peek();
			if (!(ac instanceof GroupConsumer)) {
				throw new ParseException(this,
						"Closing '}' doesn't match any opening '{'");
			} else if (!((GroupConsumer) ac).close(this,
					TeXConstants.Opener.LBRACE)) {
				throw new ParseException(this,
						"Closing '}' is not matching an opening '{'");
			}
		}
	}

	public boolean hasGroupConsumer(final TeXConstants.Opener opener) {
		final AtomConsumer ac = stack.peek();
		if ((ac instanceof GroupConsumer)) {
			final GroupConsumer gc = (GroupConsumer) ac;
			if (gc.getOpener() == opener) {
				return true;
			}
		}
		return false;
	}

	public boolean hasGroupConsumer() {
		return stack.peek() instanceof GroupConsumer;
	}

	private GroupConsumer getGroupConsumerOption() {
		for (AtomConsumer ac : stack) {
			if ((ac instanceof GroupConsumer)) {
				final GroupConsumer gc = (GroupConsumer) ac;
				if (gc.getOpener() == TeXConstants.Opener.LSQBRACKET) {
					return gc;
				}
				return null;
			}
			if (!ac.isClosable()) {
				return null;
			}
		}
		return null;
	}

	public boolean processRSqBracket() {
		if (!stack.isEmpty()) {
			final GroupConsumer gc = getGroupConsumerOption();
			if (gc == null) {
				return false;
			}
			close();
			gc.close(this, TeXConstants.Opener.LSQBRACKET);
			return true;
		}
		return false;
	}

	public boolean isColumn() {
		if (stack.isEmpty()) {
			return false;
		}
		return stack.peek() instanceof Column;
	}

	public void close() {
		// Some consumers are waiting for data (like SubSupCom)
		// and when we encounter for example a '}' must close it
		// x_2} => SubSupCom is waiting for a '^' but we've a '}'
		// or { x \over y_2} => close the y_ and close the \over
		if (!stack.isEmpty()) {
			AtomConsumer ac = stack.peek();
			while (ac.close(this) && !stack.isEmpty()) {
				ac = stack.peek();
			}
		}
	}

	private Atom flush() {
		close();
		while (!stack.isEmpty()) {
			AtomConsumer ac = stack.pop();
			if (ac.close(this)) {
				ac = stack.pop();
			}
			if (ac instanceof GroupConsumer) {
				final Atom a = ((GroupConsumer) ac).getAtom();
				if (stack.isEmpty()) {
					return a;
				} else {
					final int size = stack.size();
					// make an attempt to push the atom in the top consumer
					addToConsumer(a);
					final int nsize = stack.size();

					if (nsize > size) {
						// oups ! the stack size increased
						// we remove the added elements and the top consumer
						// since
						// this one is not able to close itself...
						for (int i = 0; i < nsize - size + 1; ++i) {
							stack.pop();
						}
					}
				}
			}
		}
		return EmptyAtom.get();
	}

	public Env.Begin getBegin() {
		// when we've a \begin{foo}, a begin is pushed on the stack and
		// maybe followed by other consumers (added in the pre-stuff)
		// which will be closed by post-stuff so when we've a \end{foo}
		// we must get the first matching environment.
		for (AtomConsumer ac : stack) {
			if (ac instanceof Env.Begin) {
				return (Env.Begin) ac;
			}
		}
		return null;
	}

	/**
	 * Get the contents between two delimiters
	 *
	 * @param open
	 *            the opening character
	 * @param close
	 *            the closing character
	 * @return the enclosed contents
	 * @throws ParseException
	 *             if the contents are badly enclosed
	 */
	public String getGroup(final char open, final char close) {
		final int spos = pos;
		int group = 1;

		while (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == open) {
				++group;
				++pos;
			} else if (c == close) {
				if (group == 1) {
					return parseString.substring(spos, pos);
				}
				++pos;
				--group;
			} else if (c == '\\') {
				pos += 2;
			} else {
				++pos;
			}
		}

		throw new ParseException(this, "missing '" + close + "'!");
	}

	/**
	 * Get the contents between double quotes
	 *
	 * @param open
	 *            the opening character
	 * @param close
	 *            the closing character
	 * @return the enclosed contents
	 * @throws ParseException
	 *             if the contents are badly enclosed
	 */
	public String getPureString() {
		final int spos = pos;

		while (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '\"') {
				return parseString.substring(spos, pos++);
			} else if (c == '\\') {
				pos += 2;
			} else {
				++pos;
			}
		}

		throw new ParseException(this, "missing \" !");
	}

	public String getGroupAsArgument() {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				final String s = getGroup('{', '}');
				++pos;
				return s;
			}
		}
		throw new ParseException(this, "Expect a '{'");
	}

	public String getOptionAsCode() {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '[') {
				++pos;
				final String s = getGroup('[', ']');
				++pos;
				return s;
			}
		}
		throw new ParseException(this, "Expect a '['");
	}

	public ArrayList<String> getArgsAsStrings(final int nargs) {
		final ArrayList<String> args = new ArrayList<String>(nargs);
		for (int i = 0; i < nargs; ++i) {
			skipPureWhites();
			if (pos < len) {
				final char c = parseString.charAt(pos);
				if (c == '{') {
					++pos;
					args.add(getGroup('{', '}'));
					++pos;
				} else if (c == '\\') {
					args.add(getCommandWithBackslash());
				} else {
					args.add(Character.toString(c));
					++pos;
				}
			}
		}
		if (args.size() != nargs) {
			throw new ParseException(this,
					"Invalid number of arguments: " + nargs + " expected");
		}
		return args;
	}

	public TeXLength getArgAsLength() {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				final TeXLength l = getLength();
				if (pos < len) {
					c = parseString.charAt(pos);
					if (c == '}') {
						++pos;
						return Double.isNaN(l.getL()) ? TeXLength.getZero() : l;
					}
					throw new ParseException(this, "A closing '}' expected");
				}
			} else if ((c >= '0' && c <= '9') || (c == '.') || (c == '-')
					|| (c == '+')) {
				return getLength();
			}
		}
		throw new ParseException(this, "A length expected");
	}

	public TeXLength getArgAsLengthOrExcl() {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			TeXLength l = null;
			if (c == '{') {
				++pos;
				if (pos < len) {
					c = parseString.charAt(pos);
					if (c == '!') {
						++pos;
						if (pos < len) {
							c = parseString.charAt(pos);
							if (c != '}') {
								throw new ParseException(this,
										"A closing '}' expected");
							}
							++pos;
							return null;
						}
						throw new ParseException(this,
								"A closing '}' expected");
					} else {
						l = getLength();
						if (pos < len) {
							c = parseString.charAt(pos);
							if (c != '}') {
								throw new ParseException(this,
										"A closing '}' expected");
							}
							++pos;
						}
					}
				}
			} else if ((c >= '0' && c <= '9') || (c == '.') || (c == '-')
					|| (c == '+')) {
				l = getLength();
			}
			if (l != null) {
				return l;
			}
		}
		throw new ParseException(this, "A length expected");
	}

	public TeXLength getOptionAsLength(TeXLength def) {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == '[') {
				++pos;
				final TeXLength l = getLength();
				if (Double.isNaN(l.getL())) {
					if (pos < len) {
						c = parseString.charAt(pos);
						if (c == ']') {
							++pos;
							return def;
						}
					}
					throw new ParseException(this, "A closing ']' expected");
				}
				if (pos < len) {
					c = parseString.charAt(pos);
					if (c == ']') {
						++pos;
						return l;
					}
					throw new ParseException(this, "A closing ']' expected");
				}
			}
		}
		return def;
	}

	public char getOptionAsChar() {
		return getAsChar('[', ']');
	}

	public char getArgAsChar() {
		return getAsChar('{', '}');
	}

	public char getAsChar(final char open, final char close) {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == open) {
				++pos;
				if (pos < len) {
					final char r = parseString.charAt(pos++);
					if (pos < len) {
						c = parseString.charAt(pos);
						if (c == close) {
							++pos;
							return r;
						}
						throw new ParseException(this,
								"A closing '" + close + "' expected");
					}
				}
				throw new ParseException(this,
						"A closing '" + close + "' expected");
			}
		}
		return '\0';
	}

	public boolean setLength(final String com) {
		if (TeXLength.isLengthName(com)) {
			skipPureWhites();
			if (pos < len) {
				final char c = parseString.charAt(pos);
				if (c == '=') {
					++pos;
					skipPureWhites();
					final TeXLength l = getLength(TeXLength.Unit.NONE);
					TeXLength.setLength(com, l);
					cancelPrevPos();
					return true;
				}
			}
		}
		return false;
	}

	public TeXLength getLength() {
		return getLength(TeXLength.Unit.PT);
	}

	public TeXLength getLength(final TeXLength.Unit def) {
		// TODO: handle case where length is \foo (ie no number before)
		final double x = getDecimal();
		skipPureWhites();
		if (pos + 1 < len) {
			final char c = parseString.charAt(pos++);
			char cn = parseString.charAt(pos++);
			switch (c) {
			case '\\':
				final int spos = pos;
				final String name = getCommand();
				final TeXLength l = TeXLength.getLength(name, x);
				if (l != null) {
					return l;
				}
				pos = spos;
				return new TeXLength(def, x);
			case 'b':
				if (cn == 'p') { // bp
					return new TeXLength(TeXLength.Unit.POINT, x);
				}
				break;
			case 'c':
				if (cn == 'c') { // cc
					return new TeXLength(TeXLength.Unit.CC, x);
				}
				if (cn == 'm') { // cm
					return new TeXLength(TeXLength.Unit.CM, x);
				}
				break;
			case 'd':
				if (cn == 'd') { // dd
					return new TeXLength(TeXLength.Unit.DD, x);
				}
				break;
			case 'e':
				if (cn == 'm') { // em
					return new TeXLength(TeXLength.Unit.EM, x);
				}
				if (cn == 'x') { // ex
					return new TeXLength(TeXLength.Unit.EX, x);
				}
				break;
			case 'i':
				if (cn == 'n') { // in
					return new TeXLength(TeXLength.Unit.IN, x);
				}
				break;
			case 'm':
				if (cn == 'u') { // mu
					return new TeXLength(TeXLength.Unit.MU, x);
				}
				if (cn == 'm') { // mm
					return new TeXLength(TeXLength.Unit.MM, x);
				}
				break;
			case 'p':
				if (cn == 'c') { // pc
					return new TeXLength(TeXLength.Unit.PICA, x);
				}
				if (cn == 't') { // pt
					return new TeXLength(TeXLength.Unit.PT, x);
				}
				if (cn == 'x') { // px
					return new TeXLength(TeXLength.Unit.PIXEL, x);
				}
				if (cn == 'i') {
					if (pos + 1 < len) {
						cn = parseString.charAt(pos);
						if (cn == 'c' && parseString.charAt(pos + 1) == 'a') { // pica
							pos += 2;
							return new TeXLength(TeXLength.Unit.PICA, x);
						} else if (cn == 'x') { // pixel
							if (pos + 2 < len
									&& parseString.charAt(pos + 1) == 'e'
									&& parseString.charAt(pos + 2) == 'l') {
								pos += 3;
								return new TeXLength(TeXLength.Unit.PIXEL, x);
							}
							++pos;
							return new TeXLength(TeXLength.Unit.PIXEL, x);
						}
					}
				}
				break;
			case 's':
				if (cn == 'p') { // sp
					return new TeXLength(TeXLength.Unit.SP, x);
				}
				break;
			}
			pos -= 2;
		}
		return new TeXLength(def, x);
	}

	private DoubleOrInt getDoubleOrInteger() {
		char c = parseString.charAt(pos);
		final boolean negative = c == '-';
		if (negative || c == '+') {
			++pos;
		}

		if (pos < len) {
			c = parseString.charAt(pos);
			if (c >= '0' && c <= '9') {
				int intPart = c - '0';
				++pos;
				while (pos < len) {
					c = parseString.charAt(pos);
					if (c >= '0' && c <= '9') {
						intPart = 10 * intPart + c - '0';
					} else {
						if (c == '.') {
							++pos;
							double x = intPart + getDecimalPart();
							if (pos < len) {
								c = parseString.charAt(pos);
								if (c == '%') {
									++pos;
									x /= 100.;
								}
							}
							return new DoubleOrInt(negative ? -x : x);
						} else if (c == '%') {
							++pos;
							final double x = (intPart) / 100.;
							return new DoubleOrInt(negative ? -x : x);
						} else {
							return new DoubleOrInt(
									negative ? -intPart : intPart);
						}
					}
					++pos;
				}
				return new DoubleOrInt(negative ? -intPart : intPart);
			} else if (c == '.') {
				++pos;
				double x = getDecimalPart();
				if (pos < len) {
					c = parseString.charAt(pos);
					if (c == '%') {
						++pos;
						x /= 100.;
					}
				}
				return new DoubleOrInt(negative ? -x : x);
			}
		}

		throw new ParseException(this, "Not a valid number");
	}

	public double getOptionAsDecimal() {
		return getOptionAsDecimal(Double.NaN);
	}

	public double getOptionAsDecimal(final double def) {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == '[') {
				++pos;
				skipPureWhites();
				double x = getDecimal();
				if (pos < len) {
					c = parseString.charAt(pos);
					if (c == '%') {
						x /= 100.;
						++pos;
						if (pos < len) {
							c = parseString.charAt(pos);
						} else {
							throw new ParseException(this,
									"A closing ']' expected");
						}
					}
					if (isWhite(c)) {
						++pos;
						skipPureWhites();
						c = parseString.charAt(pos);
					}
					if (c != ']') {
						throw new ParseException(this,
								"A closing ']' expected");
					}
					++pos;
					return x;
				}
				throw new ParseException(this, "A closing ']' expected");
			}
		}
		return def;
	}

	public double getArgAsDecimal() {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				skipPureWhites();
				double x = getDecimal();
				if (pos < len) {
					c = parseString.charAt(pos);
					if (c == '%') {
						x /= 100.;
						++pos;
						if (pos < len) {
							c = parseString.charAt(pos);
						} else {
							throw new ParseException(this,
									"A closing '}' expected");
						}
					}
					if (isWhite(c)) {
						++pos;
						skipPureWhites();
						c = parseString.charAt(pos);
					}
					if (c != '}') {
						throw new ParseException(this,
								"A closing '}' expected");
					}
					++pos;
					return x;
				}
				throw new ParseException(this, "A closing '}' expected");
			} else if ((c >= '0' && c <= '9') || (c == '.') || (c == '-')
					|| (c == '+')) {
				return getDecimal();
			}
		}
		throw new ParseException(this, "A number expected");
	}

	public void getArgAsDecimals(final double[] res, final int resLen) {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == '{') {
				for (int i = 0; i < resLen; ++i) {
					++pos;
					skipPureWhites();
					res[i] = getDecimal();
					if (pos < len) {
						if (parseString.charAt(pos) == '%') {
							++pos;
							res[i] /= 100.;
						}
					} else {
						throw new ParseException(this, "Expect a \'}\'");
					}
					skipPureWhites();
					if (pos < len) {
						c = parseString.charAt(pos);
						if (c == '}') {
							++pos;
							if (i != resLen - 1) {
								throw new ParseException(this,
										"Expect " + res.length + " numbers");
							}
							return;
						} else if (c != ',' && c != ';') {
							throw new ParseException(this,
									"Invalid character \'" + c
											+ "\' in list of numbers: expect a \',\' or \';\'");
						}
					}
				}
				throw new ParseException(this, "Expect a \'}\'");
			}
		}
		throw new ParseException(this, "Expect a \'{\'");
	}

	public void getArgAsPositiveIntegers(final int[] res, final int resLen) {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == '{') {
				for (int i = 0; i < resLen; ++i) {
					++pos;
					skipPureWhites();
					res[i] = getPositiveInteger();
					skipPureWhites();
					if (pos < len) {
						c = parseString.charAt(pos);
						if (c == '}') {
							++pos;
							if (i != resLen - 1) {
								throw new ParseException(this,
										"Expect " + res.length + " numbers");
							}
							return;
						} else if (c != ',' && c != ';') {
							throw new ParseException(this,
									"Invalid character \'" + c
											+ "\' in list of numbers: expect a \',\' or \';\'");
						}
					}
					throw new ParseException(this, "Expect a \'}\'");
				}
				return;
			}
		}
		throw new ParseException(this, "Expect a \'{\'");
	}

	public int getArgAsHexNumber(final int numLen) {
		skipPureWhites();
		if (pos < len) {
			char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				skipPureWhites();
				if (pos < len) {
					c = parseString.charAt(pos);
					int acc = getHex(c);
					if (acc == 16) {
						throw new ParseException(this,
								"An hexadecimal number expected");
					}
					for (int i = 1; i < numLen; ++i) {
						++pos;
						if (pos < len) {
							c = parseString.charAt(pos);
							final int n = getHex(c);
							if (n == 16) {
								throw new ParseException(this,
										"An hexadecimal number expected");
							}
							acc = (acc << 4) | n;
						}
					}
					++pos;
					skipPureWhites();
					if (pos < len) {
						c = parseString.charAt(pos);
						if (c == '}') {
							++pos;
							return acc;
						}
					}
					throw new ParseException(this, "Expect a \'}\'");
				}
				throw new ParseException(this, "Expect a hex number");
			}
		}
		throw new ParseException(this, "Expect a \'{\'");
	}

	public double getDecimal() {
		return getDecimal(Double.NaN);
	}

	public double getDecimal(double def) {
		char c = parseString.charAt(pos);
		final boolean negative = c == '-';
		if (negative || c == '+') {
			++pos;
		}

		if (pos < len) {
			c = parseString.charAt(pos);
			if (c >= '0' && c <= '9') {
				int intPart = c - '0';
				++pos;
				while (pos < len) {
					c = parseString.charAt(pos);
					if (c >= '0' && c <= '9') {
						intPart = 10 * intPart + c - '0';
					} else {
						double x = intPart;
						if (c == '.') {
							++pos;
							x += getDecimalPart();
						}
						return negative ? -x : x;
					}
					++pos;
				}
				final double x = intPart;
				return negative ? -x : x;
			} else if (c == '.') {
				++pos;
				final double x = getDecimalPart();
				return negative ? -x : x;
			}
		}
		return def;
	}

	private void skipNumbers() {
		while (pos < len) {
			final char c = parseString.charAt(pos);
			if (c < '0' || c > '9') {
				return;
			}
			++pos;
		}
		return;
	}

	private double getDecimalPart() {
		if (pos < len) {
			char c = parseString.charAt(pos++);
			if (c >= '0' && c <= '9') {
				int ndec = 1;
				int decPart = c - '0';
				while (pos < len) {
					c = parseString.charAt(pos);
					if (c >= '0' && c <= '9') {
						++ndec;
						decPart = 10 * decPart + c - '0';
						++pos;
						if (ndec == MAX_DEC) {
							skipNumbers();
							return decPart / POWTEN[MAX_DEC];
						}
					} else {
						break;
					}
				}
				return decPart / POWTEN[ndec];
			}
		}
		return 0.;
	}

	public void convertASCIIChar(final char c, final boolean oneChar) {
		addToConsumer(convertASCIICharToAtom(c, oneChar));
	}

	public Atom convertASCIICharToAtom(final char c, final boolean oneChar) {
		final ExternalFontManager.FontSSSF f = ExternalFontManager.get()
				.getBasicLatinFont();
		if (f == null) {
			return new CharAtom(c, isMathMode());
		} else {
			String r;
			if (oneChar) {
				r = Character.toString(c);
			} else {
				final int start = pos - 1;
				while (pos < len) {
					final char cc = parseString.charAt(pos);
					if ((cc < '0' || cc > '9') && !isRomanLetter(cc)) {
						break;
					}
					++pos;
				}
				r = parseString.substring(start, pos);
			}
			return new JavaFontRenderingAtom(r, f);
		}
	}

	/**
	 * Convert a character in the corresponding atom in using the file
	 * TeXFormulaSettings.xml for non-alphanumeric characters
	 *
	 * @param c
	 *            the character to be converted
	 * @return the corresponding atom
	 * @throws ParseException
	 *             if the character is unknown
	 */
	public void convertCharacter(char c, final boolean oneChar)
			throws ParseException {
		if (!charMapping.replace(c, this)) {
			final Character.UnicodeBlock block = UnicodeMapping.get(c);
			String r;
			if (oneChar) {
				r = Character.toString(c);
			} else {
				final int start = pos - 1;
				while (pos < len) {
					c = parseString.charAt(pos);
					if ((c != ' ' && !UnicodeMapping.get(c).equals(block))
							|| charMapping.hasMapping(c)) {
						break;
					}
					++pos;
				}
				r = parseString.substring(start, pos);
			}

			final ExternalFontManager.FontSSSF f = ExternalFontManager.get()
					.getExternalFont(block);
			addToConsumer(new JavaFontRenderingAtom(r, f));
		}
	}

	public Atom getAtomFromUnicode(char c, final boolean oneChar) {
		if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')
				|| (c >= 'A' && c <= 'Z')) {
			return convertASCIICharToAtom(c, oneChar);
		}
		Atom a = charMapping.getAtom(c, isMathMode());
		if (a == null) {
			final Character.UnicodeBlock block = UnicodeMapping.get(c);
			a = charMapping.getAtom(c, isMathMode());
			if (a == null) {
				String r;
				if (oneChar) {
					r = Character.toString(c);
				} else {
					final int start = pos - 1;
					while (pos < len) {
						c = parseString.charAt(pos);
						if ((c != ' ' && !UnicodeMapping.get(c).equals(block))
								|| charMapping.hasMapping(c)) {
							break;
						}
						++pos;
					}
					r = parseString.substring(start, pos);
				}
				final ExternalFontManager.FontSSSF f = ExternalFontManager.get()
						.getExternalFont(block);
				a = new JavaFontRenderingAtom(r, f);
			}
		}
		return a;
	}

	public ArrayOptions getArrayOptions() {
		skipPureWhites();
		if (pos < len) {
			final char c = parseString.charAt(pos);
			if (c == '{') {
				++pos;
				final ArrayOptions options = new ArrayOptions();
				parseArrayOptions(options);
				options.close();
				return options;
			}
		}
		return ArrayOptions.getEmpty();
	}

	private void parseArrayOptions(final ArrayOptions options) {
		// https://en.wikibooks.org/wiki/LaTeX/Tables
		while (pos < len) {
			final char c = parseString.charAt(pos);
			switch (c) {
			case 'c':
				++pos;
				options.addAlignment(TeXConstants.Align.CENTER);
				break;
			case 'l':
				++pos;
				options.addAlignment(TeXConstants.Align.LEFT);
				break;
			case 'r':
				++pos;
				options.addAlignment(TeXConstants.Align.RIGHT);
				break;
			case '|':
				options.addVline(getNumberOf('|'));
				break;
			case '@':
				// TODO: handle that in ArrayAtom
				// @{\pi} \pi is the column separator
				++pos;
				final String code = getGroupAsArgument();
				final SingleAtomConsumer cons = new SingleAtomConsumer();
				addConsumer(cons);
				addString(code, true /*
										 * to come back here after the code has
										 * been parsed
										 */);
				parse();
				pop(); // remove cons from the stack
				final Atom sep = cons.get();
				options.addSeparator(sep);
				break;
			case '*':
				// *{num}{str}
				// *{3}{c|} <=> c|c|c|
				++pos;
				final int num = getArgAsPositiveInteger();
				final String str = getGroupAsArgument();
				final StringBuffer buf = new StringBuffer(str.length() * num);
				for (int i = 0; i < num; ++i) {
					buf.append(str);
				}
				addString(buf.toString());
				parseArrayOptions(options);
				popString();
				break;
			case ' ':
			case '\t':
				++pos;
				break;
			case '}':
				++pos;
				return;
			default:
				++pos;
				options.addAlignment(TeXConstants.Align.CENTER);
			}
		}
	}
}