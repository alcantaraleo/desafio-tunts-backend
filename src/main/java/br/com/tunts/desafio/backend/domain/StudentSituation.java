package br.com.tunts.desafio.backend.domain;

import lombok.Getter;

@Getter
public enum StudentSituation {
	
	REPROVADO_POR_NOTA("Reprovado por nota"),
	REPROVADO_POR_FALTA("Reprovado por falta"),
	EXAME_FINAL("Exame Final"),
	APROVADO("Aprovado");
	
	private String description;
	
	StudentSituation(String description) {
		this.description = description;
	}

}
