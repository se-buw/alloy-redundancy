abstract sig Person {
  teaches : set Module
}

sig Professor extends Person {}

sig Module {
  teacher : Person
}

fact teachersOfModules {
  // teachers teach their modules
  teacher = ~teaches
}

fact moduleTeacherTeachesModule {
  all m : Module | m in m.teacher.teaches
}

run {some teaches} for 2
