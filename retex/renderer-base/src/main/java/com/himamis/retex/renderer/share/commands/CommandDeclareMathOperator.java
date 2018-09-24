package com.himamis.retex.renderer.share.commands;

import com.himamis.retex.renderer.share.NewCommandMacro;
import com.himamis.retex.renderer.share.TeXParser;

public class CommandDeclareMathOperator extends Command {

	public boolean init(TeXParser tp) {
		final String name = tp.getArgAsCommand();
		final String base = tp.getGroupAsArgument();
		final String code = "\\mathop{\\mathrm{" + base + "}}\\nolimits";
		NewCommandMacro.addNewCommand(tp, name, code, 0, false);
		return false;
	}

	@Override
	public Command duplicate() {
		return new CommandDeclareMathOperator();
	}

}
