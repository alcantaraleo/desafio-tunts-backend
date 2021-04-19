package br.com.tunts.desafio.backend.domain;

import java.util.ArrayList;
import java.util.List;

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
	
	/**
	 * Auxiliary method that parses each row from the spreadsheet and creates the
	 * appropriate {@link Student} object that represents it.
	 * 
	 * @param row the Google spreadsheet row, read from Google Sheets.
	 * @return the student object that represents the row being read.
	 */
	public static Student mapToStudent(List<Object> row) {

		return Student.builder()
				.enrollmentId(Long.valueOf((String) row.get(0)))
				.name((String) row.get(1))
				.absences(Long.valueOf((String) row.get(2)))
				.firstGrade(Long.valueOf((String) row.get(3)))
				.secondGrade(Long.valueOf((String) row.get(4)))
				.thirdGrade(Long.valueOf((String) row.get(5)))
				.build();

	}
	
	/**
	 * Maps the student back to a row format 
	 * @return the student's attributes as a list of object
	 */
	public List<Object> mapToRow() {
		
		List<Object> row = new ArrayList<Object>();
		
		row.add(this.enrollmentId);
		row.add(this.name);
		row.add(this.absences);
		row.add(this.firstGrade);
		row.add(this.secondGrade);
		row.add(this.thirdGrade);
		row.add(this.situation.getDescription());
		row.add(this.necessaryGrade);
		
		
		return row;
	}

}
