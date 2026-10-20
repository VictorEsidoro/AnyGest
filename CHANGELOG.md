# Changelog

Changes in AnyGest ([VictorEsidoro/AnyGest](https://github.com/VictorEsidoro/AnyGest)), based on [HyperGesture](https://github.com/h3nc4/HyperGesture) by Henrique Almeida ([h3nc4](https://github.com/h3nc4)). Up to 2.0.0 the project was published as a fork named HyperGesture.

Versões em português logo abaixo de cada versão.

## 3.0.1

### Changed

- The Control Center tile icon is larger and sharper: the A is about 20% bigger and the arrow has a thinner stroke with a shorter head, so it matches the weight of the system tiles and no longer looks lost inside the circle. Nothing else changes.

### Português

#### Alterado

- O ícone do tile da Central de Controle ficou maior e mais nítido: o A está cerca de 20% maior e a seta tem traço mais fino e ponta mais curta, para combinar com o peso dos tiles do sistema e não parecer perdido dentro do círculo. Mais nada muda.

## 3.0.0

A major version: the app gets its own identity. It is now **AnyGest**, with its own package name, credits and (see Changed) its own look. The gesture engine and its settings are unchanged.

**Breaking: 3.0.0 does not install over 2.0.0.** The package name changed, so Android treats AnyGest as a different app. Uninstall HyperGesture once before installing it (with both on, each gesture can fire twice). Settings are not carried over. From 3.0.0 on, every AnyGest update installs over the previous one, as long as the same signing key is kept.

### Changed

- **Name and package.** The app is called AnyGest and its package is now `io.github.victoresidoro.anygest` (it was `com.h3nc4.hypergesture`). This also renames the Accessibility service entry, the Control Center tile, the log tag and the debug override keys used by the e2e suite (`anygest_recents_hold_ms`, `anygest_app_switch`).
- **Credits and README.** Victor Eduardo (@victoresidoro) now comes first, as the author of AnyGest, followed by Henrique Almeida (h3nc4) as the creator of HyperGesture, the project AnyGest is based on, and the Circle to Search technique. The original copyright and the GPL-3.0 stay as they were; the README states both copyrights and warns people coming from HyperGesture to uninstall it first.
- **New icon.** Clean, thin-line Material style: the A of AnyGest, whose crossbar is an arrow coming from the right edge towards the left, Android's classic Back gesture. It has a themed version that follows the wallpaper colours on Android 13+, and the Control Center tile uses the same mark.
- **Card icons.** Each card in the app now has a thin-line [Material Symbols](https://fonts.google.com/icons) icon (Outlined, weight 200) in front of its title.
- **Play Protect warning.** The README now says up front, in Step 1 and in Common problems, that the first install of a new app can be blocked by Play Protect with only "App not installed" and no reason, and how to turn it off just for the install. Found while testing this version: the APK and its signature were fine.
- **Release file.** The APK is now `anygest.apk`, and the permanent download link is `https://github.com/VictorEsidoro/AnyGest/releases/latest/download/anygest.apk`. The repository was renamed to AnyGest; GitHub keeps redirecting the old HyperGesture links.

### Português

Versão major: o app ganha identidade própria. Agora ele é o **AnyGest**, com pacote, créditos e (veja Alterado) visual próprios. O motor de gestos e as configurações dele não mudaram.

**Quebra de compatibilidade: a 3.0.0 não instala por cima da 2.0.0.** O nome do pacote mudou, então o Android trata o AnyGest como outro app. Desinstale o HyperGesture uma vez antes de instalar (com os dois ligados, cada gesto pode disparar duas vezes). As configurações não são levadas. Da 3.0.0 em diante, toda atualização do AnyGest instala por cima da anterior, desde que a mesma chave de assinatura seja mantida.

#### Alterado

- **Nome e pacote.** O app se chama AnyGest e o pacote agora é `io.github.victoresidoro.anygest` (era `com.h3nc4.hypergesture`). Isso também renomeia o serviço de Acessibilidade, o atalho do Centro de controle, a tag dos logs e as chaves de ajuste usadas pelo teste e2e (`anygest_recents_hold_ms`, `anygest_app_switch`).
- **Créditos e README.** Victor Eduardo (@victoresidoro) agora vem primeiro, como autor do AnyGest, seguido de Henrique Almeida (h3nc4), criador do HyperGesture, projeto em que o AnyGest se baseia, e da técnica do Circule para pesquisar. O copyright original e a GPL-3.0 continuam como estavam; o README traz os dois copyrights e avisa quem vem do HyperGesture para desinstalá-lo antes.
- **Ícone novo.** Visual limpo e de traço fino, no estilo Material: o A do AnyGest, cuja travessa é uma seta vindo da borda direita em direção à esquerda, o gesto clássico de Voltar do Android. Tem versão temática, que acompanha as cores do papel de parede no Android 13+, e o atalho do Centro de controle usa a mesma marca.
- **Ícones nos cards.** Cada card do app ganhou um ícone [Material Symbols](https://fonts.google.com/icons) de traço fino (Outlined, peso 200) antes do título.
- **Aviso sobre o Play Protect.** O README agora diz logo de cara, no Passo 1 e em Problemas comuns, que a primeira instalação de um app novo pode ser barrada pelo Play Protect mostrando só "O app não foi instalado", sem o motivo, e como desligá-lo só durante a instalação. Descoberto testando esta versão: o APK e a assinatura estavam certos.
- **Arquivo da versão.** O APK agora é `anygest.apk`, e o link permanente de download é `https://github.com/VictorEsidoro/AnyGest/releases/latest/download/anygest.apk`. O repositório foi renomeado para AnyGest; o GitHub continua redirecionando os links antigos do HyperGesture.

## 2.0.0

A major version: the app's main screen and the whole setup flow were rebuilt around a guided checklist, so the app a new user meets is a different one. The gesture engine and its settings are unchanged, and 2.0.0 installs over 1.1.1.

### Added

- **First steps checklist** at the top of the app, replacing the Gesture service and Keep me running cards. Each step has a button to the right system screen and a colour: red to do, green done, amber for what the app cannot check on its own (with an "I did it" button), grey locked.
  - The essential steps unlock in order: allow restricted settings, turn HyperGesture on, battery with no restrictions, then autostart (HyperOS only). Nobody tries to switch the service on before Android allows it.
  - Restricted settings turn green on their own once the service has switched on, and stay green afterwards. The battery step reads HyperOS's "No restrictions" directly.
  - Finished steps keep their button, and a step marked done by hand can be unmarked, in case it turns out not to be.
  - Usage access and the Google assistant appear only when app switching or Circle to Search is on.
  - Folds to one line once everything is done, and opens again by itself if HyperOS stops the service.
- **Control Center tile.** Shows whether gestures are running; when they are not, tapping it opens HyperGesture's Accessibility page, so a stopped service with the button bar hidden no longer means restarting the phone. Its own card explains why it matters and offers to add it in one tap (Android 13+).
- **Button bar card**, replacing Navigation mode: shows whether the bar is hidden and opens the system screen. Hiding it stays locked until the gesture service runs, since a hidden bar with no gestures leaves no way to navigate.

### Changed

- **Lighter on battery.** The gesture service no longer subscribes to window-change events it never used, which woke it on every app or dialog switch all day long. Release builds also drop the debug logs written on every edge touch.
- **Credits** list everything this fork added, under Victor Eduardo (@victoresidoro), alongside the original author and the Circle to Search technique.
- The fork's CI also builds `dev/**` branches into a downloadable APK, without publishing a release, and repeats compiler, test and lint errors on the run's summary page. It runs on Brasília time, so files inside downloaded artifacts carry the local clock.

### Fixed

- The status bar is readable inside the app again: its icons were white on the light background, so the clock and battery disappeared.

### Português

Versão major: a tela principal e todo o fluxo de configuração foram refeitos em torno de uma lista guiada, então o app que um usuário novo encontra é outro. O motor de gestos e as configurações dele não mudaram, e a 2.0.0 instala por cima da 1.1.1.

#### Novidades

- **Lista de primeiros passos** no topo do app, no lugar dos cards Serviço de gestos e Mantenha o app ativo. Cada passo tem um botão para a tela certa do sistema e uma cor: vermelho pendente, verde feito, amarelo para o que o app não consegue conferir sozinho (com o botão "Já fiz"), cinza bloqueado.
  - Os passos essenciais liberam em ordem: permitir configurações restritas, ligar o HyperGesture, Economia de bateria em Nenhuma restrição e Início automático em segundo plano (só no HyperOS). Ninguém tenta ligar o serviço antes de o Android permitir.
  - As configurações restritas ficam verdes sozinhas quando o serviço liga, e continuam verdes depois. O passo da bateria lê o "Nenhuma restrição" do HyperOS diretamente.
  - Passos concluídos continuam com o botão, e um passo marcado à mão pode ser desmarcado, caso não esteja de fato feito.
  - Dados de uso do app e o assistente Google só aparecem quando a troca de apps ou o Circule para pesquisar estão ligados.
  - Encolhe para uma linha quando tudo está pronto, e abre de novo sozinha se o HyperOS parar o serviço.
- **Atalho no Centro de controle.** Mostra se os gestos estão funcionando; quando não estão, um toque abre a página do HyperGesture em Acessibilidade. Serviço parado com a barra de botões escondida não obriga mais a reiniciar o celular. Tem card próprio, que explica por que ele importa e oferece adicioná-lo com um toque (Android 13 ou mais novo).
- **Card Barra de botões**, no lugar de Modo de navegação: mostra se a barra está escondida e abre a tela do sistema. Esconder fica bloqueado até o serviço de gestos estar rodando, porque barra escondida sem gestos deixa você sem como navegar.

#### Alterações

- **Mais leve na bateria.** O serviço de gestos deixou de assinar eventos de troca de janela que nunca usou e que o acordavam a cada troca de app ou diálogo, o dia inteiro. As versões release também descartam os logs de depuração gravados a cada toque na borda.
- **Créditos** listam tudo o que este fork adicionou, com Victor Eduardo (@victoresidoro), ao lado do autor original e da técnica do Circule para pesquisar.
- O CI do fork também gera APK para download das branches `dev/**`, sem publicar release, e repete os erros de compilação, testes e lint na página de resumo da execução. Ele roda no horário de Brasília, então os arquivos dentro dos artefatos baixados trazem o horário local.

#### Correções

- A barra de status voltou a aparecer dentro do app: os ícones ficavam brancos sobre o fundo claro, e o relógio e a bateria sumiam.

## 1.1.1

### Fixed

- Each gesture vibrates once. Back (and the sideways app switch) used to buzz when the swipe armed and again when it fired on release, two quick buzzes in a row; it now buzzes only when the swipe arms, like the system Back gesture. Home and Recents, which buzz only when they fire, are unchanged.

### Português

#### Correções

- Cada gesto vibra uma vez só. O Voltar (e a troca de apps pela borda inferior) vibrava quando o gesto começava e de novo ao soltar o dedo, duas vibrações rápidas seguidas; agora vibra só no começo, como o gesto Voltar do sistema. Tela inicial e Apps recentes, que já vibravam uma vez só ao disparar, continuam iguais.

## 1.1.0

From this release on, versions follow [Semantic Versioning](https://semver.org/): patch for fixes, minor for new features, major for a full revamp. 1.1.0 is the first under that rule; it counts from the original HyperGesture 1.0.0 and replaces the `1.0.0-victor.1` name, whose hyphen marks a pre-release and would rank it below 1.0.0. The app installs over 1.0.0-victor.1.

### Fixed

- Credits name the fork's author as **Victor Eduardo (@victoresidoro)**, in the app (both languages), the READMEs and the copyright headers.

### Português

A partir desta versão, a numeração segue o [Versionamento Semântico](https://semver.org/lang/pt-BR/): patch para correções, minor para funções novas, major para uma reformulação total. A 1.1.0 é a primeira nessa regra; ela conta a partir do HyperGesture 1.0.0 original e substitui o nome `1.0.0-victor.1`, cujo hífen marca pré-lançamento e o colocaria abaixo da 1.0.0. O app instala por cima da 1.0.0-victor.1.

#### Correções

- Os créditos passam a citar o autor do fork como **Victor Eduardo (@victoresidoro)**: no app (nos dois idiomas), nos READMEs e nos cabeçalhos de copyright.

## 1.0.0-victor.1

Based on HyperGesture 1.0.0 (upstream commit `0922bda`).

### Added

- **Circle to Search on the gesture handle.** Touch and hold the middle of the bottom edge to start Circle to Search, the way a long press on the system navigation handle does. Off by default; turn it on in the new Circle to Search card.
  - Starts it without root or extra permissions, by asking `VoiceInteractionManagerService` to show the assistant session with the `omni.entry_point` argument. This is the technique of [MiCTS](https://github.com/parallelcc/MiCTS) by parallelcc and [CTSLauncher](https://github.com/chenxiaolong/CTSLauncher) by chenxiaolong.
  - Behaves like the system's own handle long press (as Launcher3 implements it): it waits for the platform long-press timeout, so it follows **Accessibility → Touch and hold delay**; it is cancelled past the platform touch slop; and it only counts over the 108dp handle, so long presses elsewhere along the edge still reach the app underneath. A manual hold time is available behind a switch.
  - Needs the Google app as the default digital assistant. A failure is shown on screen and in Diagnostics.
- **Brazilian Portuguese translation.** Every HyperOS screen and option mentioned in the app uses the exact pt-BR name HyperOS 3 shows. Serves Portuguese from any region.
- **Language picker** (Android 13+): Automatic (Portuguese when the phone is in Portuguese, English otherwise), Português (Brasil) or English. Also available in the system's per-app language setting.
- **Credits** section in the app, linking to every project this build relies on.

### Changed

- Long labels wrap instead of squeezing switches and buttons (translated names are much longer).
- "View source" points to this fork; the original project is linked under Credits.
- Fork-only CI workflow that runs the unit tests and lint, builds a signed release APK and publishes it as a GitHub Release with the `.apk` attached directly (no zip).

### Português

Baseado no HyperGesture 1.0.0 (commit `0922bda` do original).

#### Novidades

- **Circule para pesquisar na alça de navegação.** Toque e segure no meio da borda inferior para abrir o Circule para pesquisar, como ao tocar e pressionar a alça de navegação do sistema. Vem desligado; ative no card Circule para pesquisar.
  - Funciona sem root e sem permissão extra, pedindo ao `VoiceInteractionManagerService` que mostre a sessão do assistente com o argumento `omni.entry_point`. A técnica é a do [MiCTS](https://github.com/parallelcc/MiCTS), de parallelcc, e do [CTSLauncher](https://github.com/chenxiaolong/CTSLauncher), de chenxiaolong.
  - Comporta-se como o toque longo na alça do próprio sistema (como o Launcher3 implementa): usa o tempo de toque longo do sistema, acompanhando **Acessibilidade → Atraso ao tocar e pressionar**; é cancelado quando o dedo passa da tolerância de movimento do sistema; e só vale sobre a alça de 108 dp, então toques longos no resto da borda continuam chegando ao app que está embaixo. Há também a opção de tempo manual.
  - Precisa do Google como App assistente digital. Se falhar, o aviso aparece na tela e no Diagnóstico.
- **Tradução para português do Brasil.** Toda tela e opção do HyperOS citada no app usa o nome exato que o HyperOS 3 mostra em pt-BR. Vale para português de qualquer região.
- **Seletor de idioma** (Android 13+): Automático (português quando o telefone está em português, inglês nos demais casos), Português (Brasil) ou English. Também aparece na opção de idioma por app do sistema.
- Seção de **Créditos** no app, com links para todos os projetos em que esta versão se apoia.

#### Alterações

- Textos longos quebram a linha em vez de espremer chaves e botões.
- "Ver código-fonte" aponta para este fork; o projeto original está nos Créditos.
- Workflow de CI exclusivo do fork, que roda os testes e o lint, gera um APK release assinado e publica um Release no GitHub com o `.apk` direto para download, sem zip.
