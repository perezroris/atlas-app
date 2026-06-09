# Atlas — Navegação Climática

Aplicativo Android desenvolvido para a **FIAP Global Solution 2025**.

O Atlas é um app de navegação que integra dados climáticos em tempo real para tornar o deslocamento urbano mais seguro. Ele cruza a rota do usuário com informações de riscos ambientais — como focos de queimada e pontos de alagamento — e sugere automaticamente o caminho mais seguro quando detecta algum perigo no trajeto. A versão atual foca nos riscos mais comuns em São Paulo, mas a arquitetura foi pensada para suportar novos tipos de risco climático conforme o app evolui.

---

## Funcionalidades

- Mapa interativo com marcadores de queimada 🔴 e alagamento 🔵
- Toque em qualquer marcador para ver detalhes do risco
- Busca de endereços com autocomplete e histórico das últimas 5 pesquisas
- Botão GPS para usar a localização atual como ponto de partida
- Cálculo de rota com detecção automática de riscos no caminho
- Sugestão de rota alternativa mais segura com comparação de tempo
- Clima atual (temperatura, chuva, vento) nos pontos de origem e destino

---

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Kotlin 2.0 + Jetpack Compose | Linguagem e UI |
| Mapbox Maps SDK v11 | Mapa e desenho de rotas |
| Mapbox Directions API | Cálculo de rotas e alternativas |
| NASA FIRMS — VIIRS SNPP | Focos de queimada em tempo real |
| OpenWeatherMap | Clima atual e precipitação |
| Nominatim / OpenStreetMap | Geocoding e geocoding reverso |
| Retrofit + OkHttp | Chamadas REST |
| ViewModel + StateFlow + Coroutines | Arquitetura MVVM |

---

## Arquitetura

```
app/src/main/java/com/atlas/
├── data/
│   ├── api/          # interfaces Retrofit (FIRMS, Weather, Mapbox, Nominatim)
│   ├── model/        # data classes
│   └── repository/   # RiskRepository
├── ui/
│   ├── screens/      # HomeScreen, SplashScreen, AboutScreen
│   └── theme/
├── util/             # Constants, PolylineDecoder
└── viewmodel/        # HomeViewModel
```

Padrão **MVVM** com Repository. A UI observa o `HomeUiState` via `StateFlow` e só re-renderiza o que mudou.

---

## Como rodar

1. Clone o repositório
2. Abra no Android Studio
3. Aguarde o Gradle sincronizar
4. Rode em um emulador ou dispositivo com Android 8.0+ (API 26)

---

## APIs utilizadas

| API | Como obter a chave |
|---|---|
| Mapbox | [mapbox.com](https://mapbox.com) — crie uma conta e gere um token |
| NASA FIRMS | [firms.modaps.eosdis.nasa.gov](https://firms.modaps.eosdis.nasa.gov/api/) — gratuito |
| OpenWeatherMap | [openweathermap.org](https://openweathermap.org/api) — plano gratuito |

As chaves estão em `util/Constants.kt`.

---

## Requisitos

- Android Studio Hedgehog ou superior
- JDK 17
- Android 8.0+ (minSdk 26)
- Conexão com internet

---

## Telas

| Tela | Descrição |
|---|---|
| Splash | Animação de entrada com o ícone do app |
| Home | Mapa em tela cheia com marcadores, busca de rota e alertas |
| Detalhe do risco | Informações completas do ponto clicado |
| Sobre | Descrição do app, fontes de dados e tecnologias |

---

Projeto acadêmico — FIAP Global Solution 2025
