/*Define Switches*/
abstract sig Switches{}
sig SwitcheA extends Switches{}
sig SwitcheB extends Switches{}
fact { Switches = SwitcheA + SwitcheB }
