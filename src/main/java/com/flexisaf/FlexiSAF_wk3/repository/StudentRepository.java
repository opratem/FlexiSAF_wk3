package com.flexisaf.FlexiSAF_wk3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.flexisaf.FlexiSAF_wk3.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    
}
