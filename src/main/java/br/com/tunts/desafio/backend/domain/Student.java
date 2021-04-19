package br.com.tunts.desafio.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Student {
	
	private Long enrollmentId;
	private String name;
	private Long absences;
	private Long firstGrade;
	private Long secondGrade;
	private Long thirdGrade;
	private StudentSituation situation;
	private Long necessaryGrade;

}
