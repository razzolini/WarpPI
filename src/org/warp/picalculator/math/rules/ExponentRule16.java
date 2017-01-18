package org.warp.picalculator.math.rules;

import java.util.ArrayList;

import org.warp.picalculator.Error;
import org.warp.picalculator.math.Calculator;
import org.warp.picalculator.math.functions.Expression;
import org.warp.picalculator.math.functions.Function;
import org.warp.picalculator.math.functions.Multiplication;
import org.warp.picalculator.math.functions.Number;
import org.warp.picalculator.math.functions.Power;
import org.warp.picalculator.math.functions.Root;

/**
 * Exponent rule<br>
 * <b>a√x=x^1/a</b>
 * @author Andrea Cavalli
 *
 */
public class ExponentRule16 {

	public static boolean compare(Function f) {
		if (f instanceof Root) {
			Root fnc = (Root) f;
			if (fnc.getVariable1().equals(fnc.getVariable2())) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<Function> execute(Function f) throws Error {
		Calculator root = f.getRoot();
		ArrayList<Function> result = new ArrayList<>();
		Multiplication fnc = (Multiplication) f;
		Power p = new Power(fnc.getRoot(), null, null);
		Expression expr = new Expression(root);
		Function a = fnc.getVariable1();
		expr.addFunctionToEnd(a);
		Number two = new Number(root, 2);
		p.setVariable1(expr);
		p.setVariable2(two);
		result.add(p);
		return result;
	}

}
