package com.peter.utils.formula;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * The util is used for verify & calculate formulas.<br>
 * e.g (x+y)*z-1 while x=2, y=3 and z=2, then we get 9.
 * 
 * @author ylkang 2011-8-29
 * @version 1.0.0
 */
public class FormulaUtil {

	private final static char ADD = '+';
	private final static char SUBTRACT = '-';
	private final static char MULTIPLY = '*';
	private final static char DIVIDE = '/';
	private final static char LEFT_BRACKET = '(';
	private final static char RIGHT_BRACKET = ')';
	private final static char SCALE = 6;

	private final static String NEGATIVE = "'";

	private final static List<Character> OPERATOR_LIST = new ArrayList<Character>();
	private final static List<Character> BRACKET_LIST = new ArrayList<Character>();

	static {
		OPERATOR_LIST.add(ADD);
		OPERATOR_LIST.add(SUBTRACT);
		OPERATOR_LIST.add(MULTIPLY);
		OPERATOR_LIST.add(DIVIDE);

		BRACKET_LIST.add(LEFT_BRACKET);
		BRACKET_LIST.add(RIGHT_BRACKET);
	}

	/**
	 * validate the formula.
	 * 
	 * Otherwise, if you want to use this function worldwide, you need to
	 * redesign a new rule to check. e.g. is (A_1 + B_2) valid, is (A@V * C$D)
	 * valid.
	 * 
	 * @author ylkang 2011-8-29
	 * @version 1.0.0
	 * 
	 * @param formula
	 * @param VAR
	 * @return
	 */
	public static FormulaFormatResult validateFormula(String formula,
			Set<String> varNames) {

		if (varNames == null) {
			varNames = new HashSet<String>();
		}
		if (formula == null || StringUtils.isEmpty(formula)) {
			return getErrorFormatResponse("formula is empty: ", formula);
		}

		formula = formula.replaceAll(" ", "").replaceAll("\t", "")
				.replaceAll("\r", "").replaceAll("\n", "");

		if (!Pattern.matches("[a-zA-Z0-9_ \\(\\)\\*/+\\-\\.]*", formula)) {
			return getErrorFormatResponse(
					"formula is invalid: contains invalid char", formula);
		} /* ( + or + ) */else if (Pattern.matches("[\\*/+\\- ]*[)]", formula)) {
			return getErrorFormatResponse(
					"formula is invalid: contains [*+-/]): ", formula);
		} /* ( + ) */else if (Pattern.matches("[(][\\*/+ ]*[)]", formula)) {
			return getErrorFormatResponse(
					"formula is invalid: start with [+*/] ", formula);
		} /* +1 */else if (!Pattern.matches("[^+\\*/].*", formula)) {
			return getErrorFormatResponse("formula is invalid: start with +*/",
					formula);
		} /* 1- */else if (!Pattern.matches(".*[^\\-+\\*/]", formula)) {
			return getErrorFormatResponse(
					"formula is invalid: end with [+-*/] ", formula);
		}
		if (formula.startsWith("-")) {
			formula = formula.replaceFirst("-", "(-1)*");
		}

		if (formula.indexOf("" + LEFT_BRACKET + RIGHT_BRACKET) != -1) {
			return getErrorFormatResponse("formula is invalid: contains () ",
					formula);
		}

		int length = formula.length();
		int bracketNum = 0;
		// +-*/ should not follow each other directly.
		char previousChar = '@';

		// in case there is A+3/3.2
		StringBuffer newFormula = new StringBuffer();
		StringBuffer varName = new StringBuffer();

		boolean isAnyValidElementExists = false;

		for (int i = 0; i < length; i++) {
			char currChar = formula.charAt(i);
			if (currChar == ' ') {
				continue;
			}
			if (bracketNum < 0) {
				return getErrorFormatResponse(
						"formula is invalid: left ( is less then right ) ",
						formula);
			}

			// VARS
			if (!OPERATOR_LIST.contains(currChar)
					&& !BRACKET_LIST.contains(currChar)) {
				varName.append(currChar);
				previousChar = currChar;
				continue;
			} /* operators */else {
				if (varName.length() > 0
						&& !NumberUtils.isNumber(varName.toString())
						&& !varNames.contains(varName.toString())) {
					return getErrorFormatResponse(
							"formula is invalid: invalid parameters " + varName,
							formula);
				} else if (varName.length() > 0) {
					newFormula.append(varName);
					isAnyValidElementExists = true;
				}
				varName = new StringBuffer();
				if (currChar == LEFT_BRACKET) {
					bracketNum++;
					if (isCharNumber(previousChar, varNames)) {
						newFormula.append(MULTIPLY);
					}
				} else if (currChar == RIGHT_BRACKET) {
					bracketNum--;
				}

				// previous char is +-*/, current char shouldn't be +-*/
				if (OPERATOR_LIST.contains(previousChar)) {
					if (OPERATOR_LIST.contains(currChar)
							|| RIGHT_BRACKET == currChar) {
						// "+-" or "+)"
						return getErrorFormatResponse(
								"formula is invalid: contains symbol like +-",
								formula);
					}
					// previous char is (
				} else if (LEFT_BRACKET == previousChar) {
					// previous is ( and current is +/*-
					if (OPERATOR_LIST.contains(currChar)
							&& currChar != SUBTRACT) {
						return getErrorFormatResponse(
								"formula is invalid: contains ([+/*]", formula);
					} else {
						newFormula.append("'1").append(MULTIPLY);
						previousChar = currChar;
						continue;
					}
				} /* previous is VAR */else {
					if (currChar == SUBTRACT) {
						newFormula.append("+('1)").append(MULTIPLY);
						previousChar = currChar;
						continue;
					}
				}
				newFormula.append(currChar);
				previousChar = currChar;
			}
		}
		if (bracketNum != 0 || !isAnyValidElementExists || bracketNum != 0) {
			return getErrorFormatResponse(
					"formula is invalid: left '(' is more than right ')' ",
					formula);
		}
		return getOkFormatResponse(newFormula.toString());
	}

