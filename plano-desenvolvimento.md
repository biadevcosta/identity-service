# identity-service — Plano de pair programming

Documento de trabalho para desenvolver o `identity-service` **passo a passo**.
Consolida o que já foi definido em `../guia-implementacao-codigo.md` (seção 2),
`../README.md` (seções 3, 5, 12) e `material-base-aula.md` (seção 6).

> **Decisões tomadas (2026-09-06):**
> 1. **Cadastro (`POST /users`) protegido por papel `ADMIN`** — 4º papel, com um admin semeado na migration.
> 2. **Nimbus** (`nimbus-jose-jwt`) para assinar o JWT — mesma lib que o `scheduling` usa para validar.
> 3. **Refresh token entra agora** — tabela `refresh_tokens`, `POST /auth/refresh` com rotação + revogação.
> 4. **Hash de senha: Argon2id** (memory-hard) — `Argon2PasswordEncoder` + BouncyCastle, via `DelegatingPasswordEncoder` (prefixo `{argon2id}`). Params base OWASP (m=19456 KiB, t=2, p=1); profile de teste com params leves.
> 5. *(em aberto)* validação estrita de `aud` / `iss` nos outros serviços — decidir mais tarde.

---

## 1. Combinado de trabalho (como vamos parear)

**Ritmo**

- Um passo pequeno por vez — em geral **1 arquivo** ou **1 conceito**.
- **Antes** do passo: eu digo **o que vou fazer e por quê** (2–4 linhas).
- **Depois** do passo: eu **explico o que foi criado**, focando nos pontos novos (não em getter/setter).
- Eu **paro e espero seu "ok / continua"** antes do próximo passo. Não emendo vários passos.

**O que eu NÃO faço sem combinar**

- Criar vários arquivos de uma vez ou "adiantar" etapas.
- Adicionar dependência nova sem avisar.
- Mudar uma decisão de arquitetura no meio — se aparecer a necessidade, eu paro e proponho.
- `git commit` / `git push` sem você pedir.

**Qualidade a cada bloco**

- Compilar e **rodar os testes** ao fim de cada bloco lógico.
- `domain` e `application` **sem nenhum import de framework** (a regra de ouro da Clean Architecture).
- Cobertura: mirar **JaCoCo 80%** — testar comportamento (regras, ordem, erros), não acessadores.

**Idioma**

- Código, identificadores, comentários, testes, mensagens de commit: **inglês**.
- Explicação, discussão, este documento: **português**.

**Decisões**

- Quando houver escolha de design, eu apresento **opção recomendada + alternativa** e **você decide**.

---

## 2. O que é o `identity-service`

| | |
|---|---|
| Papel | **Authorization Server** do sistema (emite os tokens) |
| Porta | **8080** |
| Banco | `identity_db` (MySQL) |
| Faz | cadastro de usuários (**só `ADMIN` cadastra**), login com senha, emissão de **JWT RS256** (access + refresh), refresh de token, `GET /users/{id}` |
| Também | valida o **próprio** JWT (com a `public.pem`) nas rotas de `ADMIN` — para essas rotas ele é *resource server*, além de *authorization server* |
| Não faz | validar o token dos **outros** serviços · sessão HTTP · gateway · login social |
| Pacote base | `com.biadevcosta.identity` |

Fluxo de login: `POST /auth/login {email, senha}` → valida a senha com **Argon2id** → assina um
**access token JWT RS256** (1 h) com a **chave privada** + gera um **refresh token** opaco (persistido,
vida longa) → devolve os dois. `scheduling` e `history` validam o access token com a **chave pública**
e **nunca** chamam o identity para isso — só chamam `GET /users/{id}` para resolver nome / e-mail.

Fluxo de refresh: `POST /auth/refresh {refreshToken}` → confere se existe, não expirou e não foi
revogado → **revoga o antigo e emite um par novo** (rotação).

Cadastro: `POST /users` exige `Authorization: Bearer <access token de um ADMIN>`. Existe um admin
semeado pela migration para dar a partida.

---

## 3. Regras de negócio (autoritativas — README §3)

