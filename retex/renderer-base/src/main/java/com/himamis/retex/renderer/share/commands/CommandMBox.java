package com.himamis.retex.renderer.share.commands;

import com.himamis.retex.renderer.share.Atom;
import com.himamis.retex.renderer.share.RomanAtom;
import com.himamis.retex.renderer.share.StyleAtom;
import com.himamis.retex.renderer.share.TeXConstants;
import com.himamis.retex.renderer.share.TeXParser;
import com.himamis.retex.renderer.share.TextStyle;
import com.himamis.retex.renderer.share.TextStyleAtom;

public class CommandMBox extends Command {

	boolean mode;

	public boolean init(TeXParser tp) {
		mode = tp.setTextMode();
		return true;
	}

	public void add(TeXParser tp, Atom a) {
		tp.setMathMode(mode);
		a = new TextStyleAtom(a, TextStyle.MATHNORMAL);
		tp.closeConsumer(
				new StyleAtom(TeXConstants.STYLE_TEXT, new RomanAtom(a)));
	}

	@Override
	public Command duplicate() {
		CommandMBox ret = new CommandMBox();
		ret.mode = mode;
		return ret;
	}

}
