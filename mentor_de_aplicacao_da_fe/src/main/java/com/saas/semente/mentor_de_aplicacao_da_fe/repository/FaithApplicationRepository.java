package com.saas.semente.mentor_de_aplicacao_da_fe.repository;

import com.saas.semente.mentor_de_aplicacao_da_fe.model.FaithApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaithApplicationRepository extends JpaRepository<FaithApplication, Long> {

    // Útil para o Dashboard, para buscar todos os registros em ordem decrescente
    List<FaithApplication> findAllByOrderByCreatedAtDesc();
}