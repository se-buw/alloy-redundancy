abstract sig Prisoner {
  cellMate : some Prisoner - this,
--  cellMate : set univ,
}
--fact {all p : Prisoner | some p.cellMate and p.cellMate in (Prisoner - p)}

sig OtherPrisoner extends Prisoner{}
one sig CounterPrisoner extends Prisoner {}

fact { # Prisoner > 1}

run {} for 100
