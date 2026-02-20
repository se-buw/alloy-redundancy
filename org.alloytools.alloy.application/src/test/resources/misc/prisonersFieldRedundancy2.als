abstract sig Prisoner {
  cellMate : some Prisoner - this,
}

fact { no p : Prisoner | p in p.cellMate}

run {} for 3
