package com.himamis.retex.renderer.share.commands;

import com.himamis.retex.renderer.share.Atom;
import com.himamis.retex.renderer.share.ItAtom;
import com.himamis.retex.renderer.share.TeXParser;

public class CommandMathIt extends Command1A {

	@Override
	public Atom newI(TeXParser tp, Atom a) {
		return new ItAtom(a);
	}

	@Override
	public Command duplicate() {
		return new CommandMathIt();
	}

}
