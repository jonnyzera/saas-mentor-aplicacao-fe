package com.saas.semente.mentor_de_aplicacao_da_fe.controller;

import com.saas.semente.mentor_de_aplicacao_da_fe.model.FaithApplication;
import com.saas.semente.mentor_de_aplicacao_da_fe.repository.FaithApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // NOVA IMPORTAÇÃO
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Controller
public class ApplicationController {

    // A chave será injetada do application.properties. Se não for encontrada, usará
    // uma string vazia (fix: ":").
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-05-20:generateContent?key=";

    @Autowired
    private FaithApplicationRepository repository;

    /**
     * Rota principal: /
     */
    @GetMapping("/")
    @Transactional(readOnly = true)
    public String index(Model model) {
        List<FaithApplication> applications = repository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("applications", applications);
        return "index";
    }

    /**
     * Rota POST: /generate
     * INTEGRAÇÃO DA IA: Usa o desafio do usuário para gerar a mentoria dinâmica
     * usando o Gemini.
     */
    @PostMapping("/generate")
    public String generateMentorship(@RequestParam("userChallenge") String userChallenge, RedirectAttributes ra) {
        if (userChallenge.isBlank()) {
            ra.addFlashAttribute("errorMessage", "O Desafio do usuário não pode ser vazio.");
            return "redirect:/";
        }

        try {
            // --- CHAMADA À IA REAL (GEMINI) ---
            FaithApplication newApp = callGeminiApi(userChallenge);
            // --- FIM CHAMADA À IA REAL (GEMINI) ---

            repository.save(newApp);
            ra.addFlashAttribute("successMessage", "Mentoria gerada e registrada com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro ao chamar a API do Gemini ou processar JSON: " + e.getMessage());
            ra.addFlashAttribute("errorMessage", "Erro ao gerar a mentoria. Verifique sua chave de API ou a conexão.");
        }

        return "redirect:/";
    }

