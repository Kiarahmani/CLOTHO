package Z3;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Quantifier;
import cons.ConstantArgs;

public class StaticAssertions {
	Context ctx;
	DeclaredObjects objs;
	Expr o1, o2, o3;

	public StaticAssertions(Context ctx, DeclaredObjects objs) {
		this.ctx = ctx;
		this.objs = objs;
		o1 = ctx.mkFreshConst("o", objs.getSort("O"));
		o2 = ctx.mkFreshConst("o", objs.getSort("O"));
		o3 = ctx.mkFreshConst("o", objs.getSort("O"));
	}

	public Quantifier mk_par_then_sib() {
		BoolExpr lhs = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), o1), ctx.mkApp(objs.getfuncs("parent"), o2));
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_sib_then_par() {
		BoolExpr rhs = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), o1), ctx.mkApp(objs.getfuncs("parent"), o2));
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_ar_on_writes() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2);
		BoolExpr rhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o1);
		BoolExpr rhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o2);
		BoolExpr body = ctx.mkImplies(lhs, ctx.mkAnd(rhs1, rhs2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_vis_on_writes() {
		BoolExpr lhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("vis"), o1, o2);
		BoolExpr lhs2 = (BoolExpr) ctx.mkNot(ctx.mkEq(o1, o2));
		BoolExpr lhs = ctx.mkAnd(lhs1, lhs2);

		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o1);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_vis_then_ar() {
		BoolExpr lhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("vis"), o1, o2);
		BoolExpr lhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o1);
		BoolExpr lhs3 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o2);
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2);
		BoolExpr body = ctx.mkImplies(ctx.mkAnd(lhs1, lhs2, lhs3), rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_types_then_eq() {
		BoolExpr lhs1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), o1), ctx.mkApp(objs.getfuncs("parent"), o2));
		BoolExpr lhs2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), o1), ctx.mkApp(objs.getfuncs("otype"), o2));
		BoolExpr rhs = (BoolExpr) ctx.mkEq(o1, o2);
		BoolExpr body = ctx.mkImplies(ctx.mkAnd(lhs1, lhs2), rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_no_loops_o() {
		BoolExpr ass1 = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), o1, o1);
		BoolExpr ass2 = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), o1, o1);
		BoolExpr ass3 = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), o1, o1);
		BoolExpr ass4 = (BoolExpr) ctx.mkApp(objs.getfuncs("vis"), o1, o1);
		BoolExpr body = ctx.mkNot(ctx.mkOr(ass1, ass2, ass3, ass4));
		Quantifier x = ctx.mkForall(new Expr[] { o1 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_trans_ar() {
		BoolExpr lhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2);
		BoolExpr lhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o2, o3);
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o3);
		BoolExpr body = ctx.mkImplies(ctx.mkAnd(lhs1, lhs2), rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2, o3 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_total_ar() {
		BoolExpr lhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o1);
		BoolExpr lhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o2);
		BoolExpr lhs3 = (BoolExpr) ctx.mkNot(ctx.mkEq(o1, o2));
		BoolExpr lhs4 = ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2));
		BoolExpr rhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2);
		BoolExpr rhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o2, o1);
		BoolExpr body = ctx.mkImplies(ctx.mkAnd(lhs1, lhs2, lhs3, lhs4), ctx.mkXor(rhs1, rhs2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_wr_then_vis() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), o1, o2);
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("vis"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_rw_then_not_vis() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), o1, o2);
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("vis"), o2, o1);
		BoolExpr body = ctx.mkImplies(lhs, ctx.mkNot(rhs));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_ww_then_ar() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), o1, o2);
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_irreflx_ar() {
		BoolExpr body = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o1);
		Quantifier x = ctx.mkForall(new Expr[] { o1 }, ctx.mkNot(body), 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_irreflx_sibling() {
		BoolExpr body = (BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o1);
		Quantifier x = ctx.mkForall(new Expr[] { o1 }, ctx.mkNot(body), 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_gen_dep() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("D"), o1, o2);
		BoolExpr rhs1 = ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2));
		BoolExpr rhs2 = ctx.mkOr((BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), o1, o2),
				(BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), o1, o2),
				(BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), o1, o2));
		BoolExpr body = ctx.mkImplies(lhs, ctx.mkAnd(rhs1, rhs2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_gen_dep_props() {
		BoolExpr lhs = ctx.mkOr((BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), o1, o2),
				(BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), o1, o2),
				(BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), o1, o2),
				(BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2));
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("X"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_gen_depx() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("X"), o1, o2);
		BoolExpr rhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2);
		BoolExpr rhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("D"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, ctx.mkOr(rhs1, rhs2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_gen_depx_props() {
		BoolExpr lhs = ctx.mkOr((BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), o1, o2),
				(BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), o1, o2),
				(BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), o1, o2));
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("D"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_otime_props() {
		ArithExpr o1T = (ArithExpr) ctx.mkApp(objs.getfuncs("otime"), o1);
		ArithExpr o2T = (ArithExpr) ctx.mkApp(objs.getfuncs("otime"), o2);
		ArithExpr o1P = (ArithExpr) ctx.mkApp(objs.getfuncs("opart"), o1);
		ArithExpr o2P = (ArithExpr) ctx.mkApp(objs.getfuncs("opart"), o2);
		BoolExpr o1IU = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o1);
		BoolExpr o2IU = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o2);
		BoolExpr eqO = ctx.mkEq(o1, o2);
		BoolExpr eqP = ctx.mkEq(o1P, o2P);
		BoolExpr eqT = ctx.mkEq(o1T, o2T);

		BoolExpr body01 = ctx.mkAnd(ctx.mkEq(o1P, o2P), ctx.mkGt(o2T, o1T), o1IU, ctx.mkNot(o2IU));
		BoolExpr body02 = (BoolExpr) ctx.mkApp(objs.getfuncs("vis"), o1, o2);
		BoolExpr body0 = ctx.mkImplies(body01, body02);
		BoolExpr body1 = ctx.mkImplies(body02, body01);
		BoolExpr body2 = ctx.mkImplies(ctx.mkAnd(eqP, ctx.mkNot(eqO)), ctx.mkNot(eqT));
		BoolExpr body3 = ctx.mkGt(o1T, ctx.mkInt(0));
		BoolExpr body4 = ctx.mkImplies(ctx.mkNot(eqP), ctx.mkNot(body02));
		BoolExpr body5 = ctx.mkImplies(
				ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), o1), ctx.mkApp(objs.getfuncs("parent"), o2)), eqP);

		BoolExpr body6 = ctx.mkImplies((BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2), ctx.mkGt(o2T, o1T));
		BoolExpr body = ctx.mkAnd(body0, body1, body2, body3, body4, body5, body6);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_opart_props() {
		ArithExpr o1P = (ArithExpr) ctx.mkApp(objs.getfuncs("opart"), o1);
		BoolExpr body = ctx.mkEq(o1P, ctx.mkInt(ConstantArgs._current_partition_size));
		// BoolExpr body2 = ctx.mkGe(o1P, ctx.mkInt(1));
		// BoolExpr body = ctx.mkAnd(body1, body2);
		Quantifier x = ctx.mkForall(new Expr[] { o1 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_string_function_bounded_2(String funcName, String arg1Type, String arg2Type) {
		Expr a1 = ctx.mkFreshConst(arg1Type, objs.getSort(arg1Type));
		Expr a2 = ctx.mkFreshConst(arg1Type, objs.getSort(arg2Type));
		Expr r1P = ctx.mkApp(objs.getfuncs(funcName), a1, a2);
		BoolExpr body = (BoolExpr) ctx.mkApp(objs.getfuncs("my_strings"), r1P);
		Quantifier x = ctx.mkForall(new Expr[] { a1, a2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_integer_function_bounded_2(String funcName, String arg1Type, String arg2Type, int max) {
		Expr a1 = ctx.mkFreshConst(arg1Type, objs.getSort(arg1Type));
		Expr a2 = ctx.mkFreshConst(arg1Type, objs.getSort(arg2Type));
		ArithExpr r1P = (ArithExpr) ctx.mkApp(objs.getfuncs(funcName), a1, a2);
		BoolExpr body1 = ctx.mkLe(r1P, ctx.mkInt(max));
		BoolExpr body2 = ctx.mkGe(r1P, ctx.mkInt(1));
		BoolExpr body = ctx.mkAnd(body1, body2);
		Quantifier x = ctx.mkForall(new Expr[] { a1, a2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_integer_function_bounded_1(String funcName, String arg1Type, int max) {
		Expr a1 = ctx.mkFreshConst(arg1Type, objs.getSort(arg1Type));
		ArithExpr r1P = (ArithExpr) ctx.mkApp(objs.getfuncs(funcName), a1);
		BoolExpr body1 = ctx.mkLe(r1P, ctx.mkInt(max));
		BoolExpr body2 = ctx.mkGe(r1P, ctx.mkInt(1));
		BoolExpr body = ctx.mkAnd(body1, body2);
		Quantifier x = ctx.mkForall(new Expr[] { a1 }, body, 1, null, null, null, null);
		return x;
	}

	// To Temporarily change the shape of the anomalies
	public Quantifier mk_no_ww() {
		BoolExpr body = ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), o1, o2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	// To Temporarily change the shape of the anomalies
	public Quantifier mk_no_rw() {
		BoolExpr body = ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), o1, o2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	// To Temporarily change the shape of the anomalies
	public Quantifier mk_no_wr() {
		BoolExpr body = ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), o1, o2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr my_strings_props(int max) {
		Expr s1 = ctx.mkFreshConst("s", objs.getSort("String"));
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("my_strings"), s1);
		BoolExpr allRhs[] = new BoolExpr[max];
		for (int i = 0; i < max; i++) {
			allRhs[i] = ctx.mkEq(s1, ctx.MkString("text#" + String.valueOf(i)));
		}
		BoolExpr rhs = ctx.mkOr(allRhs);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { s1 }, body, 1, null, null, null, null);
		return x;
	}

	//

	//
	//
}
