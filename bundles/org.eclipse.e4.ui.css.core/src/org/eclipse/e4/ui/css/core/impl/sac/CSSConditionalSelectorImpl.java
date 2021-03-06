/*

   Copyright 2002, 2015  The Apache Software Foundation

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

/* This class copied from org.apache.batik.css.engine.sac */

package org.eclipse.e4.ui.css.core.impl.sac;

import java.util.Set;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.SimpleSelector;
import org.w3c.dom.Element;

/**
 * This class provides an implementation of the
 * {@link org.w3c.css.sac.ConditionalSelector} interface.
 */
public class CSSConditionalSelectorImpl implements ConditionalSelector, ExtendedSelector {

	/**
	 * The simple selector.
	 */
	protected SimpleSelector simpleSelector;

	/**
	 * The condition.
	 */
	protected Condition condition;

	/**
	 * Creates a new ConditionalSelector object.
	 */
	public CSSConditionalSelectorImpl(SimpleSelector s, Condition c) {
		simpleSelector = s;
		condition      = c;
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * @param obj the reference object with which to compare.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || (obj.getClass() != getClass())) {
			return false;
		}
		CSSConditionalSelectorImpl s = (CSSConditionalSelectorImpl)obj;
		return (s.simpleSelector.equals(simpleSelector) &&
				s.condition.equals(condition));
	}

	/**
	 * <b>SAC</b>: Implements {@link
	 * org.w3c.css.sac.Selector#getSelectorType()}.
	 */
	@Override
	public short getSelectorType() {
		return SAC_CONDITIONAL_SELECTOR;
	}

	/**
	 * Tests whether this selector matches the given element.
	 */
	@Override
	public boolean match(Element e, String pseudoE) {
		return ((ExtendedSelector)getSimpleSelector()).match(e, pseudoE) &&
				((ExtendedCondition)getCondition()).match(e, pseudoE);
	}

	/**
	 * Fills the given set with the attribute names found in this selector.
	 */
	@Override
	public void fillAttributeSet(Set<String> attrSet) {
		((ExtendedSelector)getSimpleSelector()).fillAttributeSet(attrSet);
		((ExtendedCondition)getCondition()).fillAttributeSet(attrSet);
	}

	/**
	 * Returns the specificity of this selector.
	 */
	@Override
	public int getSpecificity() {
		return ((ExtendedSelector)getSimpleSelector()).getSpecificity() +
				((ExtendedCondition)getCondition()).getSpecificity();
	}

	/**
	 * <b>SAC</b>: Implements {@link
	 * org.w3c.css.sac.ConditionalSelector#getSimpleSelector()}.
	 */
	@Override
	public SimpleSelector getSimpleSelector() {
		return simpleSelector;
	}

	/**
	 * <b>SAC</b>: Implements {@link
	 * org.w3c.css.sac.ConditionalSelector#getCondition()}.
	 */
	@Override
	public Condition getCondition() {
		return condition;
	}

	/**
	 * Returns a representation of the selector.
	 */
	@Override
	public String toString() {
		return String.valueOf( simpleSelector ) + condition;
	}
}
