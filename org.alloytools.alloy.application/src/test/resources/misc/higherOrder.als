sig Person {
  children : set Person
}
fact higherOrder {
  some x : set Person | Person.children = x
}
