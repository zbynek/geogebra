package com.himamis.retex.renderer.share.commands;

import com.himamis.retex.renderer.share.Atom;
import com.himamis.retex.renderer.share.TeXParser;

public class CommandTFrac extends Command2A {

	@Override
	public Atom newI(TeXParser tp, Atom a, Atom b) {
		return CommandGenfrac.get(null, a, b, null, null, 1);
	}

	@Override
	public Command duplicate() {
		CommandTFrac ret = new CommandTFrac();
		ret.atom = atom;
		return ret;
	}

}
