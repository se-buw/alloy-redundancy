abstract sig Prisoner {}
sig OtherPrisoner extends Prisoner{}
one sig CounterPrisoner extends Prisoner {}

fact { Prisoner = OtherPrisoner + CounterPrisoner }

run {}
