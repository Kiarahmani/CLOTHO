package Z3.initialConstraints;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Quantifier;
import Z3.DeclaredObjects;

public class InitialAssertions {
	Context ctx;
	DeclaredObjects objs;
	Expr o1, i, o3, log;

	public InitialAssertions(Context ctx, DeclaredObjects objs) {
		this.ctx = ctx;
		this.objs = objs;
		log = ctx.mkFreshConst("l", objs.getSort("Logs"));
		i = ctx.mkFreshConst("i", objs.getSort("BitVec"));
		o3 = ctx.mkFreshConst("o", objs.getSort("O"));

	}

	public Quantifier mk_all_logs_greater_than_zero() {
		System.out.println("~~~>>>" + objs.getfuncs("Logs_PROJ_val"));
		BoolExpr body = ctx.mkGt((ArithExpr) ctx.mkApp(objs.getfuncs("Logs_PROJ_val"), log, ctx.mkBV(0, 3)),
				ctx.mkInt(2));
		Quantifier x = ctx.mkForall(new Expr[] { log, i }, body, 1, null, null, null, null);
		return x;
	}

}
