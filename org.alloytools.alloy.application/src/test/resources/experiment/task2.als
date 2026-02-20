sig Module {}

abstract sig Person {}

sig Student extends Person {
  attends : set Module
}
sig Professor extends Person {
  teaches : some Module
}

sig University {
  members : set Person
}

fact allMembers {
  University.members in Student + Professor
}

fact professorsMustTeach {
  all p : Professor | #p.teaches >= 1
}
