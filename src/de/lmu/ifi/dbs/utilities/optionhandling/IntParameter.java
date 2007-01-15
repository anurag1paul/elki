package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying an integer value.
 */
public class IntParameter extends NumberParameter<Integer,Number> {

	/**
	 * Constructs an integer parameter with the given name and description.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 */
	public IntParameter(String name, String description) {
		super(name, description);

	}
	
	/**
	 * Constructs an integer parameter with the given name, description, and parameter constraint.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param constraint the constraint for this integer parameter
	 */
	public IntParameter(String name, String description, ParameterConstraint<Number> constraint) {
		this(name, description);
		addConstraint(constraint);
	}

	/**
	 * Constructs an integer parameter with the given name, description, and list of parameter constraints.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param constraints a list of parameter constraints for this integer parameter
	 */
	public IntParameter(String name, String description, List<ParameterConstraint<Number>> constraints) {
		this(name, description);
		addConstraintList(constraints);
	}

	@Override
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {
			this.value = Integer.parseInt(value);
		}
	}

	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
	 */
	public boolean isValid(String value) throws ParameterException {

		try {
			Integer.parseInt(value);

		} catch (NumberFormatException e) {
			throw new WrongParameterValueException("Wrong parameter format! Parameter \""
					+ getName() + "\" requires an integer value! (given value: "+value+")\n");
		}

		try {
			for (ParameterConstraint<Number> cons : this.constraints) {
				cons.test(Integer.parseInt(value));
			}
		} catch (ParameterException ex) {
			throw new WrongParameterValueException("Specified parameter value for parameter \""
					+ getName() + "\" breaches parameter constraint!\n" + ex.getMessage());
		}
		return true;
	}

}
