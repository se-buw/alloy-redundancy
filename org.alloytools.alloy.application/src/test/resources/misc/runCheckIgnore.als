// module that shows that ignoring a check assertions, but not a run predicate leads to
// incorrect redundancy detection of facts

sig A {}

fact oneA_fact {one A}

pred oneA_pred {
  one A
}

assert oneA_assert {
  one A
}

// here we could determine the fact oneA_fact to be redundant
run oneA_pred

// however, removing the fact changes the valuation of this check command
check oneA_assert
