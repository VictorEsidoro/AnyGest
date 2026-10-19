# Changelog

Changes in this fork ([VictorEsidoro/HyperGesture](https://github.com/VictorEsidoro/HyperGesture)) on top of [HyperGesture](https://github.com/h3nc4/HyperGesture) by Henrique Almeida ([h3nc4](https://github.com/h3nc4)).

Versões em português logo abaixo de cada versão.

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
- Fork-only CI workflow that runs the unit tests and lint and builds a signed release APK.

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
- Workflow de CI exclusivo do fork, que roda os testes e o lint e gera um APK release assinado.
