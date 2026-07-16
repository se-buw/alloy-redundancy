package de.buw.alloy.redundancy;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AlloyInhvac {
  public static void main(String[] args) throws IOException {
    AlloyInhvac aiv = new AlloyInhvac();
    if (args.length < 2) {
      System.out.println("Please provide the path to the Alloy models folder.");
      System.exit(0);
    }
    String path = args[0];
    System.out.println("Checking: " + path.toString());

    String report = aiv.computeInhVacSpecsSizes(path, false);
    try{
      Files.writeString(Paths.get(args[1]), report, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }catch(IOException e){
      System.out.println("Error writing to file");
    }
  }

  private String computeInhVacSpecsSizes(String path, boolean eachCmdAlone) throws IOException {
    int timeout = 360;
    Path x = Paths.get(path);
    StringBuilder result = new StringBuilder();
    result.append("Spec: " + x.toString() + "\n");

    AlloyRedundancyChecker ivc = new AlloyRedundancyChecker();
    final long start = System.currentTimeMillis();
    String computeResult = null; // Store the result from computeInhVacSpecsSize
    try {
      computeResult = CompletableFuture.supplyAsync(() -> computeInhVacSpecsSize(ivc, x, eachCmdAlone))
          .orTimeout(timeout, TimeUnit.SECONDS)
          .exceptionally(throwable -> {
            result.append(" " + throwable.getMessage() + "\n");
            return null;
          }).get();
    } catch (Exception e) {
      result.append(" " + e.getMessage() + "\n");
    }
    final long end = System.currentTimeMillis();
    result.append(computeResult);
    result.append("Time Taken: " + (end - start) / 1000.0 + "s\n");

    return result.toString();
  }

  private String computeInhVacSpecsSize(AlloyRedundancyChecker ivc, Path x, boolean eachCmdAlone) {
    String result = "";
    try {
      CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, x.toString());
      if (eachCmdAlone) {
        for (Command c : m.getAllCommands()) {
          List<Expr> maxSet = ivc.maxRedundantSet(m, c);
          if (maxSet.size() > 0) {
            result += " " + c.label + " " + maxSet.size() + "\n";
            for (Expr e : maxSet) {
              result += "\t" + e.pos + " " + e + "\n";
            }
          }
        }
      } else {
        List<Expr> maxSet = ivc.maxRedundantSet(m);
        if (maxSet.size() > 0) {
          result += "redundant elements: " + maxSet.size() + "\n";
          for (Expr e : maxSet) {
            result += "\t" + e.pos + " " + e + "\n";
          }
        }
      }
    } catch (Err e) {
      result += " Alloy Err " + e.getClass().getName() + "\n";
    } catch (Exception e) {
      result += " " + e.getMessage() + "\n";
    }

    return result;
  }
}
