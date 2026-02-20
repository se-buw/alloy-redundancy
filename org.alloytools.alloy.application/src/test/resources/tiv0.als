var lone sig Request {}
var lone sig Grant {}

fact noRequests {
  no Request
}

fact requestsAreEventuallyGranted {
  always (one Request implies eventually one Grant)
}

run {} for 3 steps 