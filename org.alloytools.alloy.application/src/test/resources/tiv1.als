var lone sig Request {}
var lone sig Grant {}

fact noRequests {
  always no Request
}

// classic example of a property that is vacuously true
fact requestsAreEventuallyGranted {
  always (one Request implies eventually one Grant)
}

run {} for 3 steps 