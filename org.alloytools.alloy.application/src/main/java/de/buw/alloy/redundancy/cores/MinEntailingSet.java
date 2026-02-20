package de.buw.alloy.redundancy.cores;

import java.util.List;

import java.util.ArrayList;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

/**
 * Find a minimal set of conjuncts to semantically entail the given constraint.
 * 
 * A constraint is semantically entailed if we cannot find an instance
 * that satisfies the set and violates the constraint.
 * 
 * Minimization returns null if the given elements do not entail the constraint.
 * 
 */
public class MinEntailingSet extends AbstractDdmin<Expr> {

  /** constraint to semantically entail */
  private Expr cons;
  private A4Options opt;
  private Module m;
  /** command to use for this check (will only use scope and nothing else) */
  private Command cmd;

  /**
   * Create a new instance of the minimal entailing set.
   * 
   * @param opt solver options
   * @param m module
   * @param cmd command to use for this check (will only use scope and nothing else)
   * @param cons constraint to entail
   */
  public MinEntailingSet(A4Options opt, Module m, Command cmd, Expr cons) {
    this.opt = opt;
    this.m = m;
    this.cmd = cmd;
    this.cons = cons;
  }

  @Override
  public List<Expr> minimize(List<Expr> elements) {
    if (check(new ArrayList<>())) {
      return new ArrayList<>();
    }
    if (!check(elements)) {
      return null;
    }
    return super.minimize(elements);
  }

  @Override
  protected boolean check(List<Expr> part) {
    Expr entailExp;
    if (part.isEmpty()) {
      entailExp = cons.not();
    } else {
      Expr leftExpr = ExprList.make(null, null, ExprList.Op.AND, part);
      entailExp = leftExpr.and(cons.not());
    }
    
    // check entailment 
    A4Solution ans = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, m.getAllReachableSigs(),
        cmd.change(entailExp), opt);

    boolean entailed = !ans.satisfiable();
    if (!entailed) {
      return false;
    }    
    return true;
  }

}
