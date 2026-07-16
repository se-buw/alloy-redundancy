abstract sig Prisoner {
  cellMate : some Prisoner - this,
}

sig OtherPrisoner extends Prisoner{}
one sig CounterPrisoner extends Prisoner {}

fact { # Prisoner > 1}

run {}
