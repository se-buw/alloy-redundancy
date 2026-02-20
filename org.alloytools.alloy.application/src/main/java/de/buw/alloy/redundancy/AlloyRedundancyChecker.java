package de.buw.alloy.redundancy;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import de.buw.alloy.redundancy.cores.MaxRedundantSet;
import de.buw.alloy.redundancy.cores.MinEntailingSet;
import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import kodkod.engine.satlab.SATFactory;

public class AlloyRedundancyChecker {
  private A4Options opt;
  private A4Reporter rep = A4Reporter.NOP;
  private static final String osName = System.getProperty("os.name").toLowerCase();

  public AlloyRedundancyChecker() {
    this.opt = new A4Options();
    opt.solver = osName.contains("linux") ? SATFactory.get("minisat") : SATFactory.DEFAULT;
  }

  public AlloyRedundancyChecker(A4Options opt) {
    this.opt = opt;
  }

  /**
   * Check the Alloy module for redundant constraints.
   * 
   * @param fileName
   * @param cmdId command id to check for redundancy (-1 for all commands)
   * @return list of redundant constraints
   */
  public List<Expr> redundantConstraints(String fileName, int cmdId) {
    CompModule m = CompUtil.parseEverything_fromFile(rep, null, fileName);
    m = ModuleTransformer.globalizeInlineFacts(m);
    
    List<Expr> redundantElems = new ArrayList<>();
    
    if (cmdId == -1) {
      List<Expr> elements = ModuleTransformer.flatten(m.getAllReachableFacts());
      for( int i = 0; i < elements.size(); i++) {
        boolean redundant = true;
        // check redundancy of element for all commands
        for (Command c : m.getAllCommands()) {
          if (redundant && !checkHasInherentVacuity(m, c, ModuleTransformer.flatten(c.formula), elements.get(i))) {
            redundant = false; // abort early if not redundant
          }                    
        }
        if (redundant) {
          redundantElems.add(elements.get(i));
        }
      }
    } else {
      // check each command independently
        Command c = m.getAllCommands().get(cmdId);
        List<Expr> elements = ModuleTransformer.flatten(c.formula);
        for (Expr e : elements) {
          if (checkHasInherentVacuity(m, c, elements, e)) {
            redundantElems.add(e);
          }
        }
      }
      return redundantElems;
  }

