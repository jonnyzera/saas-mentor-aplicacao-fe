package com.saas.semente.mentor_de_aplicacao_da_fe.controller;

import com.saas.semente.mentor_de_aplicacao_da_fe.model.FaithApplication;
import com.saas.semente.mentor_de_aplicacao_da_fe.repository.FaithApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional; // NOVA IMPORTAÇÃO

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ApplicationController {

    @Autowired
    private FaithApplicationRepository repository;

    /**
     * Rota principal: /
     */
    @GetMapping("/")
    @Transactional(readOnly = true) // ADICIONADO: Corrige o erro de auto-commit do LOB
    public String index(Model model) {
        List<FaithApplication> applications = repository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("applications", applications);
        return "index";
    }

    /**
     * Rota POST: /generate (Não precisa de @Transactional, pois @PostMapping é
     * transacional por padrão)
     * INTEGRAÇÃO DA IA: Usa o desafio do usuário para gerar a mentoria dinâmica.
     */
    @PostMapping("/generate")
    public String generateMentorship(@RequestParam("userChallenge") String userChallenge, RedirectAttributes ra) {
        if (userChallenge.isBlank()) {
            ra.addFlashAttribute("errorMessage", "O Desafio do usuário não pode ser vazio.");
            return "redirect:/";
        }

        // --- CHAMADA À IA SIMULADA ---
        FaithApplication newApp = generateAiMentorship(userChallenge);
        // --- FIM CHAMADA À IA SIMULADA ---

        repository.save(newApp);
        ra.addFlashAttribute("successMessage", "Mentoria gerada e registrada com sucesso!");
        return "redirect:/";
    }

    /**
     * Rota: /dashboard
     */
    @GetMapping("/dashboard")
    @Transactional(readOnly = true) // ADICIONADO: Corrige o erro de auto-commit do LOB
    public String dashboard(Model model) {
        List<FaithApplication> allRecords = repository.findAll();

        // 1. Cálculos para o Calendário de Hábito Diário (dasboard.html)
        List<Integer> registrationDates = allRecords.stream()
                .map(app -> app.getCreatedAt().getDayOfMonth())
                .distinct()
                .collect(Collectors.toList());

        // Cria um Mapa de Registros por Dia (para o Modal de Detalhe Diário)
        Map<Integer, List<FaithApplication>> recordsByDay = allRecords.stream()
                .collect(Collectors.groupingBy(app -> app.getCreatedAt().getDayOfMonth()));

        // 2. Cálculos para a Composição dos Desafios (Gráfico ApexCharts)
        Map<String, Long> themeFrequency = allRecords.stream()
                .flatMap(app -> {
                    if (app.getIdentifiedTheme() != null) {
                        return List.of(app.getIdentifiedTheme().split(",")).stream()
                                .map(String::trim);
                    }
                    return null;
                })
                .filter(theme -> theme != null && !theme.isBlank())
                .collect(Collectors.groupingBy(theme -> theme, Collectors.counting()));

        // 3. Insight Reflexivo (Placeholder)
        String latestInsight = generateLatestInsight(themeFrequency);

        model.addAttribute("registrationDates", registrationDates);
        model.addAttribute("themeFrequency", themeFrequency);
        model.addAttribute("recordsByDay", recordsByDay);
        model.addAttribute("latestInsight", latestInsight);

        return "dasboard";
    }

    /**
     * Rota: /all-records
     */
    @GetMapping("/all-records")
    @Transactional(readOnly = true) // ADICIONADO: Corrige o erro de auto-commit do LOB
    public String allRecords(Model model) {
        List<FaithApplication> applications = repository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("applications", applications);
        return "all-records";
    }

    /**
     * Rota DELETE: /delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public String deleteRecord(@PathVariable("id") Long id, RedirectAttributes ra) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            ra.addFlashAttribute("successMessage", "Registro excluído com sucesso!");
        } else {
            ra.addFlashAttribute("errorMessage", "Erro: Registro não encontrado.");
        }
        return "redirect:/dashboard";
    }

    /**
     * [NOVO MÉTODO ESSENCIAL] Simula a chamada à IA (Mentor Premium) para gerar a
     * mentoria.
     */
    private FaithApplication generateAiMentorship(String userChallenge) {
        FaithApplication app = new FaithApplication();
        app.setUserChallenge(userChallenge);

        String challenge = userChallenge.toLowerCase();

        String identifiedTheme;
        String versiculoBussola;
        String reflexaoAplicada;
        String planoDeAcao;
        String referenciasCruzadas;
        String oracaoSemente;

        if (challenge.contains("ansiedade") || challenge.contains("preocupado")
                || challenge.contains("sobrecarregado")) {
            identifiedTheme = "Ansiedade, Confiança, Paz";
            versiculoBussola = "Filipenses 4:6-7";
            reflexaoAplicada = "A Palavra nos convida a lançar toda a ansiedade sobre Ele, praticando a gratidão e a oração. A paz de Deus, que excede todo o entendimento, guardará o seu coração e a sua mente em Cristo Jesus. Não se prenda às preocupações; confie Nele.";
            planoDeAcao = "- Dedique 3 minutos para respirar fundo e listar 3 motivos de gratidão.\n- Divida suas tarefas sobrecarregantes em 3 passos menores.\n- Pratique a oração de entrega antes de começar o trabalho.";
            referenciasCruzadas = "Mateus 6:34, 1 Pedro 5:7, Salmos 46:1";
            oracaoSemente = "Pai, eu Te entrego esta ansiedade. Que a Tua paz, que ultrapassa minha compreensão, me guarde hoje e me lembre de Tua soberania em todas as coisas. Amém.";
        } else if (challenge.contains("paciência") || challenge.contains("raiva") || challenge.contains("conflito")) {
            identifiedTheme = "Paciência, Amor, Autocontrole";
            versiculoBussola = "Efésios 4:2";
            reflexaoAplicada = "Com toda a humildade e mansidão, com longanimidade, suportando-vos uns aos outros em amor. A paciência (longanimidade) é um fruto do Espírito que se desenvolve na prática diária de suportar as falhas dos outros, espelhando o amor de Cristo por nós. Lembre-se de como Deus é paciente com você.";
            planoDeAcao = "- Quando a impaciência surgir, pause por 10 segundos antes de responder.\n- Identifique a real fonte da sua impaciência (cansaço, estresse).\n- Peça a Deus o fruto do autocontrole antes de interagir com a pessoa desafiadora.";
            referenciasCruzadas = "Provérbios 15:18, Colossenses 3:12-13, Gálatas 5:22-23";
            oracaoSemente = "Espírito Santo, encha-me de mansidão e longanimidade. Que minhas palavras e ações edifiquem e demonstrem o Teu amor. Amém.";
        } else {
            // Default/Fallback para temas genéricos
            identifiedTheme = "Foco, Direção, Sabedoria";
            versiculoBussola = "Provérbios 3:5-6";
            reflexaoAplicada = "Confie no Senhor de todo o seu coração e não se apoie no seu próprio entendimento. Reconheça-o em todos os seus caminhos, e Ele endireitará as suas veredas. A sabedoria começa no temor ao Senhor e em buscar a Sua direção em vez da nossa própria lógica, submetendo todos os planos a Ele.";
            planoDeAcao = "- Comece o dia lendo um Salmo para centrar seu foco e propósito.\n- Faça uma lista de prioridades e se atenha a ela, eliminando distrações.\n- Busque o conselho de um mentor ou líder espiritual.";
            referenciasCruzadas = "Salmos 119:105, Tiago 1:5, 1 Coríntios 10:31";
            oracaoSemente = "Senhor, confio em Ti e não em minha força. Mostra-me o caminho que devo seguir hoje e me dê a sabedoria necessária para cada decisão. Amém.";
        }

        app.setIdentifiedTheme(identifiedTheme);
        app.setVersiculoBussola(versiculoBussola);
        app.setReflexaoAplicada(reflexaoAplicada);
        app.setPlanoDeAcao(planoDeAcao);
        app.setReferenciasCruzadas(referenciasCruzadas);
        app.setOracaoSemente(oracaoSemente);

        return app;
    }

    private String generateLatestInsight(Map<String, Long> themeFrequency) {
        if (themeFrequency.isEmpty()) {
            return "Parabéns! Você tem plantado sementes diariamente. Continue acompanhando seu Mapa de Crescimento. 🌱";
        }

        String mainTheme = themeFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Reflexão");

        long totalRecords = themeFrequency.values().stream().mapToLong(Long::longValue).sum();

        return String.format(
                "Você tem %d registros de aplicações neste período, e seu foco principal tem sido em **%s**. O Mentor sugere que você releia o Salmo 23 para encontrar descanso e direção. Continue a jornada!",
                totalRecords, mainTheme);
    }
}