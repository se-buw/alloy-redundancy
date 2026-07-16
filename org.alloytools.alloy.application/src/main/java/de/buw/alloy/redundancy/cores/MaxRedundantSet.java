package de.buw.alloy.redundancy.cores;

import java.util.List;
import java.util.Map;

import de.buw.alloy.redundancy.ModuleTransformer;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

/**
 * Find a maximal set of redundant conjuncts in a module.
 * 
 * A set of conjuncts is redundant if it is implied by the remaining module
 * constraints.
 * 
 * Since DDMin minimizes, we minimize the set of non-redundant conjuncts.
 * 
 */
public class MaxRedundantSet extends AbstractDdmin<Expr> {

  /** all conjuncts of the model */
  private List<Expr> all;
  private A4Options opt;
  private Module m;
  private List<Command> cmds;
  private Map<Command, Expr> pred;

  public MaxRedundantSet(A4Options opt, CompModule m, List<Command> cmds) {
    this.opt = opt;
    this.m = m;
    this.cmds = cmds;
    m = ModuleTransformer.globalizeInlineFacts(m);
    List<Expr> facts = ModuleTransformer.flatten(m.getAllReachableFacts());
    this.all = new ArrayList<>(facts);
    this.pred = new LinkedHashMap<>();
    for (Command c : cmds) {
      pred.put(c, extractPred(c.formula, facts));
    }
  }

  /**
   * Extract the predicate from the formula that is not in the facts.
   * 
   * @param formula
   * @param allFacts
   * @return
   */
  private Expr extractPred(Expr formula, List<Expr> allFacts) {
    List<Expr> formulaElems = ModuleTransformer.flatten(formula);
    formulaElems.removeAll(allFacts);
    if (formulaElems.isEmpty()) {
      return ExprConstant.TRUE;
    } else if (formulaElems.size() == 1) {
      return formulaElems.get(0);
    }
    return ExprList.make(formula.pos, null, ExprList.Op.AND, formulaElems);
  }

  public List<Expr> getMaxRedundantSet() {
    List<Expr> antecedent = super.minimize(all);
    List<Expr> redundant = new java.util.ArrayList<Expr>(all);
    redundant.removeAll(antecedent);
    return redundant;
  }


  @Override
  protected boolean check(List<Expr> part) {
    List<Expr> candRedundant = new java.util.ArrayList<Expr>(all);
    candRedundant.removeAll(part);
    return checkSemanticallyEntailed(part, candRedundant);
  }

  /**
   * Check for the current module, whether the given left semantically entails the
   * right. If right is empty, it is trivially entailed.
   * 
   * @param left
   * @param right
   * @return
   */
  private boolean checkSemanticallyEntailed(List<Expr> left, List<Expr> right) {
    if (right.isEmpty()) {
      return true;
    }
    Expr leftExpr = ExprList.make(null, null, ExprList.Op.AND, left);
    Expr rightExpr = ExprList.make(null, null, ExprList.Op.AND, right);
    Expr entailExp = leftExpr.and(rightExpr.not());
    for (Command c : cmds) {
      // add predicate of the command (run or check treated equally after preprocessing)
      Expr pred = this.pred.get(c);
      Expr entailExpWC = pred.and(entailExp);

      // check entailment and return as soon as not entailed
      A4Solution ans = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, m.getAllReachableSigs(),
          c.change(entailExpWC), opt);
      boolean entailed = !ans.satisfiable();
      if (!entailed) {
        return false;
      }
    }
    // all commands entailed the right side
    return true;
  }

}
