package com.saas.semente.mentor_de_aplicacao_da_fe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob; // NOVA IMPORTAÇÃO
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class FaithApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Campos preenchidos pelo usuário
    @Lob // Garante que o texto seja longo (TEXT)
    private String userChallenge;

    // Campos gerados pela IA (Mentor Premium)
    private String identifiedTheme;

    @Lob // Garante que o texto seja longo (TEXT)
    private String versiculoBussola;

    @Lob // Garante que o texto seja longo (TEXT)
    private String reflexaoAplicada;

    @Lob // Garante que o texto seja longo (TEXT)
    private String planoDeAcao;

    private String referenciasCruzadas;

    @Lob // Garante que o texto seja longo (TEXT)
    private String oracaoSemente;

    @CreationTimestamp
    private LocalDateTime createdAt;
}