- **4 papéis:** `DOCTOR`, `NURSE`, `PATIENT` (README §3) + **`ADMIN`** (só administra usuários; não é ator clínico).
- **Login:** qualquer usuário cadastrado. Senha guardada como **hash Argon2id** (memory-hard), via `DelegatingPasswordEncoder` (prefixo `{argon2id}`), nunca em claro.
- **Cadastro:** só `ADMIN`. Um admin é semeado pela migration (credenciais dev documentadas no README do serviço).
- **Claims do access token:** `sub` (userId), `role`, `patientId` (**só para PATIENT**), `iss`, `aud`, `exp`, `iat`.
- **`patientId` = `user.id`** para paciente (simplificação do guia — uma tabela `users` só).
- **RS256:** assina com `private.pem` (só aqui); os outros serviços validam com `public.pem`. O identity também
  usa a `public.pem` para validar o access token nas próprias rotas de `ADMIN`.
- **Erro de login genérico:** e-mail inexistente e senha errada → **mesma** resposta ("credenciais inválidas").
- **`exp` curto:** access token de **1 h** (`security.jwt.access-ttl-seconds: 3600`).
- **Refresh token:** opaco (não é JWT), **hash guardado** na tabela `refresh_tokens`, vida longa
  (`security.jwt.refresh-ttl-seconds`, ex.: 7 dias), **de uso único** (rotaciona a cada refresh),
  revogável (marca `revoked`).

---

## 4. Stack e restrições

- **Java 21 · Spring Boot 4.1 · Spring Data JDBC (sem JPA / Hibernate) · MySQL · Flyway · Spring Security.**
- Testes: **JUnit 5 + Mockito + AssertJ** (unit) · **Testcontainers** (integração) · **JaCoCo ≥ 80%** · Allure.
- ⚠️ **O guia foi escrito para Boot 3.x.** O skeleton do submódulo usa nomes de módulo do **Boot 4.1**
  (`spring-boot-starter-webmvc`, starters de teste por fatia). **Seguir o `pom.xml` real** depois de baixar o submódulo.
- **Assinatura e validação do JWT: Nimbus** (`com.nimbusds:nimbus-jose-jwt`, transitivo do `oauth2-resource-server`) — mesma lib que o `scheduling` usa.
- **Hash de senha: Argon2id** — `Argon2PasswordEncoder` (Spring Security) + `org.bouncycastle:bcprov-jdk18on`.
- Para validar o access token nas rotas de `ADMIN`: `spring-boot-starter-oauth2-resource-server` + `@EnableMethodSecurity`
  (mesmo padrão do `scheduling`).

---

## 5. Estrutura alvo (Clean Architecture)

```
identity-service/src/main/java/com/biadevcosta/identity/
├── domain/
│   ├── User.java                 entidade + regras (e-mail obrigatório, crm/specialty só p/ médico)
│   ├── Role.java                 enum DOCTOR | NURSE | PATIENT | ADMIN
│   ├── RefreshToken.java         valor: expiração + rotação/revogação (regras, sem framework)
│   └── exception/                InvalidCredentialsException, UserNotFoundException,
│                                 EmailAlreadyUsedException, InvalidUserException,
│                                 InvalidRefreshTokenException
├── application/
│   ├── port/                     UserRepository, PasswordHasher, TokenIssuer,
│   │                             RefreshTokenRepository            (interfaces)
│   ├── command/                  RegisterUserCommand
│   └── usecase/                  RegisterUserUseCase, AuthenticateUseCase,
│                                 RefreshAccessTokenUseCase          (POJOs)
└── infrastructure/
    ├── persistence/              UserEntity/Mapper/JdbcRepository/RepositoryImpl,
    │                             RefreshTokenEntity/Mapper/JdbcRepository/RepositoryImpl
    ├── security/                 Argon2PasswordHasher, NimbusJwtTokenIssuer (RS256), SecurityConfig
    ├── web/                      AuthController (POST /auth/login, POST /auth/refresh),
    │                             UserController (POST /users [ADMIN], GET /users/{id})
    └── config/                   UseCaseConfig  (@Bean liga os use cases)

src/main/resources/
├── application.yaml
├── db/migration/
│   ├── V1__create_users.sql
│   ├── V2__create_refresh_tokens.sql
│   └── V3__seed_admin.sql        (admin dev com hash Argon2id fixo — documentado)
├── private.pem                   (fora do Git — .gitignore; assina)
└── public.pem                    (valida o access token nas rotas ADMIN)
```

