package org.geogebra.common.kernel.commands;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.algos.AlgoBinomial;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumberValue;

/**
 * Binomial[ <Number>, <Number> ] Michael Borcherds 2008-04-12
 */
public class CmdBinomial extends CmdTwoNumFunction {
	/**
	 * Create new command processor
	 * 
	 * @param kernel
	 *            kernel
	 */
	public CmdBinomial(Kernel kernel) {
		super(kernel);
	}

	@Override
	final protected GeoElement doCommand(String a, GeoNumberValue b,
			GeoNumberValue c) {
		AlgoBinomial algo = new AlgoBinomial(cons, a, b, c);
		return algo.getResult();
	}
}
