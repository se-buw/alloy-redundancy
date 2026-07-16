package de.buw.alloy.redundancy;


import java.util.ArrayList;
import java.util.List;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprLet;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.ExprUnary;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.ConvToConjunction;

public class ModuleTransformer {

  public static List<Expr> getCommandExprs(CompModule m, Command c) {
    List<Expr> exprs = flatten(c.formula);
    exprs.removeAll(flatten(m.getAllReachableFacts()));
    return exprs;
  }

  /**
   * for all reachable signatures in the module, convert their inline facts to regular facts of the module
   * 
   * @param m
   * @return the transformed module
   */
  public static CompModule globalizeInlineFacts(CompModule m) {
    for (Sig s : m.getAllReachableSigs()) {
      for (Expr fact : s.getFacts()) {
        int i = 0;
        for (Expr f : flatten(fact)) {
          Expr globalFact = null;
          if (s.isOne == null) {
            globalFact = f.forAll(s.decl);
            // reset pos to that of original inline facts
            globalFact.pos = f.pos;
          } else {
             ExprLet letExpr = (ExprLet) ExprLet.make(f.pos, (ExprVar) (s.decl.get()), s, f);
             // override the span of the let expression to be the same as the original fact
             letExpr.setSpan(f.span());
             globalFact = letExpr;
          }
          System.out.println(globalFact.pos.toRangeString() + globalFact.span().toRangeString());
          String name = "inline_" + s.label + i++;
          m.facts.add(new Pair<String, Expr>(name, globalFact));
        }
      }
      s.clearFacts();
    }
    return m;
  }

  public static List<Expr> flatten(Expr exp) {
    return flattenR((new ConvToConjunction()).visitThis(exp));
  }

  /**
   * Flatten the given expression into a list of conjuncts.
   * 
   * @param exp
   * @return
   */
  public static List<Expr> flattenR(Expr exp) {
    List<Expr> exps = new ArrayList<>();
    if (exp instanceof ExprList) {
      ExprList el = (ExprList) exp;
      if (el.op == ExprList.Op.AND) {        
        for (Expr e : el.args) {
          exps.addAll(flattenR(e));
        }        
      } else {
        exps.add(exp);
      }
    } else if (exp instanceof ExprBinary) {
      ExprBinary eb = (ExprBinary) exp;
      if (eb.op == ExprBinary.Op.AND) {
        exps.addAll(flattenR(eb.left));
        exps.addAll(flattenR(eb.right));
      } else {
        exps.add(exp);
      }
    } else if (ExprConstant.TRUE.equals(exp)) {
      // nothing to do, list should be empty
    }  else if (exp instanceof ExprUnary) {
      ExprUnary eu = (ExprUnary) exp;
      if (eu.op == ExprUnary.Op.NOOP) {
        exps.addAll(flattenR(eu.sub));
      } else {
        exps.add(exp);
      }
    } else {
      exps.add(exp);
    }
    return exps;
  }


}
