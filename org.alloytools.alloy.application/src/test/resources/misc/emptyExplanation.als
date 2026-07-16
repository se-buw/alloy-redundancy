sig A {
  x: one A
}

fact {
  all a: A | one a.x
}