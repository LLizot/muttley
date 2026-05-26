# Muttley API

API para gestão de eventos, participantes, certificados e medalhas.

## Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 13+
- Conta SMTP (Brevo, Gmail, etc) para envio de e-mails

## Configuração

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/seu-usuario/muttley.git
   cd muttley
   ```

2. **Configure o banco de dados:**
   - Crie um banco PostgreSQL chamado `projetomuttley`.
   - Ajuste as credenciais em `src/main/resources/application.properties`:
     ```properties
     spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/projetomuttley
     spring.datasource.username=postgres
     spring.datasource.password=admin
     spring.jpa.hibernate.ddl-auto=update
     ```

3. **Configure o envio de e-mail SMTP:**
   - Preencha as propriedades SMTP em `application.properties`:
     ```properties
     spring.mail.host=smtp-relay.brevo.com
     spring.mail.port=587
     spring.mail.username=seu@email.com
     spring.mail.password=sua-senha-ou-chave
     spring.mail.properties.mail.smtp.auth=true
     spring.mail.properties.mail.smtp.starttls.enable=true
     ```

4. **Instale as dependências:**
   ```bash
   mvn clean install -DskipTests
   ```

5. **Rode a aplicação:**
   ```bash
   ./mvnw spring-boot:run
   # ou
   mvn spring-boot:run
   ```

## Endpoints principais

Veja o arquivo [`endpoints.txt`](endpoints.txt) para exemplos de uso e payloads.

- Autenticação: `/auth/login`
- Clientes: `/clients`
- Eventos: `/events`
- Participantes: `/events/{eventoId}/participantes`
- Finalização de evento: `/events/{id}/finalizar` (gera certificados e envia por e-mail)
- Medalha em lote: `POST /events/participantes/medalha` (gera certificados e envia por e-mail)
- Teste de e-mail: `POST /emails/test` (body: `{ "email": "destinatario@exemplo.com" }`)

## Geração de Certificados

Ao finalizar um evento, certificados em PDF são gerados via uma API externa e enviados automaticamente para os participantes presentes.

Configurar a URL do gerador externo em `application.properties`:
```properties
certificate.generator.base-url=http://localhost:1336
```

Na rota de medalha em lote, o certificado usa `descricaoMedalha` e `competenciasMedalha` como texto de apresentacao e, se houver `arquivoPlanoDeFundo`, ele vira o `backgroundImage`. Caso contrario, usa a imagem padrao `templates/background.png`.

## Observações

- O projeto utiliza Lombok. Se for abrir no Eclipse/VS Code, instale o plugin Lombok.
- O banco será atualizado automaticamente (`ddl-auto=update`), mas para produção recomenda-se usar migrations.
- Para dúvidas, consulte o código ou abra uma issue.

---