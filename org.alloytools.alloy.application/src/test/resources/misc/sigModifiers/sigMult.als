one sig A {
  f: Int
}

sig B {
}

fact {
 some A implies # f = 2
}

run {} for 3