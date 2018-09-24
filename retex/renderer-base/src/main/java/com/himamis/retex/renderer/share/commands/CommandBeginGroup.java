package com.himamis.retex.renderer.share.commands;

import com.himamis.retex.renderer.share.TeXParser;

public class CommandBeginGroup extends Command {

	public boolean init(TeXParser tp) {
		tp.processLBrace();
		return false;
	}

	@Override
	public Command duplicate() {
		return new CommandBeginGroup();
	}

}