  /**
   * Check the Alloy module for inherent vacuity. This treats all commands
   * independently.
   * 
   * TODO would make sense to compare vacuous elements and only report those that
   * are vacuous in all commands, i.e., never needed.
   * 
   * When the command we check is a check command we would expect unsat
   * anyways; we would expect that the command is semantically entailed by the
   * rest, i.e., it is vacuous. We ignore the predicate of the check and check the
   * rest.
   * 
   * In case the command is a run command we could check it.
   * 
   * @param fileName
   * @return
   */
  public boolean checkHasInherentVacuity(String fileName) {
    Module m = CompUtil.parseEverything_fromFile(rep, null, fileName);
    for (Command c : m.getAllCommands()) {
      if (checkHasInherentVacuity(m, c)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the given Alloy module and command are inherently vacuous.
   */
  protected boolean checkHasInherentVacuity(Module m, Command c) {
    List<Expr> elements = ModuleTransformer.flatten(c.formula);
    for (Expr e : elements) {
      if (checkHasInherentVacuity(m, c, elements, e)) {
        System.out.println("Inherently Vacuous element in " + e.pos);
        return true;
      }
    }
    return false;
  }

  /**
   * Check if (elements - e) semantically entails e in this Alloy module.
   * 
   * Find an instance where (elements - e) is true and e is false, i.e., a & !e. If no such instance exists, then e is
   * inherently vacuous.
   * 
   */
  protected boolean checkHasInherentVacuity(Module m, Command c, List<Expr> elements, Expr e) {
    List<Expr> elementsMinusE = new java.util.ArrayList<Expr>(elements);
    elementsMinusE.remove(e);
    ExprList elemsMinusE = ExprList.make(e.pos, e.span(), ExprList.Op.AND, elementsMinusE);
    Expr vacExpr = elemsMinusE.and(e.not());

    A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, m.getAllReachableSigs(), c.change(vacExpr), opt);
    boolean vacuous = !ans.satisfiable();
    return vacuous;
  }

  public List<Expr> maxRedundantSet(String fileName, int cmdId) {
    CompModule m = CompUtil.parseEverything_fromFile(rep, null, fileName);
    Command c = m.getAllCommands().get(cmdId);
    return maxRedundantSet(m, c);
  }
  public List<Expr> maxRedundantSet(String fileName) {
    CompModule m = CompUtil.parseEverything_fromFile(rep, null, fileName);
    return maxRedundantSet(m);
  }

  /**
   * Find a maximal set of redundant conjuncts in a module.
   * 
   * A set of conjuncts is redundant if it is implied by the remaining module
   * constraints.
   */
  public List<Expr> maxRedundantSet(CompModule m, Command c) {
    MaxRedundantSet mrs = new MaxRedundantSet(opt, m, List.of(c));
    return mrs.getMaxRedundantSet();
  }

  public List<Expr> maxRedundantSet(CompModule m) {
    MaxRedundantSet mrs = new MaxRedundantSet(opt, m, m.getAllCommands());
    return mrs.getMaxRedundantSet();
  }

  /**
   * Explain why expression e is redundant in module m.
   * @param m the module
   * @param c the command that we want to explain redundancy for (null for global explanation, i.e., any command)
   * @param all all expressions in the module (e will be removed from this list)
   * @param e the expression to explain
   * @return minimal list of constraints from all that semantically entail e, 
   * null if e is not redundant, empty if e is redundant due to structural elements
   */
  public List<Expr> explainRedundancy(CompModule m, Command c, List<Expr> all, Expr e) {
    if (c == null) {
      // pick any command as null means we wanted a global explanation (any explanation will do)
      c = m.getAllCommands().get(0);
    }
    List<Expr> others = new ArrayList<>(all);
    others.addAll(ModuleTransformer.getCommandExprs(m, c));
    others.remove(e);

    MinEntailingSet mes = new MinEntailingSet(opt, m, c, e);      
    List<Expr> core = mes.minimize(others);
    return core;
  }

  /**
   * Explain why expression e is redundant in module m.
   * @param m the module
   * @param c the command that we want to explain redundancy for (null for global explanation, i.e., any command)
   * @param all all expressions in the module (e will be removed from this list)
   * @param e the expression to explain
   * @return minimal list of constraints from all that semantically entail e
   */
  public List<Pos> explainRedundancyNative(CompModule m, Command c, List<Expr> all, Expr e) {
    if (c == null) {
      // pick any command as null means we wanted a global explanation (any explanation will do)
      c = m.getAllCommands().get(0);
    }
    List<Expr> others = new ArrayList<>(all);
    others.addAll(ModuleTransformer.getCommandExprs(m, c));
    others.remove(e);

    SATFactory old = opt.solver;
    opt.solver = SATFactory.get("minisat.prover");

    // remove e from m, negate e and add it to m, compute a core
    Expr unsat;
    if (others.isEmpty()) {
      unsat = e.not();
    } else {
      unsat = ExprList.make(null, null, ExprList.Op.AND, others);
      unsat = unsat.and(e.not());
    }
    Command cmd = c.change(unsat);
    A4Solution ans = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, m.getAllReachableSigs(),
      cmd, opt);
    if (ans.satisfiable()) {
      opt.solver = old;
      return null;
    } else {
      // extract the core from the answer
      opt.solver = old;
      return new ArrayList<Pos>(ans.highLevelCore().a);
    }
  }

  private Expr selectedConstraint;

  /**
   * Explain why the given position is redundant in the given file.
   * 
   * Finds the corresponding constraint (at Pos p) and then computes the minimal set of
   * constraints that semantically entail it.
   * 
   * @param fileName
   * @param p the position to explain redundancy for
   * @return minimal list of constraints that semantically entail the given position. 
   * null if the position is not in any constraint.
   */
  public List<Expr> explainRedundancy(String fileName, Pos p) {
    selectedConstraint = null;
    // convert module to expressions
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, fileName);
    m = ModuleTransformer.globalizeInlineFacts(m);
    List<Expr> constraints = ModuleTransformer.flatten(m.getAllReachableFacts());
    // find the constraints that contain the position
    for (Expr c : constraints) {
      // FIXME does not work for function calls, e.g., referenced predicates
      if (c.pos.contains(p) || c.span().contains(p)) {
        // compute the explanation
        selectedConstraint = c;
        return explainRedundancy(m, null, constraints, c);
      }
    }

    // next, iterate over all commands and check whether the Pos p is in the command
    for (Command c : m.getAllCommands()) {
      // all constraints in the command
      List<Expr> all = ModuleTransformer.flatten(c.formula);
      List<Expr> cmdConstraints = new ArrayList<>(all);
      // the ones specific to the command
      cmdConstraints.removeAll(constraints);
      for (Expr cmdC : cmdConstraints) {
        // FIXME does not work for function calls, e.g., referenced predicates (not top level expressions)
        if (cmdC.pos.contains(p) || cmdC.span().contains(p)) {
          // compute the explanation
          selectedConstraint = cmdC;
          return explainRedundancy(m, c, all, cmdC);
        }
      }
    }
    
    return null;
  }

  public List<Pos> explainRedundancyNative(String fileName, Pos p) {
    selectedConstraint = null;
    // convert module to expressions
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, fileName);
    m = ModuleTransformer.globalizeInlineFacts(m);
    List<Expr> constraints = ModuleTransformer.flatten(m.getAllReachableFacts());
    // find the constraints that contain the position
    for (Expr c : constraints) {
      // FIXME does not work for function calls, e.g., referenced predicates
      if (c.pos.contains(p) || c.span().contains(p)) {
        // compute the explanation
        selectedConstraint = c;
        return explainRedundancyNative(m, null, constraints, c);
      }
    }

    // next, iterate over all commands and check whether the Pos p is in the command
    for (Command c : m.getAllCommands()) {
      // all constraints in the command
      List<Expr> all = ModuleTransformer.flatten(c.formula);
      List<Expr> cmdConstraints = new ArrayList<>(all);
      // the ones specific to the command
      cmdConstraints.removeAll(constraints);
      for (Expr cmdC : cmdConstraints) {
        // FIXME does not work for function calls, e.g., referenced predicates (not top level expressions)
        if (cmdC.pos.contains(p) || cmdC.span().contains(p)) {
          // compute the explanation
          selectedConstraint = cmdC;
          return explainRedundancyNative(m, c, all, cmdC);
        }
      }
    }
    
    return null;
  }

  public Expr getSelectedConstraint() {
    return selectedConstraint;
  }

}
