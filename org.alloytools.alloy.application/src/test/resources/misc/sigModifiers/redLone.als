one abstract sig A {}
lone sig B extends A {}
lone sig C extends A {}

// both lone multiplicities are redundant here as A must be exaxtly one
run {}
