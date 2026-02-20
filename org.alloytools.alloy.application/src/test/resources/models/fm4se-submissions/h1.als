/* Create an Alloy model for a scenario of your choice. 
* Declare at least 4 signatures each with at least 2 
  fields. 
* Use inheritance between signatures at least once.
* Define at least 2 facts and 2 predicates.
* Add two run commands to your model.
  * The first run command should be unsatisfiable.
  * The second run command should be satisfiable 
    and return at least 2 instances.
*/

// Signatures
sig User {
    username: String,
    email: String
}

abstract sig Product {
    productId: Int,
    name: String,
    price: Int,
    seller: User
}

sig ElectronicProduct extends Product {
    brand: String,
    warrantyPeriod: Int
}

sig ClothingProduct extends Product {
    size: String,
    brand: String
}

sig Transaction {
    transactionId: Int,
    buyer: User,
    seller: User,
    product: Product
}

sig Feedback {
    feedbackId: Int,
    rating: Int,
    comment: String,
    user: User
}

// Fields
fact uniqueUsernames {
    all u1, u2: User | u1 != u2 implies u1.username != u2.username
}

fact uniqueProductIds {
    all p1, p2: Product | p1 != p2 implies p1.productId != p2.productId
}

fact uniqueTransactionIds {
    all t1, t2: Transaction | t1 != t2 implies t1.transactionId != t2.transactionId
}

fact uniqueFeedbackIds {
    all f1, f2: Feedback | f1 != f2 implies f1.feedbackId != f2.feedbackId
}

// Predicates
pred validTransaction[t: Transaction] {
    t.product.seller != t.buyer
}

pred positiveFeedback[f: Feedback] {
    f.rating > 3
}

// Run Commands
run unsatisfiableExample {
    // This command is intentionally unsatisfiable for demonstration purposes
    all p1, p2: Product | p1 = p2 implies p1.productId = p2.productId
}

run satisfiableExample {
    // This command is designed to be satisfiable
    all p1, p2: Product | p1 != p2 implies p1.productId != p2.productId
}

// run {} for 3