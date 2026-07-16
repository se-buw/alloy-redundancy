abstract sig Person {
    name: one String,
    age: one Int
}

sig Doctor extends Person {
    specialty: one String,
    patients: set Patient
}

sig Nurse extends Person {
    department: one String,
    assignedDoctors: set Doctor
}

sig Patient extends Person {
    disease: one String,
    attendingDoctor: one Doctor
}

sig Appointment {
    patient: one Patient,
    doctor: one Doctor,
    timeSlot: one TimeSlot
}

sig TimeSlot {
    startTime: one Int,
    endTime: one Int
}

fact NoSelfTreatment {
    all d: Doctor | no p: d.patients | p.attendingDoctor = d
}

fact ConsistentDoctorAssignment {
    all p: Patient, d: Doctor | p.attendingDoctor = d iff p in d.patients
}

pred isAvailable[d: Doctor, t: TimeSlot] {
    no a: Appointment | a.doctor = d and a.timeSlot = t
}

pred needsSpecialist[p: Patient, s: String] {
    p.disease = s
}

run { some p: Patient, d: Doctor | p.attendingDoctor = d and p in d.patients } for 3 but 2 Doctor, 2 Patient, 3 TimeSlot, 2 Nurse
run { all d: Doctor | no p: Patient | p in d.patients and p.attendingDoctor = d } for 3 but 2 Doctor, 2 Patient, 3 TimeSlot, 2 Nurse