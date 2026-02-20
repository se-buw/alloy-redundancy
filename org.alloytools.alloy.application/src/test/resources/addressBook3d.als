module tour/addressBook3d ----- this is the final model in fig 2.18

open util/ordering [Book] as BookOrder

abstract sig Target { }
sig Addr extends Target { }
abstract sig Name extends Target { }

sig Alias, Group extends Name { }

sig Book {
	names: set Name,
	addr: names->some Target
} 

pred add [b, b": Book, n: Name, t: Target] {
	b".addr = b.addr + n->t
}

pred del [b, b": Book, n: Name, t: Target] {
	no b.addr.n or some n.(b.addr) - t
	b".addr = b.addr - n->t
}



pred init [b: Book]  { no b.addr }

fact traces {
	init [first]
	all b: Book-last |
	  let b" = b.next |
	    some n: Name, t: Target |
	      add [b, b", n, t] or del [b, b", n, t]
}


------------------------------------------------------

assert addIdempotent {
	all b, b", b"": Book, n: Name, t: Target |
		add [b, b", n, t] and add [b", b"", n, t]
		implies
		b".addr = b"".addr
}

// This should not find any counterexample.
check addIdempotent for 3

------------------------------------------------------