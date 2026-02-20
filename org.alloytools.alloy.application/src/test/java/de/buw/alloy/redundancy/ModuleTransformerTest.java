package de.buw.alloy.redundancy;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;

public class ModuleTransformerTest {

  @Test
  public void testGlobalizeInlineFactsNone() {
    int inlineFacts = 0;
    String fileName = "src/test/resources/iv0.als";
    assertGlobalFactsAdded(inlineFacts, fileName);
  }

  @Test
  public void testGlobalizeInlineFactsOne() {
    int inlineFacts = 1;
    String fileName = "src/test/resources/misc/inlineFacts.als";
    assertGlobalFactsAdded(inlineFacts, fileName);
  }

  @Test
  public void testGlobalizeInlineFactsTwo() {
    int inlineFacts = 2;
    String fileName = "src/test/resources/misc/inlineFacts2.als";
    assertGlobalFactsAdded(inlineFacts, fileName);
  }

  @Test
  public void testGlobalizeInlineFactsThree() {
    int inlineFacts = 2;
    String fileName = "src/test/resources/misc/inlineFacts3.als";
    assertGlobalFactsAdded(inlineFacts, fileName);
  }

  private void assertGlobalFactsAdded(int inlineFacts, String fileName) {
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, fileName);
    int facts = m.facts.size();
    m = ModuleTransformer.globalizeInlineFacts(m);
    assertEquals(facts + inlineFacts, m.facts.size());
  }

  @Ignore
  @Test
  public void testCheckHasInherentVacuityCheckPrisoner() {
    String fileName = "src/test/resources/models/puzzles/prisoner-room-visit/prisoner.als";
    int expectRedudant = 3;
    globalizeAndCheck(fileName, expectRedudant);
  }

  @Test
  public void testGlobalizeAndCheck() {
    String fileName = "src/test/resources/misc/inlineFacts.als";
    int expectRedudant = 1;
    globalizeAndCheck(fileName, expectRedudant);
  }

  @Test
  public void testGlobalizeAndCheck2() {
    String fileName = "src/test/resources/misc/inlineFacts2.als";
    int expectRedudant = 1;
    globalizeAndCheck(fileName, expectRedudant);
  }

  @Test
  public void testGlobalizeAndCheck3() {
    String fileName = "src/test/resources/misc/inlineFacts3.als";
    int expectRedudant = 1;
    globalizeAndCheck(fileName, expectRedudant);
  }

  private void globalizeAndCheck(String fileName, int expectRedudant) {
    AlloyRedundancyChecker ivc = new AlloyRedundancyChecker();
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, fileName);
    m = ModuleTransformer.globalizeInlineFacts(m);
    List<Expr> maxSet = ivc.maxRedundantSet(m);
    assertEquals(expectRedudant, maxSet.size());
  }

}