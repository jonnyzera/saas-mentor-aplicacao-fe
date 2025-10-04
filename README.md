# 🌿 Semente: Mentor de Aplicação da Fé (SaaS Simulado)

## 🌟 Descrição do Projeto

O projeto **Semente** é uma aplicação web construída com Spring Boot e Thymeleaf que simula um serviço de "Mentor Premium" para a aplicação da fé. Ele foi projetado para ser um diário digital de crescimento espiritual.

O usuário compartilha um desafio pessoal (emoção, problema, conflito, etc.) e o sistema (simulando a lógica de uma Inteligência Artificial) gera uma mentoria completa e acionável com base em princípios bíblicos, salvando o registro no banco de dados para acompanhamento.

## ✨ Funcionalidades Principais

* **Diário de Aplicações (`/`):** Formulário principal para o usuário inserir seu desafio diário (`userChallenge`) e receber a mentoria gerada.
* **Geração de Mentoria (Lógica IA):** O *controller* possui a função `generateAiMentorship` que, com base em palavras-chave no desafio, identifica temas (ex: Ansiedade, Paciência) e gera conteúdo estruturado como Versículo-Bússola, Reflexão Aplicada, Plano de Ação e Oração-Semente.
* **Mapa de Crescimento (`/dashboard`):** Painel visual que permite ao usuário acompanhar sua jornada. Inclui:
    * Um gráfico de composição de temas (`themeFrequency`) para mostrar o foco principal dos desafios.
    * Um calendário de hábito diário, destacando os dias com registros.
    * Um *Insight Reflexivo* sobre o progresso.
* **Arquivo de Registros (`/all-records`):** Exibe a lista completa de todas as mentorias salvas, em ordem decrescente de criação.
* **Gerenciamento de Registros:** Permite a exclusão de registros tanto pelo painel do calendário (`/dashboard`) quanto pela lista completa (`/all-records`) via um formulário `DELETE` com confirmação modal.
* **Persistência de Dados:** Todos os registros são salvos na entidade `FaithApplication` utilizando PostgreSQL.

## 🛠️ Stack Tecnológico

| Componente | Detalhe | Arquivos de Referência |
| :--- | :--- | :--- |
| **Linguagem** | Java 17 | `pom.xml` |
| **Framework** | Spring Boot 3.5.6 (Web, Data JPA) | `pom.xml` |
| **Banco de Dados** | PostgreSQL | `pom.xml`, `application.properties` |
| **Interface** | Thymeleaf, Bootstrap 5.3.3, ApexCharts, Font Awesome | `pom.xml`, `index.html` |
| **Ferramenta de Build** | Apache Maven (com Maven Wrapper) | `pom.xml`, `mvnw` |
| **Auxiliar** | Lombok (para Data Model) | `pom.xml`, `FaithApplication.java` |

## 🚀 Configuração e Execução

### Pré-requisitos

1.  Java Development Kit (JDK) **17** ou superior.
2.  Um servidor de banco de dados **PostgreSQL** ativo.

### 1. Configuração do Banco de Dados

O projeto está configurado para se conectar a um banco de dados PostgreSQL local. As credenciais padrão estão definidas em `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mentor_db
spring.datasource.username=mentor_user
spring.datasource.password=123456
spring.jpa.hibernate.ddl-auto=update
