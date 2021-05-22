/**
 * Interval.java, uk.ac.macaulay.util
 * 
 * Copyright (C) The James Hutton Institute 2013
 * 
 * This file is part of utils
 * 
 * utils is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * utils is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * utils. If not, see <http://www.gnu.org/licenses/>.
 */
package util;

import java.io.Serializable;

/**
 * <!-- -->
 * 
 * @author Gary Polhill
 * 
 */
public class Interval<T extends Number> implements Comparable<Interval<Double>>, Serializable {
  /**
   * <!-- minimum -->
   * 
   * The minimum of the range
   */
  private final T minimum;

  /**
   * <!-- maximum -->
   * 
   * The maximum of the range
   */
  private final T maximum;

  /**
   * <!-- minimumInclusive -->
   * 
   * <code>true</code> if the range includes the minimum
   */
  private final boolean minimumInclusive;

  /**
   * <!-- maximumInclusive -->
   * 
   * <code>true</code> if the range includes the maximum
   */
  private final boolean maximumInclusive;

  /**
   * <!-- Interval constructor -->
   * 
   * Main constructor method
   */
  public Interval(T minimum, T maximum, boolean minimumInclusive, boolean maximumInclusive) {
    this.minimum = minimum;
    this.maximum = maximum;
    if(minimum == null || maximum == null) throw new NullPointerException();
    this.minimumInclusive = minimumInclusive;
    this.maximumInclusive = maximumInclusive;
  }

  /**
   * <!-- Interval constructor -->
   * 
   * @param minimum
   * @param maximum
   */
  public Interval(T minimum, T maximum) {
    this(minimum, maximum, true, true);
  }

  /**
   * <!-- getMinimum -->
   * 
   * @return the minimum of the range
   */
  public T getMinimum() {
    return minimum;
  }

  /**
   * <!-- getMaximum -->
   * 
   * @return the maximum of the range
   */
  public T getMaximum() {
    return maximum;
  }

  /**
   * <!-- getDoubleMinimumInclusive -->
   * 
   * @return the lowest representable number that is within the range
   */
  public double getDoubleMinimumInclusive() {
    return minimumInclusive ? minimum.doubleValue() : Math.nextUp(minimum.doubleValue());
  }

  /**
   * <!-- getDoubleMaximumInclusive -->
   * 
   * @return the highest representable number that is within the range
   */
  public double getDoubleMaximumInclusive() {
    return maximumInclusive ? maximum.doubleValue() : Math.nextAfter(maximum.doubleValue(), Double.NEGATIVE_INFINITY);
  }

  /**
   * <!-- getDoubleDifference -->
   * 
   * @return the difference between the minimum and maximum representable
   *         numbers
   */
  public double getDoubleDifference() {
    return getDoubleMaximumInclusive() - getDoubleMinimumInclusive();
  }

  /**
   * <!-- getDoubleUniformSample -->
   * 
   * @return a uniform sample that is within the range
   */
  public double getDoubleUniformSample() {
    return getDoubleMinimumInclusive() + Math.random() * Math.nextUp(getDoubleDifference());
  }

  /**
   * <!-- containsDouble -->
   * 
   * @param value
   * @return whether or not the value is within the range
   */
  public boolean containsDouble(double value) {
    if((value < maximum.doubleValue() || (maximumInclusive && value == maximum.doubleValue()))
	&& (value > minimum.doubleValue() || (minimumInclusive && value == minimum.doubleValue()))) return true;
    return false;
  }

  /**
   * <!-- equals -->
   * 
   * Are the two ranges equal?
   * 
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object other) {
    if(other instanceof Interval) {
      return equals((Interval<?>)other);
    }
    return this == other;
  }

  /**
   * <!-- equals -->
   *
   * @param other
   * @return <code>true</code> if the two ranges contain the same set of numbers
   */
  public boolean equals(Interval<?> other) {
    return minimumInclusive == other.minimumInclusive && maximumInclusive == other.maximumInclusive
	&& other.maximum.equals(maximum) && other.minimum.equals(minimum);
  }

  /**
   * <!-- toString -->
   *
   * Return the interval as a string
   *
   * @see Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer buff = new StringBuffer(minimumInclusive ? "[" : "]");
    buff.append(minimum.toString());
    buff.append(", ");
    buff.append(maximum.toString());
    buff.append(maximumInclusive ? "]" : "[");
    return buff.toString();
  }

  public Interval<Double> parseDoubleInterval(String str) {
    String firstBracket = str.substring(0, 1);
    String lastBracket = str.substring(str.length() - 1, str.length());
    String rest = str.substring(1);
    int commaSpacePos = rest.indexOf(", ");
    int nextNumberPos = commaSpacePos + 2;
    if(commaSpacePos == -1) {
      commaSpacePos = rest.indexOf(",");
      nextNumberPos = commaSpacePos + 1;
    }
    String firstNumber = rest.substring(0, commaSpacePos);
    String lastNumber = rest.substring(nextNumberPos, rest.length() - 1);
    return new Interval<Double>(Double.parseDouble(firstNumber), Double.parseDouble(lastNumber),
	firstBracket.equals("["), lastBracket.equals("]"));
  }

  /**
   * <!-- clone -->
   *
   * @see Object#clone()
   */
  @Override
  public Interval<T> clone() {
    return new Interval<T>(minimum, maximum, minimumInclusive, maximumInclusive);
  }

  /**
   * <!-- compareTo -->
   *
   * @see Comparable#compareTo(Object)
   */
  @Override
  public int compareTo(Interval<Double> other) {
    int dirMin = Double.compare(minimum.doubleValue(), other.maximum);
    if(dirMin > 0 || (dirMin == 0 && (!minimumInclusive || !other.maximumInclusive))) return 1;

    int dirMax = Double.compare(maximum.doubleValue(), other.minimum);
    if(dirMax < 0 || (dirMax == 0 && (!maximumInclusive || !other.minimumInclusive))) return -1;

    return 0;
  }
}
