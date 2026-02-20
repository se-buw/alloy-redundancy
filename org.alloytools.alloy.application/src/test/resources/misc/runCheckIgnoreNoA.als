sig A {}

fact noA_fact {no A}

pred oneA_pred { one A }

assert oneA_assert {
  one A
  -- A not in A
}

// neither the fact noA_fact nor the predicate are redundant here
run oneA_pred

// however, removing the fact changes the valuation of this check command
check oneA_assert