### Tabela `users` (migration V1)

| coluna | tipo | nota |
|---|---|---|
| `id` | VARCHAR(36) PK | UUID gerado no domínio |
| `email` | VARCHAR(255) UNIQUE NOT NULL | usado no login |
| `password_hash` | VARCHAR(255) NOT NULL | Argon2id com prefixo `{argon2id}` (~100 chars) — coluna com folga |
| `role` | VARCHAR(20) NOT NULL | `DOCTOR` \| `NURSE` \| `PATIENT` \| `ADMIN` |
| `full_name` | VARCHAR(255) NOT NULL | devolvido no `GET /users/{id}` |
| `phone` | VARCHAR(30) | opcional |
| `crm` | VARCHAR(20) | só médico |
| `specialty` | VARCHAR(100) | só médico |

### Tabela `refresh_tokens` (migration V2)

| coluna | tipo | nota |
|---|---|---|
| `id` | VARCHAR(36) PK | UUID |
| `token_hash` | VARCHAR(64) UNIQUE NOT NULL | SHA-256 do refresh token opaco (nunca guardamos o valor cru) |
| `user_id` | VARCHAR(36) NOT NULL | FK lógica para `users.id` |
| `expires_at` | DATETIME NOT NULL | agora + `refresh-ttl-seconds` |
| `revoked` | BOOLEAN NOT NULL | `true` após rotação ou logout |
| `created_at` | DATETIME NOT NULL | |

---

## 6. Contratos de API (rascunho — confirmar em cada passo)

```
POST /auth/login
  req:  { "email": "...", "password": "..." }
  res:  200 { "accessToken": "<jwt>", "refreshToken": "<opaco>", "expiresIn": 3600 }
        401 { "error": "invalid credentials" }

POST /auth/refresh
  req:  { "refreshToken": "<opaco>" }
  res:  200 { "accessToken": "<jwt>", "refreshToken": "<novo opaco>", "expiresIn": 3600 }   (rotaciona)
        401  se ausente / expirado / revogado / desconhecido

POST /users                       (cadastro — Authorization: Bearer <access token de ADMIN>)
  req:  { "email","password","role","fullName","phone"?,"crm"?,"specialty"? }
  res:  201 { "id","email","role","fullName" }
        401 sem token  ·  403 token não-ADMIN  ·  409 e-mail já usado

GET /users/{id}                   (consumido por notification / history — sem token de usuário)
  res:  200 { "id","name","email","role" }   |   404
  nota: fica aberto no challenge; hardening seria exigir token de serviço ou rede interna.
```

---

## 7. Decisões de segurança já fechadas (material-base-aula §6)

| Faz agora | Opcional (se sobrar tempo) | Não faz |
|---|---|---|
| RSA 2048 / RS256 · assinar (não cifrar) · claims certas · `exp` 1 h · **refresh token rotativo + revogável** · **Argon2id (hash de senha)** · stateless (access) · erro genérico · chave fora do Git · `POST /users` só ADMIN | rate limiting / lockout no login · CORS · auditoria de login · `aud` por serviço · logout que revoga todos os refresh do usuário | Kong / API Gateway · mTLS · login Google / OIDC · AES p/ cifrar campo · Spring Cloud Gateway |

---

## 8. Roteiro de passos (cada um = um bloco de pareamento)

> Marcar `[x]` conforme concluímos.

