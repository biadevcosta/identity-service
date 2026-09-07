# Material base — Segurança (consolidado das aulas)

> **Fonte:** material de aula FIAP — módulos *Autenticação e Autorização*,
> *Introdução à Criptografia* e *Configurando Spring Security*.
> Este arquivo é a versão **organizada e corrigida** das anotações, mais o
> **mapeamento do que entra no `identity-service`** (seção 6). Não é código do projeto.

## Índice

1. [Criptografia: simétrica, assimétrica e hashing](#1-criptografia-simétrica-assimétrica-e-hashing)
2. [JWT: estrutura e ciclo de vida](#2-jwt-estrutura-e-ciclo-de-vida)
3. [Fluxo de autenticação e autorização (stateless)](#3-fluxo-de-autenticação-e-autorização-stateless)
4. [JWT × OAuth 2.0](#4-jwt--oauth-20)
5. [Spring Security, API Gateway e temas correlatos (contexto)](#5-spring-security-api-gateway-e-temas-correlatos-contexto)
6. [O que disso vai para o `identity-service`](#6-o-que-disso-vai-para-o-identity-service)

---

## 1. Criptografia: simétrica, assimétrica e hashing

### Objetivo

Transformar **texto claro** em **texto cifrado** e vice-versa, garantindo
**confidencialidade**, **integridade** e **autenticidade** dos dados (em trânsito ou
armazenados).

### Simétrica

- **Uma única chave** cifra e decifra.
- Rápida e eficiente — boa para grandes volumes de dados.
- Problema: **distribuir e gerenciar a chave** com segurança, ainda mais com muitos usuários.
- Algoritmos: **AES**, DES, Blowfish, ChaCha20.

### Assimétrica

- **Par de chaves**: **pública** (pode ser compartilhada) e **privada** (segredo absoluto).
- O que a pública cifra, só a privada correspondente decifra — e vice-versa.
- Resolve o problema da troca segura de chaves; em compensação é **mais lenta**.
- Algoritmos: **RSA**, ECC (curva elíptica), Diffie-Hellman.
- É a base do **RS256** no JWT: a **privada assina**, a **pública verifica**.

### Hashing (não estava explícito na aula — mas é o que falta para senha)

- **Não é criptografia**: é **via única**, não existe "reverter" o hash.
- É o que se usa para **guardar senha**: salva-se o *hash*, nunca a senha.
- Use um hash **adaptativo e com sal**: **Argon2id** (1ª escolha, *memory-hard*), BCrypt ou scrypt.
  O *fator de custo* deixa o cálculo propositalmente lento, encarecendo ataques de força bruta.
- **Decisão do projeto:** `identity-service` usa **Argon2id** (ver `plano-desenvolvimento.md`, "Decisões tomadas").
- Encriptação (AES/RSA) é **reversível**; hash de senha **não deve ser**.

| Técnica | Reversível? | Chave | Uso no hospital-system |
|---|:---:|---|---|
| Simétrica (AES) | sim | 1 chave secreta | — (não usamos) |
| Assimétrica (RSA) | sim | par pública/privada | **assinar / verificar o JWT (RS256)** |
| Hash (BCrypt) | **não** | — (sal embutido) | **guardar a senha do usuário** |

### Gerar o par de chaves com OpenSSL

```bash
# chave privada (fica só no identity-service)
openssl genrsa -out private_key.pem 2048

# chave pública (distribuída para scheduling e history)
openssl rsa -in private_key.pem -pubout -out public_key.pem
```

No Windows: instalar o OpenSSL (ex.: build da *Shining Light Productions*), escolher 32/64 bits
e adicionar `C:\Program Files\OpenSSL-Win64\bin` ao `PATH`.

### Classes utilitárias da aula (ilustrativas — o identity NÃO faz isto)

> A aula demonstra **cifrar/decifrar** com AES e RSA. O `identity-service` **não cifra nada**:
> ele **assina** o token (RS256, chave privada) e **faz hash** da senha (BCrypt).
> As classes abaixo ficam só como referência do conceito de criptografia.

```java
// Criptografia simétrica (AES) — Código-fonte 1 da aula
public class SymmetricEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128;          // AES aceita 128, 192 ou 256 bits
    private static final SecretKey secretKey;

    static {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE);
            secretKey = keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar chave AES", e);
        }
    }

    public static String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);   // binário -> texto
    }

    public static String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decoded = Base64.getDecoder().decode(encryptedData);
        return new String(cipher.doFinal(decoded));
    }
}
```

```java
// Criptografia assimétrica (RSA) — Código-fonte 2 da aula
public class AsymmetricEncryptionUtil {

    private static final String ALGORITHM = "RSA";
    private static final KeyPair keyPair;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(2048);              // 2048 bits => segurança
            keyPair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar par de chaves RSA", e);
        }
    }

    public static String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());   // cifra com a PÚBLICA
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    public static String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());  // decifra com a PRIVADA
        byte[] decoded = Base64.getDecoder().decode(encryptedData);
        return new String(cipher.doFinal(decoded));
    }

    public static String getPublicKey() {                        // compartilhável
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
}
```

> **Detalhe importante:** RS256 no JWT **não** usa `Cipher`/RSA "encrypt" — usa *assinatura*
> (`Signature` RSA-SHA256). Assinar ≠ cifrar. A privada produz a assinatura; a pública só
> confirma que a assinatura bate com o conteúdo.

---

## 2. JWT: estrutura e ciclo de vida

Um JWT tem **3 partes** separadas por ponto, cada uma em **Base64URL**:

```
header . payload . signature
```

| Parte | Conteúdo | Observação |
|---|---|---|
| **header** | algoritmo (`RS256`) e tipo (`JWT`) | — |
| **payload** | as *claims* (dados do token) | **só codificado, não cifrado** — qualquer um lê. Nunca coloque segredo aqui. |
| **signature** | assinatura de `header.payload` com a chave privada | garante **integridade** (não foi adulterado) e **autenticidade** (veio de quem tem a privada) |

### Claims citadas na aula (e o que usamos no lugar)

| Claim | Significado | Exemplo da aula | No hospital-system |
|---|---|---|---|
| `iss` | quem emitiu | `mybackend` | `hospital-identity` |
| `sub` | usuário dono do token | `bruno` | `userId` |
| `scope` | permissões | `BASIC` | trocado por `role` = `DOCTOR` \| `NURSE` \| `PATIENT` |
| `exp` | expiração (epoch) | — | `iat + 1h` |
| — | — | — | `aud`, `iat`, e `patientId` (só para paciente) |

### Ciclo de vida

1. Cliente faz **login** → servidor devolve o JWT.
2. Cliente envia `Authorization: Bearer <token>` em **toda** requisição seguinte.
3. Cada backend valida a **assinatura com a chave pública** (não precisa chamar o emissor).
4. Quando `exp` passa, o token morre → o cliente pede um novo (novo login, ou *refresh token*
   se o sistema tiver).

---

## 3. Fluxo de autenticação e autorização (stateless)

A "camada de segurança" de cada serviço **protegido** (Resource Server) faz **3 checagens**:

| Checagem | Pergunta | Como |
|---|---|---|
| **Autenticação** | o usuário é quem diz ser? | apresenta um JWT válido |
| **Autorização** | ele pode fazer *isto*? | o `role`/`scope` do token permite a operação |
| **Validação do token** | o token presta? | assinatura confere **e** não expirou (`exp`) |

Passo a passo no backend que recebe a requisição:

1. Recebe `Authorization: Bearer <token>`.
2. Verifica a assinatura com a **chave pública** correspondente à privada que assinou.
3. Confere **integridade** (não modificado) e **validade** (`exp`).
4. Extrai as claims (`sub`, `role`, …) e repassa para a regra de negócio — que pode
   personalizar a resposta ou limitar ações conforme as permissões.
5. Se todas as validações passam **e** o usuário tem permissão → concede o acesso.

Ganhos do modelo:

- **Chave pública** ⇒ só tokens assinados pelo servidor são aceitos → bloqueia **token forjado**.
- **`scope`/`role`** ⇒ acesso controlado por permissão.
- **Stateless** ⇒ o backend não guarda sessão; tudo que ele precisa está no token.
- Emissor (identity) e validadores (scheduling, history) ficam **desacoplados**.

---

## 4. JWT × OAuth 2.0

- **JWT** = *formato* de token: compacto, assinado, `header.payload.signature`, trafega no
  header HTTP como `Bearer`.
- **OAuth 2.0** = *protocolo de autorização* (família OpenID): concede acesso a um recurso
  **em nome do usuário**, usando *access token* e (opcionalmente) *refresh token*, sem expor
  as credenciais. Pode usar JWT como formato do access token — ou não.
- Resumo: **JWT é o formato; OAuth 2.0 é o protocolo** que pode carregar JWTs.

Papéis do OAuth 2.0 e onde caem no nosso sistema:

| Papel OAuth 2.0 | O que é | No hospital-system |
|---|---|---|
| **Resource Owner** | dono dos dados/recursos | o usuário logado (médico / enfermeiro / paciente) |
| **Authorization Server** | autentica o dono e emite tokens | **`identity-service`** |
| **Resource Server** | API protegida que aceita o token | `scheduling-service`, `history-service` |
| **Client** | app que consome os recursos | Insomnia / GraphiQL / front-end |

> A aula usa o **Google** como Authorization Server (login social / OIDC). No nosso caso, o
> **`identity-service` é o próprio Authorization Server** (login com e-mail e senha) — por isso
> **não** integramos Google nem OIDC.

---

## 5. Spring Security, API Gateway e temas correlatos (contexto)

O módulo *Configurando Spring Security* trata de um cenário **diferente da nossa arquitetura**.
Fica registrado só como contexto:

- **API Gateway (Kong)**: portão único na frente dos microsserviços. Mais que um *proxy reverso*
  de roteamento — centraliza autenticação/autorização, **CORS**, **rate limiting**, logs,
  monitoração. Analogia da aula: a "portaria" (GATE) de um condomínio; ninguém entra sem passar
  por ela. Permite **antecipar a autenticação** antes de a requisição chegar ao microsserviço.
- **mTLS (Mutual TLS)**: dois serviços trocam certificados entre si para se autenticarem
  mutuamente (ex.: serviços "Pessoa" ↔ "Boleto" via Feign Client).
- **Spring Cloud Gateway / Access Token Pattern**: controle de acesso centralizado na
  comunicação entre microsserviços.
- **Rate limiting**: limitar chamadas por cliente (ex.: cliente A = 10 req/s; cliente B = 10.000 req/s).
- **Logs centralizados** e **monitoração** de acesso às APIs.
- **CORS**: saber qual *client* (origem) pode fazer requisições.
- **Basic Auth**, **API Token**, **LDAP**: outras formas de autenticar.

> **Nossa arquitetura é SEM gateway** (README §2 e §"Fluxo"): JWT stateless, **cada serviço
> valida o token com a chave pública**, sem portal central. Kong, mTLS e Spring Cloud Gateway
> ficam **fora do escopo**. Do módulo, o que dá para reaproveitar como *hardening* do identity
> é **rate limiting no login** e **CORS** — ver a seção 6.2.

---

## 6. O que disso vai para o `identity-service`

**Papel do serviço:** é o *Authorization Server* do sistema — cadastra usuários, faz login
com senha e **emite o JWT RS256** que `scheduling` e `history` validam com a chave pública.

### 6.1 Entra no escopo — implementar

| Item | De onde vem (aula) | Como no `identity-service` |
|---|---|---|
| Par de chaves **RSA 2048** + **RS256** | Criptografia assimétrica + OpenSSL | `private.pem` só aqui **assina**; `public.pem` já distribuído a scheduling/history |
| **Assinar** o token (não cifrar) | JWT = `header.payload.signature` | porta `TokenIssuer` + adapter (JJWT ou Nimbus) usando a privada, algoritmo `RS256` |
| Claims corretas | `iss` / `sub` / `scope` / `exp` | `iss=hospital-identity`, `sub=userId`, **`role`** (no lugar de `scope`), `aud`, `iat`, `exp`, `patientId` (só paciente) |
| **`exp` curto** | "após expirar, pede novo token" | TTL de 1h (`security.jwt.ttl-seconds: 3600`) |
| **Argon2id** para senha | lacuna da aula: **hash ≠ encriptação** (a aula só cobriu cifra reversível) | porta `PasswordHasher` + adapter `Argon2PasswordHasher` (o `application` não importa Spring Security) |
| **Stateless** | fluxo Stateless JWT | o identity só **emite**; não guarda sessão nem lista de tokens |
| Config **Spring Security 6.x** | módulo Spring Security | `SecurityFilterChain`: `/auth/login` liberado, `GET /users/**` conforme política, resto autenticado |
| Chave **fora do Git** | boa prática de gestão de chave | `private.pem` via env/secret em prod; ambos os `.pem` no `.gitignore` |
| **Erro de login genérico** | hardening | e-mail inexistente e senha errada → **mesma** resposta "credenciais inválidas" (não revelar qual falhou) |

### 6.2 Opcional — hardening, se sobrar tempo

| Item | De onde vem | Custo / como |
|---|---|---|
| **Rate limiting / lockout no login** | rate limiting (módulo do gateway) | bloquear após N tentativas falhas por e-mail/IP — contador em memória ou Bucket4j |
| **Refresh token** | OAuth 2.0 (access + refresh) | endpoint `/auth/refresh`; refresh de vida longa, revogável, guardado no banco |
| **CORS** | tema do gateway | `CorsConfigurationSource` se houver front-end no navegador |
| **Auditoria de login** | logs centralizados | tabela `login_audit` (quem, quando, sucesso/falha, IP) |
| **`aud` por serviço** | validação de escopo | audience distinto por Resource Server, validado no lado de cada um |

### 6.3 Fora do escopo — não fazer

| Item | Por quê |
|---|---|
| **Kong / API Gateway** | a arquitetura é sem gateway; cada serviço valida o JWT localmente |
| **mTLS entre serviços** | comunicação serviço→serviço é assíncrona via broker, não HTTP mútuo |
| **Login social Google / OIDC** | o `identity-service` é o próprio Authorization Server (e-mail/senha) |
| **AES para cifrar dados** | não há requisito de cifra de campo; senha usa **hash**, não cifra |
| **Spring Cloud Gateway / Access Token Pattern** | idem gateway |

### 6.4 Checklist de implementação do `identity-service`

- [ ] `git submodule update --init identity-service` (a pasta está vazia hoje)
- [ ] `pom.xml` (Boot **4.1**): web, security, data-jdbc, validation, actuator, mysql, flyway + lib de JWT (JJWT ou Nimbus)
- [ ] `V1__create_users.sql` — `users(id, email UNIQUE, password_hash, role, full_name, phone, crm, specialty)`
- [ ] `domain`: `User` + enum `Role` (`DOCTOR` | `NURSE` | `PATIENT`); regras (e-mail obrigatório, `crm`/`specialty` só para médico)
- [ ] `application/port`: `UserRepository`, `PasswordHasher`, `TokenIssuer`
- [ ] `application/usecase`: `RegisterUserUseCase`, `AuthenticateUseCase` (senha certa → token; errada → `InvalidCredentialsException`)
- [ ] `infrastructure`: `Argon2PasswordHasher`, `NimbusJwtTokenIssuer` (RS256, `private.pem`), `UserRepositoryImpl`, `UseCaseConfig` (`@Bean`)
- [ ] `infrastructure/web`: `POST /auth/login`, `POST /users` (registro), `GET /users/{id}` (para notification/history resolverem nomes)
- [ ] `SecurityConfig`: `BCryptPasswordEncoder`, `/auth/**` liberado, `GET /users/**` conforme política, resto autenticado, stateless
- [ ] testes: unit (`AuthenticateUseCaseTest`, `JwtTokenIssuerTest` — assina e valida com a pública), integração Testcontainers MySQL, JaCoCo ≥ 80%
- [ ] `README.md` do serviço + coleção Insomnia (login + registro + consulta)

> **Teste de ponta a ponta deste passo:** subir MySQL → subir o identity → `POST /auth/login`
> devolve `{ token }` → colar o token em jwt.io e conferir `role`, `sub`, `exp`.