    /**
     * Rota: /dashboard
     */
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
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
                        // O tema é uma string separada por vírgulas, então precisamos separá-lo.
                        return List.of(app.getIdentifiedTheme().split(",")).stream()
                                .map(String::trim);
                    }
                    return null;
                })
                .filter(theme -> theme != null && !theme.isBlank())
                .collect(Collectors.groupingBy(theme -> theme, Collectors.counting()));

        // 3. Insight Reflexivo
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
    @Transactional(readOnly = true)
    public String allRecords(Model model) {
        List<FaithApplication> applications = repository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("applications", applications);
        return "all-records";
    }

    /**
     * Rota de EXCLUSÃO: /delete/{id}
     *
     * Usa @RequestMapping para aceitar tanto DELETE (semântico) quanto POST
     * (compatibilidade
     * com formulários HTML que usam _method=delete). Isso corrige o erro 405.
     */
    @RequestMapping(value = "/delete/{id}", method = { RequestMethod.DELETE, RequestMethod.POST })
    public String deleteRecord(@PathVariable("id") Long id, RedirectAttributes ra) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            ra.addFlashAttribute("successMessage", "Registro excluído com sucesso!");
        } else {
            ra.addFlashAttribute("errorMessage", "Erro: Registro não encontrado.");
        }
        // Redireciona de volta para a tela de todos os registros (onde o usuário
        // estava)
        return "redirect:/all-records";
    }

    /**
     * Faz a chamada à API do Gemini para gerar o conteúdo estruturado da Mentoria.
     *
     * @param userChallenge O desafio do usuário.
     * @return Um objeto FaithApplication preenchido.
     * @throws Exception Se a chamada ou o processamento falhar.
     */
    private FaithApplication callGeminiApi(String userChallenge) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        // CONSTRÓI A URL COMPLETA USANDO A CHAVE INJETADA
        String geminiApiUrl = GEMINI_API_BASE_URL + geminiApiKey;

        // 1. Definição do System Instruction (Persona e Regras)
        String systemPrompt = "Você é o 'Mentor de Aplicação da Fé', um conselheiro cristão sábio e amigável. Seu objetivo é pegar o 'Desafio do Usuário' e, com base em princípios bíblicos, gerar uma mentoria estruturada em 5 partes. Sua 'Reflexão Aplicada' deve ter um tom pessoal, caloroso e de conselho de amigo, como um guia espiritual. Retorne a resposta APENAS como um objeto JSON que segue o schema fornecido. O 'planoDeAcao' deve ser uma lista de 3 a 5 passos práticos separados por quebra de linha ('- Passo 1\\n- Passo 2\\n...').";

        // 2. Definição do Schema JSON para a resposta
        // Usamos as Text Blocks do Java 17+
        String jsonSchema = """
                {
                  "type": "OBJECT",
                  "properties": {
                    "identifiedTheme": { "type": "STRING", "description": "Tema(s) central(is) identificado(s) no desafio do usuário. Ex: Ansiedade, Perdão, Direção." },
                    "versiculoBussola": { "type": "STRING", "description": "O versículo bíblico central para o desafio. Ex: Filipenses 4:6-7" },
                    "reflexaoAplicada": { "type": "STRING", "description": "A reflexão e conselho com tom de amigo. Use quebras de linha (\\n) para formatar parágrafos." },
                    "planoDeAcao": { "type": "STRING", "description": "Uma lista de 3 a 5 passos práticos e acionáveis, separados por quebra de linha. Comece cada item com hífen e espaço. Ex: - Passo 1\\n- Passo 2" },
                    "referenciasCruzadas": { "type": "STRING", "description": "Outras referências bíblicas contextuais para estudo. Ex: Mateus 6:34, 1 Pedro 5:7, Salmos 46:1" },
                    "oracaoSemente": { "type": "STRING", "description": "Uma oração curta e poderosa baseada no desafio e no versículo. Use quebras de linha (\\n) para formatar." }
                  }
                }
                """;

        // 3. Montagem do corpo da requisição
        Map<String, Object> payload = new HashMap<>();

        // Conteúdo (User Query)
        Map<String, Object> userPart = Map.of("text", userChallenge);
        Map<String, Object> contents = Map.of("role", "user", "parts", List.of(userPart));
        payload.put("contents", List.of(contents));

        // System Instruction (Persona)
        Map<String, Object> systemInstruction = Map.of("parts", List.of(Map.of("text", systemPrompt)));
        payload.put("systemInstruction", systemInstruction);

        // Configuração de Geração (JSON Schema e modo estruturado)
        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "responseSchema", objectMapper.readValue(jsonSchema, Map.class));
        payload.put("generationConfig", generationConfig);

        // 4. Configuração do Header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 5. Envio da Requisição
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                geminiApiUrl, // Usa a URL completa com a chave injetada
                HttpMethod.POST,
                entity,
                Map.class);

        // 6. Processamento da Resposta (extração do JSON)
        if (response.getBody() == null || response.getBody().get("candidates") == null) {
            throw new Exception("Resposta da API Gemini inválida ou vazia.");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");

        if (candidates.isEmpty()) {
            throw new Exception("Nenhum candidato de resposta da API Gemini.");
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");

        if (parts.isEmpty() || !parts.get(0).containsKey("text")) {
            throw new Exception("Conteúdo da resposta Gemini está vazio ou malformado.");
        }

        String jsonText = parts.get(0).get("text");

        // Deserializa o JSON gerado pela IA para um mapa
        Map<String, String> result = objectMapper.readValue(jsonText, Map.class);

        // 7. Mapeamento para o Objeto FaithApplication
        FaithApplication app = new FaithApplication();
        app.setUserChallenge(userChallenge);
        app.setIdentifiedTheme(result.get("identifiedTheme"));
        app.setVersiculoBussola(result.get("versiculoBussola"));
        app.setReflexaoAplicada(result.get("reflexaoAplicada"));
        app.setPlanoDeAcao(result.get("planoDeAcao"));
        app.setReferenciasCruzadas(result.get("referenciasCruzadas"));
        app.setOracaoSemente(result.get("oracaoSemente"));

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
