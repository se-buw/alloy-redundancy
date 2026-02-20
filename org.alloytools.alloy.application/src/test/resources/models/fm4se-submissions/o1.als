// University Model

// Signatures
abstract sig Person { name: String, age: Int }

sig Student extends Person { studentID: Int, major: String, enrolledCourses: set Course }

sig Professor extends Person { staffID: Int, department: String, teaches: set Course }

sig Course { courseCode: String, credits: Int }

sig Classroom { roomNumber: Int, capacity: Int }

// Inheritance
fact StudentsArePeople { all s: Student | s in Person }

// Facts
fact CoursesHaveUniqueCodes { all disj c1, c2: Course | c1.courseCode != c2.courseCode }

// Predicates
pred TeachingCapacity[p: Professor, c: Course, r: Classroom] {
  some room: r | room.capacity > 30 
}

pred EnrolledStudents {
  all c: Course | some s: Student | s.major = "Computer Science" and c in s.enrolledCourses
}

  // This run command is unsatisfiable
run {

  some p: Professor | no c: Course | c in p.teaches
} for 5 

// This run command is satisfiable
run {
  some s: Student, c: Course, p: Professor, r: Classroom | s.major = "Computer Science" and c in s.enrolledCourses and TeachingCapacity[p, c, r] and EnrolledStudents
} for 5 but exactly 2 Student, 2 Course, 2 Professor, 2 Classroom