	private static boolean isCharNumber(char currChar, final Set<String> VARSET) {
		return (VARSET != null && VARSET.contains("" + currChar))
				|| currChar == '.' || (currChar >= '0' && currChar <= '9');
	}

	/**
	 * calculate current formula
	 * 
	 * @author ylkang 2011-8-30
	 * @version 1.0.0
	 * 
	 * @param formula
	 * @param paramMap
	 * @return
	 */
	public static FormulaResult calculateFormula(String formula,
			Map<String, BigDecimal> paramMap) {
		if (paramMap == null) {
			paramMap = new HashMap<String, BigDecimal>();
		}
		FormulaFormatResult formatResult = validateFormula(formula,
				paramMap.keySet());
		if (formatResult.status == FormulaStatus.ERROR) {
			return getErrorCalculationResult(formula, formatResult.message);
		}
		formula = formatResult.formatedFormula;

		Stack<Character> operatorStack = new Stack<Character>();
		Stack<BigDecimal> varStack = new Stack<BigDecimal>();

		int length = formula.length();
		StringBuffer varName = new StringBuffer();

		try {
			for (int i = 0; i < length; i++) {
				char currChar = formula.charAt(i);
				if (!OPERATOR_LIST.contains(currChar)
						&& !BRACKET_LIST.contains(currChar)) {
					varName.append(currChar);
					continue;
				}
				if (!StringUtils.isEmpty(varName.toString())) {
					// push variable into stack
					pushVarValue(paramMap, varName, varStack);
					varName = new StringBuffer();
				}
				// check current operator
				if (currChar != RIGHT_BRACKET) {
					if (currChar == LEFT_BRACKET) {
						operatorStack.push(currChar);
						continue;
					}
					if (isPopStack(currChar, operatorStack)) {
						BigDecimal pre_1_Var = varStack.pop();
						BigDecimal pre_2_Var = varStack.pop();
						Character preOperatpr = operatorStack.pop();
						switch (preOperatpr) {
						case ADD:
							varStack.push(pre_1_Var.add(pre_2_Var));
							break;
						case SUBTRACT:
							varStack.push(pre_1_Var.subtract(pre_2_Var));
							break;
						case MULTIPLY:
							varStack.push(pre_1_Var.multiply(pre_2_Var));
							break;
						case DIVIDE:
							varStack.push(pre_2_Var.divide(pre_1_Var, SCALE,
									RoundingMode.HALF_UP));
							break;
						default:
							throw new FormulaException(
									"invalid operator exists: " + preOperatpr);
						}
					}
					operatorStack.push(currChar);
				} else {
					popElementAndcalculate(operatorStack, varStack, false);
				}
			}
			// push variable into stack
			pushVarValue(paramMap, varName, varStack);
			popElementAndcalculate(operatorStack, varStack, true);
			return getOkCalculationResult(formula, varStack.pop().doubleValue());
		} catch (ArithmeticException e) {
			return getOkCalculationResult(formula, Double.POSITIVE_INFINITY);
		} catch (FormulaException e) {
			return getErrorCalculationResult(formula, e.getMessage());
		}
	}