- [x] **0. Setup** — submódulo baixado; `pom.xml`/skeleton lidos (ver §11); alinhar dependências Boot 4.1 (+ `oauth2-resource-server`, `bcprov-jdk18on`, Testcontainers, JaCoCo; remover `graphql`).
- [x] **1. Config + migrations** — `application.yaml` (8080, datasource 3309, `security.jwt.*` + `security.password.argon2.*`, `spring.flyway.locations`); `V1__create_users.sql`, `V2__create_refresh_tokens.sql`. **`V3__seed_admin.sql` adiado para depois do passo 9** (hash gerado com o nosso encoder).
- [x] **2. Domínio** — `Role` (DOCTOR/NURSE/PATIENT/ADMIN); `User.register`/`rehydrate` (e-mail válido, `passwordHash`, `fullName`; `crm`+`specialty` exigidos só p/ DOCTOR e proibidos p/ os outros); `RefreshToken.issue`/`rehydrate`/`isUsable(now)`/`revoke()`; exceptions com base `IdentityException`. `BUILD SUCCESS`.
- [x] **3. Testes de domínio** — `UserTest` (9) + `RefreshTokenTest` (6); `IdentityApplicationTests` marcado `@Disabled` até o passo 13. `./mvnw test` → 16 run, 0 fail, 1 skipped.
- [x] **4. Portas + command** — `UserRepository` (save/findById/findByEmail/existsByEmail), `PasswordHasher` (hash/matches), `TokenIssuer` (→ `AccessToken(value, expiresInSeconds)`), `RefreshTokenRepository` (save/findByTokenHash), `RefreshTokenHasher` (randomToken/hash), `RegisterUserCommand`. `BUILD SUCCESS`.
- [x] **5. `RegisterUserUseCase`** + `RegisterUserUseCaseTest` (4): hash → existsByEmail → save (ordem); duplicado → `EmailAlreadyUsedException` sem salvar; senha vazia → `InvalidUserException` sem tocar nada; enfermeiro com `crm` → `InvalidUserException`. `User.register` passou a normalizar e-mail (`trim`+`lowercase`). 21 testes, 0 fail.
- [x] **6. `AuthenticateUseCase`** + `TokenMinter` (extraído p/ reuso no refresh) + `AuthTokens` record. `AuthenticateUseCaseTest` (3): valida senha, normaliza e-mail, delega ao minter; e-mail desconhecido / senha errada → `InvalidCredentialsException` genérica sem emitir. `TokenMinterTest` (2): access com `patientId` (null p/ não-paciente, `user.id` p/ PATIENT) + refresh persistido só como hash, `expiresAt = now(clock) + ttl`. `User.normalizeEmail` extraído. 26 testes.
- [x] **7. `RefreshAccessTokenUseCase`** + `RefreshAccessTokenUseCaseTest` (6): valida tudo → só então revoga (sem efeito colateral em falha); ordem `findByTokenHash → findById → save(revoked) → mintFor`; desconhecido / revogado / expirado / input vazio / usuário sumiu → `InvalidRefreshTokenException`. 32 testes.
- [x] **8. Persistência** — `V1`/`V2` ganham `version BIGINT NULL`. `UserEntity`/`RefreshTokenEntity` (`@Id`+`@Version`), `*JdbcRepository` (derived `findByEmail`/`existsByEmail`/`findByTokenHash`), `*Mapper` (`toNewEntity`/`copyInto`/`toDomain`), `*RepositoryImpl` (`@Repository`, `findById`→`copyInto`|`toNewEntity`). `UserPersistenceTest` (5) + `RefreshTokenPersistenceTest` (4), Mockito puro — molde do `AppointmentPersistenceTest`. 41 testes.
- [x] **9. Segurança (adapters, POJOs)** — `Argon2PasswordHasher` (envolve `PasswordEncoder`), `NimbusJwtTokenIssuer` (ctor recebe `RSAPrivateKey`; claims `iss`/`aud`/`sub`/`role`/`iat`/`exp`/`patientId?`), `Sha256RefreshTokenHasher` (`randomToken` 32B Base64URL / `hash` SHA-256 hex). 3 testes unit (7 casos): prefixo `{argon2id}`, match certo/errado; assina RS256 + verifica com a pública + claims + `exp==iat+ttl`; token URL-safe/único, hash 64 hex determinístico. 48 testes. `Argon2PasswordEncoder(int,int,int,int,int)` confirmado no Spring Security 7.1.0.
- [x] **10. Wiring** — `JwtProperties`/`Argon2Properties` (`@ConfigurationProperties` records), `RsaKeys` (PEM PKCS#8/X.509), `SecurityBeansConfig` (`Clock`, `PasswordEncoder` delegating→argon2id, `PasswordHasher`, `RefreshTokenHasher`, `TokenIssuer`), `UseCaseConfig` (`TokenMinter` + 3 use cases). Compila; 48 testes. Sem teste próprio (config, coberto pela integração).
- [x] **11. `SecurityConfig`** — `infrastructure/security/SecurityConfig` (`@EnableMethodSecurity`): stateless, csrf off; `permitAll` em `/actuator/**`, `POST /auth/**`, `GET /users/*`; resto `authenticated`. `JwtDecoder` = `NimbusJwtDecoder.withPublicKey` + `JwtValidators.createDefaultWithIssuer` (assinatura + `exp` + `iss`); claim `role` → `ROLE_<role>`. `SecurityBeansConfig` ganhou o bean `RSAPublicKey jwtVerificationKey`. Compila; 48 testes.
- [x] **12. Web** — `AuthController` (`POST /auth/login`, `/auth/refresh` → `TokenResponse` Bearer), `UserController` (`POST /users` `@PreAuthorize hasRole('ADMIN')` 201; `GET /users/{id}` público, `name`=`fullName`), `DomainExceptionHandler` (`@RestControllerAdvice`, `ProblemDetail`: 401/409/404/400 + `@Valid`→400). 3 testes unit (11 casos). 59 testes.
- [x] **13. Integração** — chaves geradas (`keys/private.pem`+`public.pem` reais + `test-*.pem` commitados em `src/test/resources/`); `AdminSeeder` (`ApplicationRunner`, cria `admin@hospital.local`/`admin12345` se ausente) + teste; `application-test.yaml` (chave de teste); `AbstractIntegrationTest` (MySQL Testcontainers + `assumeTrue` Docker); `IdentityIntegrationTest` (6 cenários, `RestTestClient` — Boot 4 removeu `TestRestTemplate`); `IdentityApplicationTests` removido. `./mvnw verify` → **60 testes 0 fail, JaCoCo 97.8%**. IT roda quando o Docker existir.
- [x] **14. Fechamento** — JaCoCo já no `pom.xml` desde o passo 0 (97.8% de cobertura). `README.md` do serviço reescrito (endpoints, arquitetura, Argon2, chaves, config, run, testes). Coleção **`identity.insomnia.json`** (login, refresh, cadastro, consulta). Allure: **pulado** (opcional).
- [x] **15. Docker** — `Dockerfile` (multi-stage maven→jre, porta 8080); `docker-compose.yml` ganhou o serviço `identity` (build local, `depends_on` mysql-identity `service_healthy`, `SPRING_DATASOURCE_URL` → `mysql-identity:3306`). Jar Spring Boot empacota OK. **Teste ponta a ponta real fica pendente até o Docker ser instalado.**

**Definição de pronto (cada passo):** compila · testes do bloco passam · sem framework em `domain` / `application` · explicado no chat · você deu "ok".

---

## 9. Perguntas em aberto

Resolvidas (ver o bloco "Decisões tomadas" no topo): cadastro só `ADMIN` · Nimbus · refresh token agora.

Ainda em aberto (não bloqueia o começo):

1. **`aud` / `iss` validados de forma estrita** nos `scheduling` / `history` agora, ou só emitimos com esses claims e apertamos a validação depois?
2. **Logout** (`POST /auth/logout` que revoga o(s) refresh do usuário) entra no escopo ou fica de fora?
3. Senha do **admin dev** semeado: qual valor documentar no README do serviço?

---

## 10. Referências rápidas

| Preciso de… | Está em |
|---|---|
| Código-molde de cada camada | `../guia-implementacao-codigo.md` §1 (scheduling, referência completa) e §2 (identity) |
| Regras de negócio e papéis | `../README.md` §3 · `../guia-implementacao-codigo.md` → "Fluxo do sistema de ponta a ponta" |
| Geração das chaves RSA | `../README.md` §12.2 · `material-base-aula.md` §1 |
| Conceitos de JWT / claims / RS256 | `material-base-aula.md` §2–§4 |
| O que NÃO implementar e por quê | `material-base-aula.md` §6.3 · este doc §7 |
| **Como testar quando o Docker chegar** | **`TESTAR-COM-DOCKER.md`** (checklist do zero) |

---

## 11. Notas do skeleton (passo 0)

Estado real do submódulo após `git submodule update --init identity-service`:

- **Parent:** `spring-boot-starter-parent:4.1.0`. `groupId=com.biadevcosta`, `artifactId=identity`, `java.version=21`.
- **Estrutura pronta:** `IdentityApplication`, `IdentityApplicationTests` (`@SpringBootTest contextLoads`),
  `package-info.java` em `domain`/`application`/`infrastructure`, `src/main/resources/public.pem`,
  `application.yaml` (não `.yml`).
- **`application.yaml` atual:** `server.port: 8080`; datasource `jdbc:mysql://localhost:3309/identity_db`
  root/root. **Porta do MySQL é 3309** (o `docker-compose.yml` do serviço mapeia `3309:3306`, só MySQL).
- **`.gitignore`** já ignora `src/main/resources/private.pem`.
- **Sem** lombok, sem actuator.

### `pom.xml` — o que tem e o que falta

| Já no skeleton | Falta adicionar (proposta) |
|---|---|
| `data-jdbc`, `flyway` (starter), `security`, `validation`, `webmvc`, `flyway-mysql`, `mysql-connector-j` | `spring-boot-starter-oauth2-resource-server` (validar token ADMIN) |
| `graphql` + `graphql-test` — **REST-only, remover** | `com.nimbusds:nimbus-jose-jwt` (assinar — explícito, versão pelo BOM) |
| test por fatia: `data-jdbc-test`, `flyway-test`, `security-test`, `validation-test`, `webmvc-test`, `graphql-test` | `org.bouncycastle:bcprov-jdk18on` (Argon2) |
| plugin `spring-boot-maven-plugin` | Testcontainers: `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter`, `:mysql` + BOM + property |
| | `spring-boot-starter-actuator` (healthcheck do Docker) |
| | plugin **JaCoCo** (gate 80% + excludes: `*Application`, `infrastructure/config/**`, `infrastructure/security/**`) |
| | *(opcional)* Allure — paridade com o `scheduling` |

Proposta de consolidação dos testes: trocar os 6 starters "por fatia" por
`spring-boot-starter-test` + `spring-boot-starter-security-test` (padrão do `scheduling`).

### `pom.xml` — aplicado (passo 0 concluído, `BUILD SUCCESS`)

- **Removido:** `spring-boot-starter-graphql` + `graphql-test` (identity é REST).
- **Testes consolidados:** `spring-boot-starter-test` + `spring-boot-starter-security-test` (os 6 "por fatia" saíram).
- **Adicionado (main):** `oauth2-resource-server`, `actuator`, `bcprov-jdk18on` **1.85.2** (property `bouncycastle.version`).
- **Adicionado (test):** `spring-boot-testcontainers`, `testcontainers:junit-jupiter`, `testcontainers:mysql` + `testcontainers-bom`.
- **Plugin JaCoCo** 0.8.13, gate 80%, excludes: `**/IdentityApplication.*`, `**/infrastructure/config/**`, `**/infrastructure/security/SecurityConfig.*` (o resto de `security/**` tem comportamento e será testado — difere do `scheduling`, que exclui o pacote inteiro).
- **Notas do Boot 4.1:** o BOM **não** gerencia `nimbus-jose-jwt` (vem transitivo do `oauth2-resource-server`) nem `bouncycastle` (versão fixada à mão). Spring Framework 7 / Spring Security 7 sob o capô.
- **Allure:** adiado para o passo 14.

### Ambiente local (estado em 2026-09-06)

| Ferramenta | Situação | Ação |
|---|---|---|
| **JDK** | Sem JDK no PATH / sem `JAVA_HOME`. Há um JDK 21.0.12 completo (com `javac`) embutido na extensão Red Hat Java do VS Code: `~/.vscode/extensions/redhat.java-1.56.0-win32-x64/jre/21.0.12.1-win32-x86_64`. Maven roda passando `JAVA_HOME` inline. | **Usuário vai instalar Temurin 21 + `JAVA_HOME`.** Enquanto isso, uso o JDK embutido. |
| **Docker** | Não instalado / fora do PATH. | **Usuário vai instalar o Docker Desktop.** Necessário nos passos 12–13 e no teste manual; passos 1–11 + unit tests não precisam. |
| **`~/.m2`** | Estava vazio; árvore do Boot 4.1 baixada no passo 0. | ok |
| **OpenSSL** | 3.5.7 disponível | gera as chaves RS256 no passo 13 |
| **Comando de build** | `JAVA_HOME="<jdk>" ./mvnw -B ...` (via git bash) | trocar por `./mvnw` puro quando `JAVA_HOME` estiver no ambiente |
