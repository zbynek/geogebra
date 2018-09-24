package com.himamis.retex.renderer.share.commands;

import com.himamis.retex.renderer.share.Atom;
import com.himamis.retex.renderer.share.TeXConstants;
import com.himamis.retex.renderer.share.TeXLength;
import com.himamis.retex.renderer.share.TeXParser;
import com.himamis.retex.renderer.share.UnderOverAtom;

public class CommandOverSet extends Command2A {

	@Override
	public Atom newI(TeXParser tp, Atom a, Atom b) {
		final Atom at = new UnderOverAtom(b, a,
				new TeXLength(TeXLength.Unit.MU, 2.5), true, true);
		return at.changeType(TeXConstants.TYPE_RELATION);
	}

	@Override
	public Command duplicate() {
		CommandOverSet ret = new CommandOverSet();
		ret.atom = atom;
		return ret;
	}

}
