package com.himamis.retex.renderer.share.commands;

import com.himamis.retex.renderer.share.Atom;
import com.himamis.retex.renderer.share.MonoScaleAtom;
import com.himamis.retex.renderer.share.RowAtom;
import com.himamis.retex.renderer.share.TeXParser;

public class CommandFootnoteSize extends CommandStyle {

	public CommandFootnoteSize() {
		//
	}

	public CommandFootnoteSize(RowAtom size) {
		this.size = size;
	}

	@Override
	public Atom newI(TeXParser tp, Atom a) {
		return new MonoScaleAtom(a, 0.8);
	}

	@Override
	public Command duplicate() {
		return new CommandFootnoteSize(size);
	}

}
