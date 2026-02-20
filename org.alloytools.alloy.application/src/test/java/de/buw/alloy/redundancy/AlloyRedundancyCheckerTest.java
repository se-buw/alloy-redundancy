package de.buw.alloy.redundancy;


import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.ast.Module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

public class AlloyRedundancyCheckerTest {
  @Test
  public void testCheckHasInherentVacuity() {
    AlloyRedundancyChecker ivc = new AlloyRedundancyChecker();
    assertFalse(ivc.checkHasInherentVacuity("src/test/resources/iv0.als"));
    assertFalse(ivc.checkHasInherentVacuity("src/test/resources/iv1.als"));
    assertTrue(ivc.checkHasInherentVacuity("src/test/resources/iv2.als"));
    assertTrue(ivc.checkHasInherentVacuity("src/test/resources/iv3.als"));
    assertFalse(ivc.checkHasInherentVacuity("src/test/resources/iv4.als"));
    assertFalse(ivc.checkHasInherentVacuity("src/test/resources/iv5.als"));
    assertTrue(ivc.checkHasInherentVacuity("src/test/resources/iv6.als"));

    assertTrue(ivc.checkHasInherentVacuity("src/test/resources/core.als"));
    assertTrue(ivc.checkHasInherentVacuity("src/test/resources/misc/triviallyRedundant.als"));
  }

  @Test
  public void testCheckHasInherentVacuityCheckCommand() {
    AlloyRedundancyChecker ivc = new AlloyRedundancyChecker();
    assertTrue(ivc.checkHasInherentVacuity("src/test/resources/addressBook3d.als"));
    assertTrue(ivc.checkHasInherentVacuity("src/test/resources/iv8.als"));
  }

  @Test
  public void testGetElems() {
    assertEquals(0, getElemsFromSpec("src/test/resources/iv0.als").size());
    assertEquals(1, getElemsFromSpec("src/test/resources/iv1.als").size());
    assertEquals(2, getElemsFromSpec("src/test/resources/iv2.als").size());
    assertEquals(2, getElemsFromSpec("src/test/resources/iv3.als").size());
    assertEquals(2, getElemsFromSpec("src/test/resources/iv4.als").size());
    assertEquals(2, getElemsFromSpec("src/test/resources/iv5.als").size());
    assertEquals(3, getElemsFromSpec("src/test/resources/iv6.als").size());
    assertEquals(3, getElemsFromSpec("src/test/resources/iv8.als").size());

    assertEquals(3, getElemsFromSpec("src/test/resources/core.als").size());
    assertEquals(3, getElemsFromSpec("src/test/resources/addressBook3d.als").size());
  }

  @Test
  public void testGetElemsInlineFact() {
    // FIXME inline facts are not supported yet, not sure how they are mixed into
    // the formula
    assertEquals(0, getElemsFromSpec("src/test/resources/iv7.als").size());
  }

