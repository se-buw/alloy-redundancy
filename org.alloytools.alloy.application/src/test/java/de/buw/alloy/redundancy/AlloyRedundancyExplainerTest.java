package de.buw.alloy.redundancy;

import org.junit.Test;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

public class AlloyRedundancyExplainerTest {

  @Test
  public void testExplainRedundancy() {
    explainRedundancy("src/test/resources/iv1.als", 0, 0, -1);
    explainRedundancy("src/test/resources/iv2.als", 0, 0, 1);
    explainRedundancy("src/test/resources/iv2.als", 0, 1, 1);
    explainRedundancy("src/test/resources/iv3.als", 0, 0, 1);    
    explainRedundancy("src/test/resources/iv3.als", 0, 1, -1);

    explainRedundancy("src/test/resources/iv9.als", 0, 0, 1);
    explainRedundancy("src/test/resources/iv9.als", 0, 1, 1);
    explainRedundancy("src/test/resources/iv9.als", 0, 2, 1);

    explainRedundancy("src/test/resources/misc/emptyExplanation.als", 0, 0, 0);
  }

  private void explainRedundancy(String fileName, int cmd, int expr, int expectedSize) {
    CompModule m = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, fileName);
    AlloyRedundancyChecker explainer = new AlloyRedundancyChecker();
    m = ModuleTransformer.globalizeInlineFacts(m);
    Command c = m.getAllCommands().get(cmd);
    List<Expr> all = ModuleTransformer.flatten(c.formula);

    List<Expr> explanation = explainer.explainRedundancy(m, null, all, all.get(expr));
    System.out.println("Explanation for " + all.get(expr) + " in " + all.get(expr).span().toShortString());
    if (explanation == null) {
      if (expectedSize != -1) {
        fail("No explanation possible.");
      }
    } else if (explanation.isEmpty()) {
      System.out.println("Empty explanation: constraint is entailed by model structure.");
    } else {
      for (Expr e : explanation) {
        System.out.println(e + " in " + e.span().toShortString());
      }
      assertEquals(expectedSize, explanation.size());
    }
  }
}