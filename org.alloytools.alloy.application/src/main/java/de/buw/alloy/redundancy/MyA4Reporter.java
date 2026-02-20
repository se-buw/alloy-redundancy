package de.buw.alloy.redundancy;

import edu.mit.csail.sdg.alloy4.A4Reporter;

public class MyA4Reporter extends A4Reporter {
  public long solvingTime = 0;
  public long primSatVars = 0;
  public long satVars = 0;

  @Override
  public void resultUNSAT(Object command, long solvingTime, Object solution) {
    this.solvingTime = solvingTime;
    super.resultUNSAT(command, solvingTime, solution);
  }

  @Override
  public void resultSAT(Object command, long solvingTime, Object solution) {
    this.solvingTime = solvingTime;
    super.resultSAT(command, solvingTime, solution);
  }

  @Override
  public void solve(int plength, int primaryVars, int totalVars, int clauses) {
    this.primSatVars = primaryVars;
    this.satVars = totalVars;
    super.solve(plength, primaryVars, totalVars, clauses);
  }
}
