package de.buw.alloy.redundancy;

import java.util.ArrayList;
import java.util.List;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class RedundancyMain {
  private static final String usage = "Arguments: <analysis> <model> [\"global\" or commandId] [constraintId or constraintGroup]\n"
      + "<analysis> = modelStats | check | maxRedSet | explain | explainNative | checkHigherOrderFail\n"
      + "\t modelStats: lines of code, number of sigs, number of facts, number of flattened facts, number of commands\n"
      + "\t check: number of redundant facts for each command, set of redundant facts, time taken\n"
      + "\t maxRedSet: number of redundant facts, set of redundant facts, time taken\n"
      + "\t explain: number of explaining facts, set of explaining facts, time taken, size of the set in characters\n"
      + "\t explainNative: number of explaining facts, set of explaining facts, time taken, size of the set in characters\n"
      + "\t checkHigherOrderFail: check for higher order quantification or similar errors in the given model\n"
      + "\t checkOverhead: compare the overhead of the redundant constraints added to the model\n";

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.out.println(usage);
      return;
    }
    String model = args[1];
    boolean global = args.length > 2 && args[2].equals("global");
    int command = -1; // same as "global"
    int constraint = -1; // undefined
    String constraintGroup = null;
    if (args.length == 4) {
      if (!global) {
        command = Integer.parseInt(args[2]);
      }
      try {
        constraint = Integer.parseInt(args[3]);
      } catch (NumberFormatException e) {
        // no problem, likely a constraint group
      }
      constraintGroup = args[3];
    }

    if (!"modelStats".equals(args[0])) {
      WarmUpAlloy.warmUp();
    }

    switch (args[0]) {
      case "modelStats":
        calcModelStats(model);
        break;
      case "checkUnsat":
        checkUnsat(model);
        break;
      case "check":
        checkForRedundancy(model, global);
        break;
      case "maxRedSet":
        calcMaxRedundantSet(model, global);
        break;
      case "explain":
        calcExplainRedundancy(model, command, constraint);
        break;
      case "explainNative":
        calcExplainRedundancyNative(model, command, constraint);
        break;
      case "checkHigherOrderFail":
        checkHigherOrderFail(model);
        break;
      case "checkOverhead":
        checkOverhead(model, command, constraintGroup);
        break;
      default:
      System.out.println(usage);
      return;
    }
  }

  private static void checkUnsat(String model) throws IOException {
    A4Options opt = WarmUpAlloy.getOptions();
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, model);
    for (int command = 0; command < m.getAllCommands().size(); command++) {
      Command cmd = m.getAllCommands().get(command);
      A4Solution ans = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, m.getAllReachableSigs(), cmd, opt);
      String sat = ans.satisfiable() ? "SAT" : "UNSAT";
      String kind = cmd.check ? "check" : "run";
      writeCsv("checkUnsat.csv", model, command, kind, sat);
    }
  }

  /**
   * compare the overhead of the redundant constraints added to the model
   * 
   * @param model
   * @param command
   * @param constraintGroup
   * @throws IOException 
   */
  private static void checkOverhead(String model, int command, String constraintGroup) throws IOException {
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, model);
    m = ModuleTransformer.globalizeInlineFacts(m);
    List<Expr> constraints = ModuleTransformer.flatten(m.getAllReachableFacts());
    List<Expr> rc = new ArrayList<>();
    for (String cId : constraintGroup.split("-")) {
      rc.add(constraints.get(Integer.parseInt(cId)));
    }
    if (command == -1) {
      command = 0; // global doesn't make sense here, we need to run some command
    }
    Command cmd = m.getAllCommands().get(command);
    A4Options opt = WarmUpAlloy.getOptions();
    List<Expr> cmdCons = ModuleTransformer.flatten(cmd.formula);
    cmdCons.addAll(constraints); // add potentially missing inlined facts
    Expr newCmdExp = cmdCons.size() == 0 ? ExprConstant.TRUE : cmdCons.size() == 1 ? cmdCons.get(0) : ExprList.make(null, null, ExprList.Op.AND, cmdCons);

    // check original model
    MyA4Reporter rep = new MyA4Reporter();
    TranslateAlloyToKodkod.execute_command(rep, m.getAllReachableSigs(), cmd.change(newCmdExp), opt);
    long timeOrig = rep.solvingTime;
    long primSatVarsOrig = rep.primSatVars;
    long satVarsOrig = rep.satVars;

    // check model without redundant constraints
    cmdCons.removeAll(rc); // remove redundant constraints
    Expr newCmdExpNoRed = cmdCons.size() == 0 ? ExprConstant.TRUE : cmdCons.size() == 1 ? cmdCons.get(0) : ExprList.make(null, null, ExprList.Op.AND, cmdCons);
    TranslateAlloyToKodkod.execute_command(rep, m.getAllReachableSigs(), cmd.change(newCmdExpNoRed), opt);
    long timeNoRed = rep.solvingTime;
    long primSatVarsNoRed = rep.primSatVars;
    long satVarsNoRed = rep.satVars;
    
    writeCsv("checkOverhead.csv", model, command, constraintGroup, timeOrig, timeNoRed, primSatVarsOrig, primSatVarsNoRed, satVarsOrig, satVarsNoRed);
  }

  /**
   * Check for higher order quantification or similar errors in the given model
   * 
   * @param model
   * @throws IOException 
   */
  private static void checkHigherOrderFail(String model) throws IOException {
    A4Options opt = WarmUpAlloy.getOptions();
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, model);
    for (Command cmd : m.getAllCommands()) {
      try {
        Command negCmd = cmd.change(cmd.formula.not());
        TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, m.getAllReachableSigs(), negCmd, opt);
        TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, m.getAllReachableSigs(), cmd, opt);
      } catch (Exception e) {
        writeCsv("higherOrderFail.csv", model, e.getMessage());
      }
    }
  }

  private static void calcExplainRedundancyNative(String model, int commandId, int constraintId) throws IOException {
    AlloyRedundancyChecker checker = new AlloyRedundancyChecker();
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, model);
    m = ModuleTransformer.globalizeInlineFacts(m);
    List<Expr> constraints = ModuleTransformer.flatten(m.getAllReachableFacts());

    if (commandId == -1) { // global
      long start = System.currentTimeMillis();
      List<Pos> expl = checker.explainRedundancyNative(m, null, constraints, constraints.get(constraintId));
      long time = System.currentTimeMillis() - start;
      writeCsv("explainNative.csv", model, commandId, constraintId, expl.size(), calcSizePos(expl, model), time);
    } else {
      Command cmd = m.getAllCommands().get(commandId);      
      long start = System.currentTimeMillis();
      List<Pos> expl = checker.explainRedundancyNative(m, cmd, constraints, constraints.get(constraintId));
      long time = System.currentTimeMillis() - start;
      writeCsv("explainNative.csv", model, commandId, constraintId, expl.size(), calcSizePos(expl, model), time);    
    }
  }

  /**
   * for each redundant constraint, compute the explanation using DDMin
   * Report: number of explaining facts, set of explaining facts, time taken, size of the set in characters
   * 
   * @param model
   * @throws IOException 
   */
  private static void calcExplainRedundancy(String model, int commandId, int constraintId) throws IOException {
    AlloyRedundancyChecker checker = new AlloyRedundancyChecker();
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, model);
    m = ModuleTransformer.globalizeInlineFacts(m);
    List<Expr> constraints = ModuleTransformer.flatten(m.getAllReachableFacts());

    if (commandId == -1) { // global
      long start = System.currentTimeMillis();
      List<Expr> expl = checker.explainRedundancy(m, null, constraints, constraints.get(constraintId));
      long time = System.currentTimeMillis() - start;
      writeCsv("explain.csv", model, commandId, constraintId, expl.size(), printSubset(expl, constraints), calcSize(expl, model), time);
    } else {
      Command cmd = m.getAllCommands().get(commandId);      
      long start = System.currentTimeMillis();
      List<Expr> expl = checker.explainRedundancy(m, cmd, constraints, constraints.get(constraintId));
      long time = System.currentTimeMillis() - start;
      writeCsv("explain.csv", model, commandId, constraintId, expl.size(), printSubset(expl, constraints), calcSize(expl, model), time);    
    }
  }

  /**
   * calculate the maximal redundant set of facts for the given model
   * Report: number of redundant facts, set of redundant facts, time taken, size of the maximal redundant set in characters
   * 
   * @param model
   * @throws IOException 
   */
  private static void calcMaxRedundantSet(String model, boolean global) throws IOException {
    AlloyRedundancyChecker checker = new AlloyRedundancyChecker();
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, model);
    m = ModuleTransformer.globalizeInlineFacts(m);
    List<Expr> constraints = ModuleTransformer.flatten(m.getAllReachableFacts());

    if (global) {
      long start = System.currentTimeMillis();
      List<Expr> mrs = checker.maxRedundantSet(m);
      long time = System.currentTimeMillis() - start;
      writeCsv("maxRedSet.csv", model, -1, mrs.size(), printSubset(mrs, constraints), calcSize(mrs, model), time);
    } else {
      for (int i = 0; i < m.getAllCommands().size(); i++) {
        Command cmd = m.getAllCommands().get(i);
        long start = System.currentTimeMillis();
        List<Expr> mrs = checker.maxRedundantSet(m, cmd);
        long time = System.currentTimeMillis() - start;
        writeCsv("maxRedSet.csv", model, i, mrs.size(), printSubset(mrs, constraints), calcSize(mrs, model), time);
      }
    }
  }

  /**
   * calculate the size of the given set of facts
   * @param mrs the set of facts
   * @param model the model file name
   * @return the size of the set in characters
   * @throws IOException
   */
  private static int calcSize(List<Expr> mrs, String model) throws IOException {
    List<Pos> poss = new ArrayList<>();
    for (Expr e : mrs) {
      poss.add(e.span());
    }
    return calcSizePos(poss, model);
  }

  /**
   * calculate the size of the given set of facts
   * @param mrs the set of facts
   * @param model the model file name
   * @return the size of the set in characters
   * @throws IOException
   */
  private static int calcSizePos(List<Pos> mrs, String model) throws IOException {
    String text = Files.readString(Paths.get(model));
    int size = 0;
    for (Pos p : mrs) {
     size += p.substring(text).length();
    }
    return size;
  }

  private static String printSubset(List<Expr> mrs, List<Expr> constraints) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < mrs.size(); i++) {
      for (int j = 0; j < constraints.size(); j++) {
        if (constraints.get(j).span().contains(mrs.get(i).pos)) {
          sb.append(j);
          sb.append("-");
          break;
        }
      }      
    }
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.length() - 1);
    }
    return sb.toString();
  }

  /**
   * Check for redundancy in the given model
   * Report the number of redundant facts for each command, also report set of redundant facts
   * report time taken
   * 
   * @param model
   * @throws IOException 
   */
  private static void checkForRedundancy(String model, boolean global) throws IOException {
    AlloyRedundancyChecker checker = new AlloyRedundancyChecker();
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, model);
    m = ModuleTransformer.globalizeInlineFacts(m);
    List<Expr> constraints = ModuleTransformer.flatten(m.getAllReachableFacts());

    if (global) {
      long start = System.currentTimeMillis();
      List<Expr> rc = checker.redundantConstraints(model, -1);
      long time = System.currentTimeMillis() - start;
      writeCsv("check.csv", model, -1, rc.size(), printSubset(rc, constraints), calcSize(rc, model), time);
    } else {
      for (int i = 0; i < m.getAllCommands().size(); i++) {
        Command cmd = m.getAllCommands().get(i);
        long start = System.currentTimeMillis();
        List<Expr> rc = checker.redundantConstraints(model, i);
        long time = System.currentTimeMillis() - start;
        writeCsv("check.csv", model, i, rc.size(), printSubset(rc, constraints), calcSize(rc, model), time);
      }
    }
  }

  /**
   * Compute statistics for the given model:
   * lines of code, number of sigs, number of facts, number of flattened facts, number of commands
   * @param model
   * @throws IOException 
   */
  private static void calcModelStats(String model) throws IOException {
    int numLines = Files.readAllLines(Paths.get(model)).size();

    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, model);
    int numSigs = m.getAllSigs().size();
    int numFacts = countFacts(m);
    int numCommands = m.getAllCommands().size();

    m = ModuleTransformer.globalizeInlineFacts(m);
    List<Expr> constraints = ModuleTransformer.flatten(m.getAllReachableFacts());
    int numFactsFlat = constraints.size();

    writeCsv("modelStats.csv", model, numLines, numSigs, numFacts, numFactsFlat, numCommands);    
  }

  /**
   * counts syntactic facts in all reachable modules (corresponding to those facts obtained in m.getAllReachableFacts()
   * 
   * @param m
   * @return
   */
  private static int countFacts(CompModule m) {
    int count = 0;
    for (CompModule rm : m.getAllReachableModules()) {
      count += rm.facts.size();
    }
    return count;
  }

  private static void writeCsv(String fileName, Object... values) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (Object v : values) {
      sb.append(v);
      sb.append(",");
    }
    sb.deleteCharAt(sb.length() - 1);
    sb.append("\n");
    String entry = sb.toString();
    Files.writeString(Paths.get(fileName), entry, StandardOpenOption.APPEND, StandardOpenOption.CREATE);     
  }
}
