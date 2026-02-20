package de.buw.alloy.redundancy;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import kodkod.engine.satlab.SATFactory;

public class WarmUpAlloy {

  public static A4Options getOptions() {
    A4Options opt = new A4Options();
    String osName = System.getProperty("os.name").toLowerCase();
    opt.solver = osName.contains("linux") ? SATFactory.get("minisat") : SATFactory.DEFAULT;
    return opt;
  }

  public static void warmUp() {
    A4Options opt = getOptions();
    CompModule m = CompUtil.parseEverything_fromString(A4Reporter.NOP, prisoners);
    TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, m.getAllReachableSigs(),
        m.getAllCommands().get(0), opt);
  }


  public static final String prisoners = "\n" + //
    "open util/ordering[State]\n" + //
    "open util/integer\n" + //
    "\n" + //
    "/*Define Players*/\n" + //
    "abstract sig Prisoner {}\n" + //
    "sig OtherPrisoner extends Prisoner{}\n" + //
    "one sig CounterPrisoner extends Prisoner {}\n" + //
    "one sig NULL{}\n" + //
    "\n" + //
    "fact { Prisoner = OtherPrisoner \n" + //
    "  + CounterPrisoner }\n" + //
    "fact { #Prisoner > 1 }\n" + //
    "\n" + //
    "/*Define Boolean*/\n" + //
    "abstract sig Bool{}\n" + //
    "one sig True extends Bool {}\n" + //
    "one sig False extends Bool {}\n" + //
    "\n" + //
    "/*Define Switches*/\n" + //
    "abstract sig Switches{}\n" + //
    "one sig SwitcheA extends Switches{}\n" + //
    "one sig SwitcheB extends Switches{}\n" + //
    "fact { Switches = SwitcheA + SwitcheB }\n" + //
    "\n" + //
    "/*Define Status*/\n" + //
    "abstract sig Status{}\n" + //
    "one sig Up extends Status {}\n" + //
    "one sig Down extends Status {}\n" + //
    "\n" + //
    "/*Define State*/\n" + //
    "sig State {announced:Bool,\n" + //
    "\t       SwitchesStatus: Switches->one Status,\n" + //
    "\t       count:Int,\n" + //
    "\t       timesSwitched: OtherPrisoner ->one  Int,\n" + //
    "\t       currentPrisoner: one (Prisoner+NULL)\n" + //
    "}\n" + //
    "\n" + //
    "/*Define initial state*/\n" + //
    "pred TineSwichedSetToZero{all p:OtherPrisoner{ p->0 in first.timesSwitched}}\n" + //
    "pred CountSetToZero{first.count=0}\n" + //
    "pred SwitchesInBoolean{all s:Switches{ (s->Down in first.SwitchesStatus) or (s->Up in first.SwitchesStatus)}}\n" + //
    "pred AnnouncedSetToFalse{ first.announced = False}\n" + //
    "pred CurrentPlayerSetToNull {first.currentPrisoner = NULL}\n" + //
    "\n" + //
    "fact Init{TineSwichedSetToZero and CountSetToZero and SwitchesInBoolean and AnnouncedSetToFalse and CurrentPlayerSetToNull}\n" + //
    "\n" + //
    "pred NonCounterStep[game, game\": State,p:Prisoner]{\n" + //
    "\tp in OtherPrisoner\n" + //
    "\tgame\".currentPrisoner = p\n" + //
    "\tgame\".announced = game.announced\n" + //
    "\tgame\".count = game.count\n" + //
    "\t(game.announced= True =>\n" + //
    "\t\tgame\".SwitchesStatus = game.SwitchesStatus\n" + //
    "\t\tand game\".timesSwitched = game.timesSwitched\n" + //
    "\telse\n" + //
    "\t\t((game.SwitchesStatus[SwitcheA] = Down and p.(game.timesSwitched) <2) =>\n" + //
    "\t\t\tSwitcheA.(game\".SwitchesStatus) = Up\n" + //
    "\t\t\tand SwitcheB.(game\".SwitchesStatus) = SwitcheB.(game.SwitchesStatus)\n" + //
    "\t\t\tand game\".timesSwitched = game.timesSwitched - p->p.(game.timesSwitched) + p->(p.(game.timesSwitched)+1)\n" + //
    "\t\telse\n" + //
    "\t\t\tgame\".timesSwitched = game.timesSwitched\n" + //
    "\t\t\tand SwitcheA.(game\".SwitchesStatus) = SwitcheA.(game.SwitchesStatus)\n" + //
    "\t\t\tand (SwitcheB.(game.SwitchesStatus) = Up=>\n" + //
    "\t\t\t\tSwitcheB.(game\".SwitchesStatus) = Down\n" + //
    "\t\t\telse\n" + //
    "\t\t\t\tSwitcheB.(game\".SwitchesStatus) = Up)))\n" + //
    "}\n" + //
    "\n" + //
    "pred CounterStep[game, game\": State, p:Prisoner]{\n" + //
    "\tp = CounterPrisoner\n" + //
    "\tgame\".currentPrisoner = p\n" + //
    "\tgame\".timesSwitched = game.timesSwitched\n" + //
    "\t(game.announced= True =>\n" + //
    "\t\tgame\".SwitchesStatus = game.SwitchesStatus\n" + //
    "\t\tand game\".announced = game.announced\n" + //
    "\t\tand game\".count =game.count\n" + //
    "\telse\n" + //
    "\t\t(SwitcheA.(game.SwitchesStatus) = Up =>\n" + //
    "\t\t\tSwitcheA.(game\".SwitchesStatus) = Down\n" + //
    "\t\t\tand SwitcheB.(game\".SwitchesStatus) = SwitcheB.(game.SwitchesStatus)\n" + //
    "\t\t\tand game\".count =game.count +1\n" + //
    "\t\t\tand (game\".count = 2.mul[(#Prisoner-1)] =>\n" + //
    "\t\t\t\tgame\".announced = True\n" + //
    "\t\t\telse\n" + //
    "\t\t\t\tgame\".announced = game.announced)\n" + //
    "\t\telse\n" + //
    "\t\t\tgame\".count = game.count\n" + //
    "\t\t\tand game\".announced = game.announced\n" + //
    "\t\t\tand SwitcheA.(game\".SwitchesStatus) = SwitcheA.(game.SwitchesStatus)\n" + //
    "\t\t\tand (SwitcheB.(game.SwitchesStatus) = Up=>\n" + //
    "\t\t\t\tSwitcheB.(game\".SwitchesStatus) = Down\n" + //
    "\t\t\telse\n" + //
    "\t\t\t\tSwitcheB.(game\".SwitchesStatus) = Up)))\n" + //
    "}\n" + //
    "\n" + //
    "fact Steps{\n" + //
    "\t\tall s: State, s\": s.next {\n" + //
    "\t\t\t(one p:OtherPrisoner | NonCounterStep[s, s\",p])\n" + //
    "\t\t\t or (one p:CounterPrisoner | CounterStep[s, s\",p])\n" + //
    "\t\t}\n" + //
    "}\n" + //
    "\n" + //
    "/*Checking types*/\n" + //
    "assert  TypeOK {all s:State{\n" + //
    "\t\t\ts.count >=0\n" + //
    "\t\t\tand s.count<= 2.mul[(#Prisoner-1)]\n" + //
    "\t\t\tand  (all p:OtherPrisoner| p.(s.timesSwitched) <=2)\n" + //
    "\t}\n" + //
    "}\n" + //
    "check TypeOK for 3 Prisoner, 12 State\n" + //
    "\n" + //
    "/*Checking safety*/\n" + //
    "pred StateDone[s:State]{s.count =  2.mul[(#Prisoner-1)]}\n" + //
    "pred Announced[s:State]{s.announced = True}\n" + //
    "\n" + //
    "  /*(*************************************************************************)\n" + //
    "  (* This formula asserts that safety condition: that Done true implies    *)\n" + //
    "  (* that every prisoner other than the counter has flipped switch A at    *)\n" + //
    "  (* least once--and hence has been in the room at least once.  Since the  *)\n" + //
    "  (* counter increments the count only when in the room, and Done implies  *)\n" + //
    "  (* count > 0, it also implies that the counter has been in the room.*)\n" + //
    "  (*This is also checks the counter's announcement that all the prisoners was in the room if and only if it is true (means Done)  *)\n" + //
    "  (*************************************************************************)*/\n" + //
    "assert Safety{all s:State{\n" + //
    "\t\t\t(StateDone[s] =>\n" + //
    "\t\t\t\t(all p:OtherPrisoner| p.(s.timesSwitched)>0))\n" + //
    "\t\t\tand  (Announced[s] iff StateDone[s])\n" + //
    "\t}\n" + //
    "}\n" + //
    "check Safety for 3 Prisoner, 10 State\n" + //
    "\n" + //
    "/* Count always eaqual to the sum of timesSwitched of all OtherPrisoners(+-1)*/\n" + //
    "assert CountInvariant{all s:State {\n" + //
    "\t\t\t\t(let totalSwitched = (sum p:OtherPrisoner | p.(s.timesSwitched)) |\n" + //
    "\t\t\t\t(SwitcheA.(s.SwitchesStatus) = Up => \n" + //
    "\t\t\t\t\t((s.count = totalSwitched -1) or (s.count = totalSwitched))\n" + //
    "\t\t\t\telse\n" + //
    "\t\t\t\t\t((s.count = totalSwitched) or (s.count = totalSwitched +1))))\n" + //
    "\t}\n" + //
    "}\n" + //
    "\n" + //
    "check CountInvariant for 3 Prisoner, 10 State\n" + //
    "\n" + //
    "\n" + //
    "/*Checking fairness*/\n" + //
    "pred AfterNonCounterPlayerEventaullyCounterPlayertEnterTheRoom{\n" + //
    "\t\tall s: State|\n" + //
    "\t\t\t((s.currentPrisoner in OtherPrisoner) => \n" + //
    "\t\t\t\t(some s\": s.^next | s\".currentPrisoner = CounterPrisoner))\n" + //
    "}\n" + //
    "\n" + //
    "pred PrisonerComesImmediatelyAfterCounter[s: State, p:OtherPrisoner]{ \n" + //
    "\t\t\ts.currentPrisoner = CounterPrisoner and s.next.currentPrisoner = p\n" + //
    "}\n" + //
    "\n" + //
    "pred Fairness {(all p:OtherPrisoner{ \n" + //
    "\t\t\tsome s,s\":State {s\" in s.^next\n" + //
    "\t\t\t\t\tand PrisonerComesImmediatelyAfterCounter[s,p] \n" + //
    "\t\t\t\t\tand PrisonerComesImmediatelyAfterCounter[s\",p]\n" + //
    "\t\t\t}\n" + //
    "\t\t})\n" + //
    "\t\tAfterNonCounterPlayerEventaullyCounterPlayertEnterTheRoom\n" + //
    "}\n" + //
    "\n" + //
    "pred Done{some s:State | Announced[s]}\n" + //
    "\n" + //
    "assert Theorem{Fairness => Done}\n" + //
    "check Theorem for 3 Prisoner, 12 State\n" + //
    "\n" + //
    "run {} for 4 int, exactly 3 Prisoner, 12 State\n" + //
    "";
}