  @Ignore
  @Test
  public void findInhVacSpecs() throws IOException {
    AlloyRedundancyChecker ivc = new AlloyRedundancyChecker();
    // iterate over all files in models/ and all subfolders and check if they have
    // inherent vacuity
    Files.find(Paths.get("src/test/resources/models"), 999,
        (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().matches(".*\\.als")).forEach(x -> {
          try {
            ivc.checkHasInherentVacuity(x.toString());
          } catch (Exception e) {
            // System.out.println("Exception while checking " + x);
          }
        });
  }

  @Ignore
  @Test
  public void computeInhVacSpecsSizesEachAlone() throws IOException {
    computeInhVacSpecsSizes("src/test/resources/models", true);
  }

  @Ignore
  @Test
  public void computeInhVacSpecsSizesGlobal() throws IOException {
    computeInhVacSpecsSizes("src/test/resources/models", false);
  }

  @Ignore
  @Test
  public void computeInhVacSpecsSizesGlobalBenschmarkSpecs() throws IOException {
    computeInhVacSpecsSizes("dataset/", false);
  }

  private void computeInhVacSpecsSizes(String path, boolean eachCmdAlone) throws IOException {
    int timeout = 30;

    AlloyRedundancyChecker ivc = new AlloyRedundancyChecker();
    // iterate over all files in models/ and all subfolders and check if they have
    // inherent vacuity
    Files.find(Paths.get(path), 999,
        (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().matches(".*\\.als")).forEach(x -> {
          try {
            System.out.println("Checking " + x);
            CompletableFuture.runAsync(() -> computeInhVacSpecsSize(ivc, x, eachCmdAlone))
                .orTimeout(timeout, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                  System.err.println("Timeout while checking " + x);
                  return null;
                }).get();
          } catch (Exception e) {
            System.err.println(e.getMessage() + " while checking " + x);
          }
        });
  }

  private void computeInhVacSpecsSize(AlloyRedundancyChecker ivc, Path x, boolean eachCmdAlone) {
    try {
      CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, x.toString());
      if (eachCmdAlone) {
        for (Command c : m.getAllCommands()) {
          List<Expr> maxSet = ivc.maxRedundantSet(m, c);
          if (maxSet.size() > 0) {
            System.out.println(x + " " + c.label + " " + maxSet.size());
            for (Expr e : maxSet) {
              System.out.println("\t" + e.pos + " " + e);
            }
          }
        }
      } else {
        List<Expr> maxSet = ivc.maxRedundantSet(m);
        if (maxSet.size() > 0) {
          System.out.println(x + " redundant elements: " + maxSet.size());
          for (Expr e : maxSet) {
            System.out.println("\t" + e.pos + " " + e);
          }
        }
      }      
    } catch (Err e) {
      System.err.println("Alloy Err while checking " + x);
    } catch (Exception e) {
      System.err.println(e.getMessage() + " while checking " + x);
    }
  }

  @Test
  public void testCheckHasInherentVacuityCheckPrisoner() {
    AlloyRedundancyChecker ivc = new AlloyRedundancyChecker();
    List<Expr> maxSet = ivc.maxRedundantSet("src/test/resources/models/puzzles/prisoner-room-visit/prisoner.als");
    assertEquals(3, maxSet.size());
  }

  private List<Expr> getElemsFromSpec(String fileName) {
    Module m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, fileName);
    Command c = m.getAllCommands().get(0);
    return ModuleTransformer.flatten(c.formula);
  }

  @Test
  public void testMaxRedundantSet() {
    AlloyRedundancyChecker ivc = new AlloyRedundancyChecker();

    // specs with no inherent vacuity
    assertEquals(0, ivc.maxRedundantSet("src/test/resources/iv0.als", 0).size());
    assertEquals(0, ivc.maxRedundantSet("src/test/resources/iv1.als", 0).size());

    // max sets of size 1 of 2
    assertEquals(1, ivc.maxRedundantSet("src/test/resources/iv2.als", 0).size());
    assertEquals(1, ivc.maxRedundantSet("src/test/resources/iv3.als", 0).size());

    // max 2 of 3
    assertEquals(2, ivc.maxRedundantSet("src/test/resources/iv6.als", 0).size());

    // max 2 of 3
    assertEquals(2, ivc.maxRedundantSet("src/test/resources/iv9.als", 0).size());

    assertEquals(0, ivc.maxRedundantSet("src/test/resources/models/simple-models/books/birthday.als", 0).size());

    assertEquals(2, ivc.maxRedundantSet("src/test/resources/iv10.als", 0).size());

  }

  @Test
  public void testTemporalSpecs() {
    AlloyRedundancyChecker ivc = new AlloyRedundancyChecker();

    assertEquals(0, ivc.maxRedundantSet("src/test/resources/tiv0.als", 0).size());
    assertEquals(1, ivc.maxRedundantSet("src/test/resources/tiv1.als", 0).size());
  }
}