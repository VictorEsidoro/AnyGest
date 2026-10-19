# Changelog

Changes in this fork ([VictorEsidoro/HyperGesture](https://github.com/VictorEsidoro/HyperGesture)) on top of [HyperGesture](https://github.com/h3nc4/HyperGesture) by Henrique Almeida ([h3nc4](https://github.com/h3nc4)).

Versões em português logo abaixo de cada versão.

## 1.2.0

### Added

- **First steps checklist** at the top of the app, replacing the Gesture service, Keep me running and Navigation mode cards. Each step has a button to the right system screen and a colour: red to do, green done, amber for what the app cannot check on its own (with an "I did it" button), grey optional.
  - The essential steps unlock in order: allow restricted settings, turn HyperGesture on, battery with no restrictions, then autostart (HyperOS only). Nobody tries to switch the service on before Android allows it.
  - Restricted settings turn green on their own once the service has switched on, and stay green afterwards.
  - Hiding the button bar stays locked until the gesture service runs, since a hidden bar with no gestures leaves no way to navigate.
  - Usage access and the Google assistant appear only when app switching or Circle to Search is on.
  - Folds to one line once everything is done, and opens again by itself if HyperOS stops the service.
- **Control Center tile.** Shows whether gestures are running; when they are not, tapping it opens HyperGesture's Accessibility page, so a stopped service with the bar hidden no longer means restarting the phone. The checklist offers to add it in one tap (Android 13+).

### Changed

- The fork's CI also builds `dev/**` branches into a downloadable APK, without publishing a release.

### Português

#### Novidades

- **Lista de primeiros passos** no topo do app, no lugar dos cards Serviço de gestos, Mantenha o app ativo e Modo de navegação. Cada passo tem um botão para a tela certa do sistema e uma cor: vermelho pendente, verde feito, amarelo para o que o app não consegue conferir sozinho (com o botão "Já fiz"), cinza opcional.
  - Os passos essenciais liberam em ordem: permitir configurações restritas, ligar o HyperGesture, Economia de bateria em Nenhuma restrição e Início automático em segundo plano (só no HyperOS). Ninguém tenta ligar o serviço antes de o Android permitir.
  - As configurações restritas ficam verdes sozinhas quando o serviço liga, e continuam verdes depois.
  - Esconder a barra de botões fica bloqueado até o serviço de gestos estar rodando, porque barra escondida sem gestos deixa você sem como navegar.
  - Dados de uso do app e o assistente Google só aparecem quando a troca de apps ou o Circule para pesquisar estão ligados.
  - Encolhe para uma linha quando tudo está pronto, e abre de novo sozinha se o HyperOS parar o serviço.
- **Atalho no Centro de controle.** Mostra se os gestos estão funcionando; quando não estão, um toque abre a página do HyperGesture em Acessibilidade. Serviço parado com a barra escondida não obriga mais a reiniciar o celular. A lista oferece adicionar o atalho com um toque (Android 13 ou mais novo).

#### Alterações

- O CI do fork também gera APK para download das branches `dev/**`, sem publicar release.

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