	private static void pushVarValue(Map<String, BigDecimal> paramMap,
			StringBuffer varName, Stack<BigDecimal> stack)
			throws FormulaException {
		if (StringUtils.isEmpty(varName.toString()))
			return;
		if (varName.toString().startsWith(NEGATIVE)) {
			stack.push(new BigDecimal(varName.replace(0, 1, SUBTRACT + "")
					.toString()));
			return;
		}
		if (paramMap.get(varName.toString()) == null
				&& !NumberUtils.isNumber(varName.toString())) {
			throw new FormulaException("invalid variable: " + varName
					+ ". valueMap: " + paramMap);
		}
		BigDecimal value = null;
		if (!NumberUtils.isNumber(varName.toString())) {
			value = paramMap.get(varName.toString());
			if (value == null) {
				throw new FormulaException("invalid parameter & value: "
						+ varName);
			}
		} else {
			value = new BigDecimal(varName.toString());
		}
		stack.push(value);
	}

	private static FormulaResult getOkCalculationResult(String formula,
			double resultValue) {
		FormulaResult result = new FormulaResult();
		result.formula = formula;
		result.status = FormulaStatus.OK;
		result.value = resultValue;
		return result;
	}

	private static FormulaResult getErrorCalculationResult(String formula,
			String msg) {
		FormulaResult result = new FormulaResult();
		result.formula = formula;
		result.message = msg;
		result.status = FormulaStatus.ERROR;
		return result;
	}

	private static void popElementAndcalculate(Stack<Character> operatorStack,
			Stack<BigDecimal> varStack, boolean isFinalRound)
			throws FormulaException {
		// pop all VAR out of stack until meet ( or end
		while (!operatorStack.isEmpty()) {
			Character oper = operatorStack.pop();
			if (!isFinalRound)
				if (oper == LEFT_BRACKET) {
					return;
				}
			if (oper == LEFT_BRACKET) {
				continue;
			}
			BigDecimal var_1 = varStack.pop();
			BigDecimal var_2 = varStack.pop();
			varStack.push(calculate(var_1, var_2, oper));
		}
	}

	private static BigDecimal calculate(BigDecimal param_1_value,
			BigDecimal param_2_value, Character operatpr)
			throws FormulaException {
		switch (operatpr) {
		case ADD:
			return param_1_value.add(param_2_value);
		case SUBTRACT:
			return param_2_value.subtract(param_1_value);
		case MULTIPLY:
			return param_1_value.multiply(param_2_value);
		case DIVIDE:
			return param_2_value.divide(param_1_value, SCALE,
					RoundingMode.HALF_UP);
		default:
			throw new FormulaException("invalid operator exists: " + operatpr);
		}
	}

	/**
	 * lower priority operate will trigger pop from the stack.
	 * 
	 * @author ylkang 2011-8-30
	 * @version 1.0.0
	 * 
	 * @param currOper
	 * @param stack
	 * @return
	 */
	private static boolean isPopStack(char currOper, Stack<Character> stack) {
		if (stack.isEmpty()) {
			return false;
		}
		Character previousOper = stack.peek();
		if (previousOper == LEFT_BRACKET) {
			return false;
		}
		if (previousOper == ADD || previousOper == SUBTRACT) {
			return false;
		} else {
			return true;
		}
	}

	private static FormulaFormatResult getErrorFormatResponse(String msg,
			String formula) {
		FormulaFormatResult result = new FormulaFormatResult();
		result.message = msg + formula;
		result.status = FormulaStatus.ERROR;
		return result;
	}

	private static FormulaFormatResult getOkFormatResponse(String newFormula) {
		FormulaFormatResult result = new FormulaFormatResult();
		result.status = FormulaStatus.OK;
		result.formatedFormula = newFormula;
		return result;
	}

	public static class FormulaResult {
		public FormulaStatus status = FormulaStatus.OK;
		public String message = "";
		public double value;
		public String formula;
	}

	public static class FormulaFormatResult {
		public FormulaStatus status = FormulaStatus.OK;
		public String message = "";
		public String formatedFormula;
	}

	public enum FormulaStatus {
		OK, ERROR
	}

}