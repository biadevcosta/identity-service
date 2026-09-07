# identity-service — como testar quando o Docker estiver instalado

Checklist pra retomar do ponto exato onde paramos. O **código está 100% pronto** (60 testes unitários
passando, cobertura 97.8%, jar empacota). Falta só o que precisa de Docker: o teste de integração
com Testcontainers e rodar o serviço de verdade.

---

## 0. Pré-requisitos (instalar uma vez)

- [ ] **Docker Desktop** — instalar e **abrir pelo menos uma vez** (o daemon precisa estar rodando).
  ```
  ! winget install Docker.DockerDesktop
  ```
- [ ] **Temurin 21** (JDK) + `JAVA_HOME` no ambiente.
  ```
  ! winget install EclipseAdoptium.Temurin.21.JDK
  ```
- [ ] **Reabrir o terminal** depois de instalar (pra `docker` e `JAVA_HOME` aparecerem).

Conferir:
```bash
docker ps          # tem que responder sem erro
java -version       # tem que dizer 21
```

> **Se o Temurin não estiver instalado ainda**, dá pra rodar o Maven usando o JDK embutido do VS Code:
> ```bash
> JAVA_HOME="C:/Users/Mikael/.vscode/extensions/redhat.java-1.56.0-win32-x64/jre/21.0.12.1-win32-x86_64" ./mvnw ...
> ```

---

## 1. Rodar a bateria de testes completa (com a integração)

```bash
cd C:/Users/Mikael/bia-workspace/hospital-system/identity-service
./mvnw verify
```

**Esperado:** `BUILD SUCCESS`. Agora o `IdentityIntegrationTest` **roda de verdade** (antes ele
auto-pulava por falta de Docker) — ele sobe um MySQL em container, aplica as migrations, cria o admin
e testa 6 cenários por HTTP:

| Cenário | Esperado |
|---|---|
| login do admin semeado | `200` + `{ accessToken, refreshToken, tokenType: "Bearer", expiresIn: 3600 }` |
| refresh com o mesmo token 2×  | 1ª vez `200`, 2ª vez `401` (rotação: o token velho morre) |
| `POST /users` sem token | `401` |
| admin cadastra médico → `GET /users/{id}` | `201` e depois `200` com `name`/`role` certos |
| token de NURSE tenta `POST /users` | `403` |
| `GET /users/{id}` inexistente | `404` |

Também roda o gate de cobertura JaCoCo (mínimo 80% — hoje está em ~98%).

---

## 2. Subir o serviço

### Opção A — tudo em container (mais próximo do real)

```bash
cd identity-service
docker compose up --build
```

Sobe `mysql-identity` (porta 3309 no host) + `identity-service` (porta **8080**). O compose espera o
MySQL ficar *healthy* antes de subir o app. Flyway roda `V1`/`V2` e o `AdminSeeder` cria o admin.

### Opção B — MySQL em container, app na IDE/terminal (dev)

```bash
cd identity-service
docker compose up -d mysql-identity
./mvnw spring-boot:run
```

> ⚠️ Se a porta **8080** estiver ocupada, pare o que estiver usando ou mude `server.port`.

---

## 3. Testar na mão

Admin semeado: **`admin@hospital.local` / `admin12345`**.

```bash
# 1) login
curl -s localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@hospital.local","password":"admin12345"}'
# -> copie o "accessToken" da resposta

# 2) cadastrar um médico (troque $TOKEN pelo accessToken)
curl -s -i localhost:8080/users -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"email":"doc@hospital.local","password":"secret12345","role":"DOCTOR","fullName":"Dr House","crm":"CRM-1","specialty":"Diagnostics"}'
# -> 201, copie o "id" da resposta

# 3) ler o perfil (rota pública, usada por notification/history)
curl -s localhost:8080/users/<id>

# 4) refresh
curl -s localhost:8080/auth/refresh -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken do passo 1>"}'
```

- Cole o `accessToken` em <https://jwt.io> → confira as claims: `iss=hospital-identity`,
  `aud=hospital-services`, `sub`, `role`, `exp` (~1h à frente), e `patientId` só se for PATIENT.
- Ou importe **`identity.insomnia.json`** no Insomnia (tem as 4 requisições; preencha
  `access_token` / `refresh_token` / `user_id` no environment depois do login).

**Sinais de que deu certo:** login devolve os dois tokens; cadastrar sem token dá `401`;
com token de não-admin dá `403`; o médico aparece no `GET /users/{id}`; usar o refresh token duas
vezes seguidas → a segunda dá `401`.

---

## 4. Problemas comuns

| Sintoma | Causa / solução |
|---|---|
| `./mvnw verify` pula a integração | Docker Desktop não está aberto/rodando. Abra e tente de novo. |
| App não sobe: `cannot load RSA private key` | Falta `src/main/resources/private.pem`. Regenere (ver seção 5). |
| App não sobe: erro de conexão MySQL | MySQL ainda não está pronto. Na opção B, espere uns segundos após `docker compose up -d mysql-identity`. |
| Porta 8080 ocupada | Pare o outro processo ou mude `server.port` no `application.yaml`. |
| `docker` não encontrado | Reabra o terminal depois de instalar; confirme que o Docker Desktop está aberto. |

---

## 5. Chaves RSA (referência)

O par **real** já foi gerado e está em `keys/` + `identity-service/src/main/resources/`
(`private.pem` é git-ignored). Se precisar regerar (clone novo, ou a `private.pem` sumiu):

```bash
cd C:/Users/Mikael/bia-workspace/hospital-system
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private.pem
openssl rsa -in keys/private.pem -pubout -out keys/public.pem
cp keys/private.pem identity-service/src/main/resources/private.pem
cp keys/public.pem  identity-service/src/main/resources/public.pem
```

O par de **teste** (`src/test/resources/test-*.pem`) é commitado e independente — a integração não
depende da chave real.

> **Quando for integrar com `scheduling`/`history`:** copiar `keys/public.pem` para o
> `src/main/resources/` desses dois serviços também (hoje eles têm a chave pública antiga).

---

## 6. Arquivos importantes

| Arquivo | O quê |
|---|---|
| `plano-desenvolvimento.md` | roteiro dos 15 passos (todos ✅) + notas do skeleton/ambiente |
| `README.md` | doc do serviço: endpoints, arquitetura, config |
| `identity.insomnia.json` | coleção de requisições |
| `material-base-aula.md` | anotações de segurança da aula + o que virou escopo |
| `src/main/resources/application.yaml` | config (porta, datasource, `security.jwt.*`, `app.admin.*`) |
| `docker-compose.yml` | MySQL + app |
