package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.security.InvalidParameterException;
import java.util.List;
import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Abstract class for specifying a parameter.
 * 
 * A parameter is defined as an option having a specific value.
 * 
 * See the {@link de.lmu.ifi.dbs.elki.utilities.optionhandling} package for
 * documentation!
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * 
 * @apiviz.composedOf OptionID
 * @apiviz.uses ParameterConstraint
 * 
 * @param <T> the type of a possible value (i.e., the type of the option)
 * @param <S> the supertype for constraints
 */
public abstract class AbstractParameter<S, T extends S> implements Parameter<S, T> {
  /**
   * The default value of the parameter (may be null).
   */
  protected T defaultValue = null;

  /**
   * Specifies if the default value of this parameter was taken as parameter
   * value.
   */
  private boolean defaultValueTaken = false;

  /**
   * Specifies if this parameter is an optional parameter.
   */
  protected boolean optionalParameter = false;

  /**
   * Holds parameter constraints for this parameter.
   */
  protected final List<ParameterConstraint<S>> constraints;

  /**
   * The option name.
   */
  protected final OptionID optionid;

  /**
   * The short description of the option. An extended description is provided by
   * the method {@link #getFullDescription()}
   */
  protected String shortDescription;

  /**
   * The value last passed to this option.
   */
  protected T givenValue = null;

  /**
   * The value of this option.
   */
  private T value;

  /**
   * Constructs a parameter with the given optionID, constraints, and default
   * value.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraints of this parameter, may be empty if there
   *        are no constraints
   * @param defaultValue the default value of this parameter (may be null)
   */
  public AbstractParameter(OptionID optionID, List<ParameterConstraint<S>> constraints, T defaultValue) {
    this.optionid = optionID;
    this.shortDescription = optionID.getDescription();
    this.optionalParameter = true;
    this.defaultValue = defaultValue;
    this.constraints = (constraints != null) ? constraints : new ArrayList<ParameterConstraint<S>>();
  }

  /**
   * Constructs a parameter with the given optionID, constraints, and optional
   * flag.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraints of this parameter, may be empty if there
   *        are no constraints
   * @param optional specifies if this parameter is an optional parameter
   */
  public AbstractParameter(OptionID optionID, List<ParameterConstraint<S>> constraints, boolean optional) {
    this.optionid = optionID;
    this.shortDescription = optionID.getDescription();
    this.optionalParameter = optional;
    this.defaultValue = null;
    this.constraints = (constraints != null) ? constraints : new ArrayList<ParameterConstraint<S>>();
  }

  /**
   * Constructs a parameter with the given optionID, and constraints.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraints of this parameter, may be empty if there
   *        are no constraints
   */
  public AbstractParameter(OptionID optionID, List<ParameterConstraint<S>> constraints) {
    this(optionID, constraints, false);
  }

  /**
   * Constructs a parameter with the given optionID, constraint, and default
   * value.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   * @param defaultValue the default value of this parameter (may be null)
   */
  public AbstractParameter(OptionID optionID, ParameterConstraint<S> constraint, T defaultValue) {
    this(optionID, makeConstraintsVector(constraint), defaultValue);
  }

  /**
   * Constructs a parameter with the given optionID, constraint, and optional
   * flag.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public AbstractParameter(OptionID optionID, ParameterConstraint<S> constraint, boolean optional) {
    this(optionID, makeConstraintsVector(constraint), optional);
  }

  /**
   * Constructs a parameter with the given optionID, and constraint.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   */
  public AbstractParameter(OptionID optionID, ParameterConstraint<S> constraint) {
    this(optionID, constraint, false);
  }

  /**
   * Constructs a parameter with the given optionID and default value.
   * 
   * @param optionID the unique id of the option
   * @param defaultValue default value.
   */
  public AbstractParameter(OptionID optionID, T defaultValue) {
    this(optionID, (ArrayList<ParameterConstraint<S>>) null, defaultValue);
  }

  /**
   * Constructs a parameter with the given optionID and optional flag.
   * 
   * @param optionID the unique id of the option
   * @param optional optional flag
   */
  public AbstractParameter(OptionID optionID, boolean optional) {
    this(optionID, (ArrayList<ParameterConstraint<S>>) null, optional);
  }

  /**
   * Constructs a parameter with the given optionID.
   * 
   * @param optionID the unique id of the option
   */
  public AbstractParameter(OptionID optionID) {
    this(optionID, (ArrayList<ParameterConstraint<S>>) null, false);
  }

