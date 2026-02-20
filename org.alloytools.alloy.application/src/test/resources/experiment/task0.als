abstract sig Person {
  teaches : set Module
}

sig Professor extends Person {}

sig Module {
  teacher : Person
}

fact teachersAndTeaching {
  // only professors teach modules
  Module.teacher = Professor
}

fact allModulesTaught {  
  // no dangling modules
  Professor.teaches = Module
}

fact teachersOfModules {
  // teachers teach their modules
  teacher = ~teaches
}

run {some teaches} for 2
