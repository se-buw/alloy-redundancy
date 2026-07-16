sig A {
  f: set Int
} { 
  one f
}

sig B extends A {} {some f}

run {} for 3