  /**
   * Wrap a single constraint into a vector of constraints.
   * 
   * @param <S> Type
   * @param constraint Constraint, may be {@code null}
   * @return Vector containing the constraint (if not null)
   */
  private static <S> List<ParameterConstraint<S>> makeConstraintsVector(ParameterConstraint<S> constraint) {
    ArrayList<ParameterConstraint<S>> constraints = new ArrayList<ParameterConstraint<S>>((constraint == null) ? 0 : 1);
    if(constraint != null) {
      constraints.add(constraint);
    }
    return constraints;
  }

  /**
   * Sets the default value of this parameter.
   * 
   * @param defaultValue default value of this parameter
   */
  @Override
  public void setDefaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
    this.optionalParameter = true;
  }

  /**
   * Checks if this parameter has a default value.
   * 
   * @return true, if this parameter has a default value, false otherwise
   */
  @Override
  public boolean hasDefaultValue() {
    return !(defaultValue == null);
  }

  /**
   * Sets the default value of this parameter as the actual value of this
   * parameter.
   */
  // TODO: can we do this more elegantly?
  @Override
  public void useDefaultValue() {
    setValueInternal(defaultValue);
    defaultValueTaken = true;
  }

  /**
   * Handle default values for a parameter.
   * 
   * @return Return code: {@code true} if it has a default value, {@code false}
   *         if it is optional without a default value. Exception if it is a
   *         required parameter!
   * @throws UnspecifiedParameterException If the parameter requires a value
   */
  @Override
  public boolean tryDefaultValue() throws UnspecifiedParameterException {
    // Assume default value instead.
    if(hasDefaultValue()) {
      useDefaultValue();
      return true;
    }
    else if(isOptional()) {
      // Optional is fine, but not successful
      return false;
    }
    else {
      throw new UnspecifiedParameterException(this);
    }
  }

  /**
   * Specifies if this parameter is an optional parameter.
   * 
   * @param opt true if this parameter is optional, false otherwise
   */
  @Override
  public void setOptional(boolean opt) {
    this.optionalParameter = opt;
  }

  /**
   * Checks if this parameter is an optional parameter.
   * 
   * @return true if this parameter is optional, false otherwise
   */
  @Override
  public boolean isOptional() {
    return this.optionalParameter;
  }

  /**
   * Checks if the default value of this parameter was taken as the actual
   * parameter value.
   * 
   * @return true, if the default value was taken as actual parameter value,
   *         false otherwise
   */
  @Override
  public boolean tookDefaultValue() {
    return defaultValueTaken;
  }

  /**
   * Returns true if the value of the option is defined, false otherwise.
   * 
   * @return true if the value of the option is defined, false otherwise.
   */
  @Override
  public boolean isDefined() {
    return (this.value != null);
  }

  /**
   * Returns the default value of the parameter.
   * <p/>
   * If the parameter has no default value, the method returns <b>null</b>.
   * 
   * @return the default value of the parameter, <b>null</b> if the parameter
   *         has no default value.
   */
  // TODO: change this to return a string value?
  @Override
  public T getDefaultValue() {
    return defaultValue;
  }

  /**
   * Whether this class has a list of default values.
   * 
   * @return whether the class has a description of valid values.
   */
  @Override
  public boolean hasValuesDescription() {
    return false;
  }

  /**
   * Return a string explaining valid values.
   * 
   * @return String explaining valid values (e.g. a class list)
   */
  @Override
  public String getValuesDescription() {
    return "";
  }

  /**
   * Returns the extended description of the option which includes the option's
   * type, the short description and the default value (if specified).
   * 
   * @return the option's description.
   */
  @Override
  public String getFullDescription() {
    StringBuilder description = new StringBuilder();
    // description.append(getParameterType()).append(" ");
    description.append(shortDescription);
    description.append(FormatUtil.NEWLINE);
    if(hasValuesDescription()) {
      final String valuesDescription = getValuesDescription();
      description.append(valuesDescription);
      if(!valuesDescription.endsWith(FormatUtil.NEWLINE)) {
        description.append(FormatUtil.NEWLINE);
      }
    }
    if(hasDefaultValue()) {
      description.append("Default: ");
      description.append(getDefaultValueAsString());
      description.append(FormatUtil.NEWLINE);
    }
    if(!constraints.isEmpty()) {
      if(constraints.size() == 1) {
        description.append("Constraint: ");
      }
      else if(constraints.size() > 1) {
        description.append("Constraints: ");
      }
      for(int i = 0; i < constraints.size(); i++) {
        ParameterConstraint<S> constraint = constraints.get(i);
        if(i > 0) {
          description.append(", ");
        }
        description.append(constraint.getDescription(getName()));
        if(i == constraints.size() - 1) {
          description.append('.');
        }
      }
      description.append(FormatUtil.NEWLINE);
    }
    return description.toString();
  }

  /**
   * Validate a value after parsing (e.g. do constrain checks!)
   * 
   * @param obj Object to validate
   * @return true iff the object is valid for this parameter.
   * @throws ParameterException when the object is not valid.
   */
  protected boolean validate(T obj) throws ParameterException {
    try {
      for(ParameterConstraint<S> cons : this.constraints) {
        cons.test(obj);
      }
    }
    catch(ParameterException e) {
      throw new WrongParameterValueException("Specified parameter value for parameter \"" + getName() + "\" breaches parameter constraint.\n" + e.getMessage());
    }
    return true;
  }

  /**
   * Return the OptionID of this option.
   * 
   * @return Option ID
   */
  @Override
  public OptionID getOptionID() {
    return optionid;
  }

  /**
   * Returns the name of the option.
   * 
   * @return the option's name.
   */
  @Override
  public String getName() {
    return optionid.getName();
  }

  /**
   * Returns the short description of the option.
   * 
   * @return the option's short description.
   */
  @Override
  public String getShortDescription() {
    return shortDescription;
  }

  /**
   * Sets the short description of the option.
   * 
   * @param description the short description to be set
   */
  @Override
  public void setShortDescription(String description) {
    this.shortDescription = description;
  }

  /**
   * Sets the value of the option.
   * 
   * @param obj the option's value to be set
   * @throws ParameterException if the given value is not a valid value for this
   *         option.
   */
  @Override
  public void setValue(Object obj) throws ParameterException {
    T val = parseValue(obj);
    if(validate(val)) {
      setValueInternal(val);
    }
    else {
      throw new InvalidParameterException("Value for option \"" + getName() + "\" did not validate: " + obj.toString());
    }
  }

  /**
   * Internal setter for the value.
   * 
   * @param val Value
   */
  protected final void setValueInternal(T val) {
    this.value = this.givenValue = val;
  }

  /**
   * Returns the value of the option.
   * 
   * You should use either {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization#grab}
   * or {@link #isDefined} to test if getValue() will return a well-defined value.
   * 
   * @return the option's value.
   */
  @Override
  public final T getValue() {
    if(this.value == null) {
      LoggingUtil.warning("Programming error: Parameter#getValue() called for unset parameter \"" + this.optionid.getName() + "\"", new Throwable());
    }
    return this.value;
  }

  /**
   * Get the last given value. May return {@code null}
   * 
   * @return Given value
   */
  @Override
  public Object getGivenValue() {
    return this.givenValue;
  }

  /**
   * Checks if the given argument is valid for this option.
   * 
   * @param obj option value to be checked
   * @return true, if the given value is valid for this option
   * @throws ParameterException if the given value is not a valid value for this
   *         option.
   */
  @Override
  public final boolean isValid(Object obj) throws ParameterException {
    T val = parseValue(obj);
    return validate(val);
  }

  /**
   * Returns a string representation of the parameter's type (e.g. an
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter}
   * should return {@code <int>}).
   * 
   * @return a string representation of the parameter's type
   */
  @Override
  public abstract String getSyntax();

  /**
   * Parse a given value into the destination type.
   * 
   * @param obj Object to parse (may be a string representation!)
   * @return Parsed object
   * @throws ParameterException when the object cannot be parsed.
   */
  protected abstract T parseValue(Object obj) throws ParameterException;

  /**
   * Get the value as string. May return {@code null}
   * 
   * @return Value as string
   */
  @Override
  public abstract String getValueAsString();

  /**
   * Get the default value as string.
   * 
   * @return default value
   */
  @Override
  public String getDefaultValueAsString() {
    return getDefaultValue().toString();
  }

  /**
   * Add an additional constraint.
   * 
   * @param constraint Constraint to add.
   */
  @Override
  public void addConstraint(ParameterConstraint<S> constraint) {
    constraints.add(constraint);
  }
}