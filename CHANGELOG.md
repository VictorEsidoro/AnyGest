# Changelog

Changes in AnyGest ([VictorEsidoro/AnyGest](https://github.com/VictorEsidoro/AnyGest)), based on [HyperGesture](https://github.com/h3nc4/HyperGesture) by Henrique Almeida ([h3nc4](https://github.com/h3nc4)). Up to 2.0.0 the project was published as a fork named HyperGesture.

Versões em português logo abaixo de cada versão.

## 3.6.0

### Added

- ADB mode: Android now wakes AnyGest whenever the list of enabled accessibility services changes, even with the app's process dead and without any polling. With *Switch the gestures back on by itself* on, AnyGest then does what opening the app would do: switches the gestures back on (shows the button bar first if it was left hidden), except after the tile's off switch or a pause for an exception app. Before, this only happened when the app was opened, the tile panel pulled down or the phone rebooted. To switch the gestures off on purpose, use the Control Center tile, or turn that switch off first.
- ADB mode: the "gestures stopped" notification (opt-in alerts) has a **Switch back on** button that switches the gestures on directly, with no settings screen.

### Português

#### Adicionado

- Modo ADB: o Android agora acorda o AnyGest sempre que a lista de serviços de acessibilidade ligados muda, mesmo com o processo do app morto e sem nenhuma verificação periódica. Com *Religar os gestos sozinho* ligado, o AnyGest faz o mesmo que ao abrir o app: religa os gestos (mostrando antes a barra de botões, se ela tiver ficado escondida), exceto depois do desligamento pelo atalho ou de uma pausa por app em exceção. Antes isso só acontecia ao abrir o app, puxar o painel do atalho ou reiniciar o celular. Para desligar os gestos de propósito, use o atalho do Centro de controle, ou desligue antes essa chave.
- Modo ADB: a notificação "gestos pararam" (avisos opcionais) ganhou o botão **Ligar de novo**, que religa os gestos direto, sem abrir tela de configurações.

## 3.5.1

### Added

- **AnyGest in Recents** card: shows or hides the AnyGest card in Recents. Swiping that card away ends the app and stops the gestures until Android brings it back; hiding it avoids that. Shown by default, so nothing changes unless you choose.

### Fixed

- Stuck edge strips are now released by how long the touch has been quiet rather than by a fixed 8 s from its start, each touch carries its own token so a late release of an old touch cannot affect a new one, and a strip taken off the screen mid-touch stops its timers. Based on HyperGesture commit 209e504 (#124) by Henrique Almeida (h3nc4).
- When the screen comes on, AnyGest checks that the edge strips are in place and touchable, and reinstalls them if not, so dead gestures are put right before the first swipe.
- The Control Center shortcut cards could say "Added" for shortcuts that were gone, for example after reinstalling: Android backup restored the old state, and with it the card hid its button, leaving no way to add the shortcut from the app. The device state is no longer backed up (the gesture settings still are), and **Add again** is always offered, since Android cannot tell an app whether its shortcut is still there.
- With ADB mode and *Keep the button bar hidden* on, if HyperOS switches the navigation mode back to buttons by itself, gesture navigation is put back at once, only while the gestures are running. Based on HyperGesture commit 81c1ad1 (#127) by Henrique Almeida (h3nc4). To change the mode by hand, turn that switch off first.

### Português

#### Adicionado

- Card **AnyGest nos Recentes**: mostra ou esconde o card do AnyGest nos Recentes. Deslizar esse card para fora encerra o app e para os gestos até o Android religá-lo; escondê-lo evita isso. Aparece por padrão, então nada muda se você não escolher.

#### Corrigido

- As faixas das bordas presas agora são liberadas pelo tempo que o toque ficou parado, e não por 8 s fixos desde o início; cada toque leva uma marca própria, para a liberação atrasada de um toque antigo não afetar um novo; e uma faixa removida da tela no meio de um toque para os próprios timers. Baseado no commit 209e504 (#124) do HyperGesture, de Henrique Almeida (h3nc4).
- Ao acender a tela, o AnyGest confere se as faixas das bordas estão no lugar e tocáveis, e as recria se não estiverem, para gestos mortos se resolverem antes do primeiro deslize.
- Os cards dos atalhos do Centro de controle podiam dizer "Adicionado" para atalhos que tinham sumido, por exemplo depois de reinstalar: o backup do Android restaurava o estado antigo, e o card escondia o botão, sem jeito de adicionar o atalho pelo app. O estado do aparelho não entra mais no backup (as configurações dos gestos continuam entrando), e o **Adicionar de novo** aparece sempre, porque o Android não informa a um app se o atalho dele ainda está lá.
- Com o Modo ADB e *Manter a barra de botões escondida* ligados, se o HyperOS voltar sozinho para a navegação por botões, os gestos são postos de volta na hora, só enquanto o AnyGest está funcionando. Baseado no commit 81c1ad1 (#127) do HyperGesture, de Henrique Almeida (h3nc4). Para trocar o modo à mão, desligue essa chave antes.

## 3.5.0

### Added

- New **Shortcuts through adb** card, always optional: ready-to-copy commands for battery optimisation (AnyGest and your default launcher, named automatically), usage access and restricted settings. Each shows its normal route through the Android screens, so adb is never required. The app only shows the commands and cannot run them.
- The card checks what is already done (ADB mode permission, battery optimisation for AnyGest and the launcher, usage access) every time you come back to the app, shows ✓ or Pending for each, and confirms when everything is done. Restricted settings cannot be checked from the app.

### Includes

- The 3.4.3 fix: the edge strips are released by a watchdog if a touch never ends, with a line in the Diagnostics log when it happens.

### Português

#### Adicionado

- Novo card **Atalhos por adb**, sempre opcional: comandos prontos para copiar para a economia de bateria (AnyGest e o seu launcher padrão, com o nome descoberto sozinho), o acesso de uso e as configurações restritas. Cada um mostra o caminho normal pelas telas do Android, então o adb nunca é obrigatório. O app só mostra os comandos e não consegue executá-los.
- O card confere o que já foi feito (permissão do Modo ADB, economia de bateria do AnyGest e do launcher, acesso de uso) toda vez que você volta ao app, mostra ✓ ou Pendente em cada item e confirma quando tudo está pronto. As configurações restritas o app não consegue conferir.

#### Inclui

- A correção da 3.4.3: o vigia que libera as faixas das bordas se um toque nunca terminar, com uma linha no registro do Diagnóstico quando acontece.

## 3.4.3

### Fixed

- Gestures that stopped working while the service still looked healthy (the button bar stayed hidden and only switching the service off and on from the tile brought them back). While a touch is being handled the edge strips are made inert; if the end of that touch never reached them, they stayed inert. A watchdog now releases them after 8 s and writes a line to the Diagnostics log ("strips were stuck inert") whenever it has to, so a repeat is visible.

### Português

#### Corrigido

- Gestos que paravam de funcionar com o serviço aparentemente saudável (a barra de botões continuava escondida e só desligar e ligar o serviço pelo tile os trazia de volta). Enquanto um toque é tratado, as faixas das bordas ficam inertes; se o fim desse toque não chegasse a elas, continuavam inertes. Agora um vigia as libera após 8 s e escreve uma linha no registro do Diagnóstico ("strips were stuck inert") sempre que precisar, para uma repetição ficar visível.

## 3.4.2

### Added

- The Diagnostics log now records when the service connects, is unbound or destroyed, and whether the previous stop was unclean (the process was killed without a callback). The log keeps the last 20 entries instead of 8.

### Fixed

- With ADB mode on, if the service went down without being paused on purpose and the button bar was left hidden, the bar is now shown again as soon as the app, the tile panel or a reboot gets a chance to run, so the phone is never left without navigation.

### Português

#### Adicionado

- O registro do Diagnóstico agora anota quando o serviço conecta, é desvinculado ou destruído, e se a parada anterior foi abrupta (o processo foi morto sem aviso). O registro guarda as últimas 20 linhas, em vez de 8.

#### Corrigido

- Com o modo ADB ligado, se o serviço caísse sem ter sido pausado de propósito e a barra de botões ficasse escondida, ela agora volta assim que o app, o painel do tile ou uma reinicialização tiverem chance de rodar, para o celular nunca ficar sem navegação.

## 3.4.1

### Fixed

- Lower battery and CPU use: with an empty exception list (the default), AnyGest no longer checks the foreground app every 1.5 s nor subscribes to window events. The check now runs only when at least one app is in the list.

### Português

#### Corrigido

- Menos bateria e processamento: com a lista de exceções vazia (o padrão), o AnyGest não consulta mais o app em primeiro plano a cada 1,5 s nem assina eventos de janela. A verificação só roda quando há pelo menos um app na lista.

## 3.4.0

### Added

- **ADB mode**, fully optional and not needed for normal use. A one-time `pm grant io.github.victoresidoro.anygest android.permission.WRITE_SECURE_SETTINGS` (over adb, or from an app such as aShell with Shizuku — how it was tested, on a Poco F8 Ultra / HyperOS) gives AnyGest the one permission that was out of reach, and nothing more. It is expected to stay after the debugging is turned off (to be confirmed per device: the card shows whether it did). The new **ADB mode** card shows and copies the command and reports whether it took. Without the grant nothing changes: every feature below is a no-op.
- With ADB mode, AnyGest **keeps the HyperOS button bar hidden** by itself (`force_fsg_nav_bar`, found on a Poco F8 Ultra, where `navigation_mode` stays at buttons while this key hides the bar) and puts it back when the service stops on purpose or by itself, so the phone is never left with no navigation. It writes the key on HyperOS only, and only if the switch in the card is on.
- With ADB mode, AnyGest **switches its own accessibility service back on** at boot, when the app opens and when the Control Center panel is pulled down (the moments a killed app still gets to run). Not after you turned it off with the tile, and a switch in the card turns it off.
- **Exception apps**: a list, empty by default, editable in the app (with a search box when adding) and kept in the settings backup, of apps that refuse to run while an accessibility service is on. When one comes to the front AnyGest shows the button bar, takes itself out of the enabled services and starts a small foreground watcher (a quiet notification); when you leave the app the service is switched back on and the bar hidden again. It needs ADB mode and usage access, and it is experimental: an app that checks the instant it opens may still see the service. The card carries a warning that this works around a check some apps make on purpose, that the app may still refuse to run or may treat it as a security bypass, and that it is used at your own risk.
- A second way to notice a listed app, for apps that hide their windows from services so no window event ever arrives: while the screen is on and the list is in use, the foreground app is also read every 1.5 s from the usage events (a little late, but nothing the app can block). It stops with the screen.
- With ADB mode the **Control Center tile is a switch for the gestures**: off shows the button bar and turns the service off, on brings both back. Without ADB mode the tile behaves as before.
- The service asks for window events at runtime, and only for the listed apps and only with ADB mode set up, so no other app wakes it, and only the package name is ever read. ADB mode is also shown in Diagnostics, together with a short log of the last pause decisions (what the service saw and did), since the service is off for most of a pause.

### Português

#### Adicionado

- **Modo ADB**, totalmente opcional, dispensável no uso normal. Um `pm grant io.github.victoresidoro.anygest android.permission.WRITE_SECURE_SETTINGS` rodado uma vez (por adb, ou num app como o aShell com Shizuku — como foi testado, num Poco F8 Ultra / HyperOS) dá ao AnyGest a única permissão que estava fora de alcance, e nada além disso. Ela deve continuar depois de desligar a depuração (a confirmar em cada aparelho: o card mostra se continuou). O novo card **Modo ADB** mostra e copia o comando e informa se pegou. Sem a permissão nada muda: tudo abaixo não faz nada.
- Com o Modo ADB, o AnyGest **mantém a barra de botões do HyperOS escondida** sozinho (`force_fsg_nav_bar`, descoberta num Poco F8 Ultra, onde o `navigation_mode` continua em botões e essa chave é que esconde a barra) e a devolve quando o serviço para, de propósito ou não, para o celular nunca ficar sem navegação. Só escreve a chave no HyperOS, e só se a chave no card estiver ligada.
- Com o Modo ADB, o AnyGest **religa o próprio serviço de acessibilidade** ao ligar o celular, ao abrir o app e ao puxar o painel do Centro de controle (os momentos em que um app morto ainda consegue rodar). Não religa se você desligou pelo atalho, e uma chave no card desliga isso.
- **Apps em exceção**: uma lista, vazia por padrão, editável no app (com caixa de busca ao adicionar) e incluída no backup das configurações, de apps que se recusam a funcionar com um serviço de acessibilidade ligado. Quando um deles vai para a frente, o AnyGest mostra a barra de botões, se tira da lista de serviços ativos e inicia um pequeno vigia em primeiro plano (notificação discreta); quando você sai do app o serviço é religado e a barra escondida de novo. Precisa do Modo ADB e do acesso de uso, e é experimental: um app que confere no instante em que abre ainda pode ver o serviço. O card traz um aviso de que isso contorna de propósito uma checagem que alguns apps fazem, que o app pode continuar recusando ou tratar isso como um contorno de segurança, e que o uso é por sua conta e risco.
- Uma segunda forma de notar um app da lista, para apps que escondem as janelas dos serviços e por isso nenhum aviso de janela chega: com a tela ligada e a lista em uso, o app em primeiro plano também é lido a cada 1,5 s nos eventos de uso (um pouco atrasado, mas algo que o app não consegue bloquear). Para junto com a tela.
- Com o Modo ADB o **atalho do Centro de controle vira um liga/desliga dos gestos**: desligar mostra a barra de botões e desliga o serviço, ligar traz os dois de volta. Sem o Modo ADB o atalho se comporta como antes.
- O serviço pede eventos de janela em tempo de execução, só para os apps da lista e só com o Modo ADB configurado, então nenhum outro app o acorda, e só o nome do pacote é lido. O Modo ADB também aparece no Diagnóstico, junto com um pequeno registro das últimas decisões de pausa (o que o serviço viu e fez), já que o serviço fica desligado durante quase toda a pausa.

## 3.3.0

### Added

- Optional **Alerts** (off by default, turned on from the new Alerts card): a quiet notification when the gesture service stops, and another when the system brings the button bar back on its own, each with a tap that opens the screen that fixes it. Android does not let an app switch either one back on (`WRITE_SECURE_SETTINGS` is signature-level), so this only shortens the trip. Turning the service off yourself also shows the alert, and it clears by itself once the service is back. Turning it on asks for the Android 13+ notification permission, only then.
- A new optional step in the setup checklist, HyperOS only: **Lock AnyGest in Recents**. The lock on the Recents card is what keeps "clear all" from ending the service. HyperOS does not let apps check it, so it is confirmed by hand like Autostart, and the step has an **Open Recents** button.

### Português

#### Adicionado

- **Alertas** opcionais (desligados por padrão, ativados no novo card Alertas): uma notificação discreta quando o serviço de gestos para, e outra quando o sistema traz a barra de botões de volta sozinho, cada uma com um toque que abre a tela que resolve. O Android não deixa um app religar nenhum dos dois (`WRITE_SECURE_SETTINGS` é de assinatura), então isso só encurta o caminho. Desligar o serviço você mesmo também mostra o alerta, e ele some sozinho quando o serviço volta. Ao ativar, o app pede a permissão de notificações do Android 13+, só nesse momento.
- Um novo passo opcional no checklist de primeiros passos, só no HyperOS: **Travar o AnyGest nos Recentes**. O cadeado no card dos Recentes é o que impede o "limpar tudo" de encerrar o serviço. O HyperOS não deixa o app conferir isso, então é confirmado na mão, igual ao Início automático, e o passo tem um botão **Abrir Recentes**.

## 3.2.0

### Added

- A second, independent Control Center tile: when HyperOS reverts to the button bar on its own, it opens the HyperOS navigation-mode screen in one tap instead of hunting through Settings. It shows the current mode (gestures or buttons) and cannot switch it itself — `WRITE_SECURE_SETTINGS` is signature-level, so only the user can — it just gets you to the right screen. HyperOS-only, same as the existing "Button bar" card. Add it from the app the same way as the first shortcut, or edit Control Center by hand.

### Português

#### Adicionado

- Um segundo atalho no Centro de Controle, independente do primeiro: se o HyperOS voltar sozinho para a barra de botões, ele abre a tela de modo de navegação do HyperOS num toque, sem precisar procurar nas Configurações. Mostra o modo atual (gestos ou botões) e não troca sozinho — `WRITE_SECURE_SETTINGS` é de assinatura, só o usuário pode — só poupa a ida até a tela certa. Só aparece em aparelhos HyperOS, igual o card "Barra de botões" já existente. Adicione pelo app, do mesmo jeito que o primeiro atalho, ou edite o Centro de Controle manualmente.

## 3.1.1

### Fixed

- The Control Center tile could silently do nothing when tapped with the gesture service off, instead of opening Accessibility. AOSP guards the details page it tries first with a signature permission this app does not hold, and a denied start from a tile fails without even an exception, so the previous check (whether something could resolve the intent) was not enough to know the tap would actually work. The tile now checks the target's required permission before using it, falling back to the general Accessibility list otherwise, the same way the in-app "Open Accessibility" button already did. Taken from HyperGesture ([h3nc4/HyperGesture](https://github.com/h3nc4/HyperGesture)), which ported this file from AnyGest and added the permission check independently.

### Changed

- The Credits screen and the README now name what Henrique Almeida (h3nc4) contributed directly to AnyGest after the fork (the cancelled back swipe and this release's tile fix), the same way HyperGesture's own Credits name what was ported from AnyGest.
- The README's Play Protect note now covers the common case too: a "This app looks safe" screen with an Install button, which just needs a tap, not the Play Protect toggle. Screenshot included.

### Português

#### Corrigido

- O atalho do Centro de Controle podia não fazer nada ao ser tocado com o serviço de gestos desligado, em vez de abrir a Acessibilidade. A tela de detalhes que ele tentava primeiro é protegida pelo Android por uma permissão de assinatura que este app não tem, e uma abertura negada a partir de um atalho falha sem nem lançar uma exceção, então a verificação anterior (se algo resolvia a intenção) não garantia que o toque funcionaria de fato. O atalho agora confere a permissão exigida pelo alvo antes de usá-lo, caindo para a lista geral de Acessibilidade quando falta, do mesmo jeito que o botão "Abrir Acessibilidade" de dentro do app já fazia. Trazido do HyperGesture ([h3nc4/HyperGesture](https://github.com/h3nc4/HyperGesture)), que portou este arquivo do AnyGest e adicionou essa verificação de permissão por conta própria.

#### Alterado

- A tela de Créditos e o README agora nomeiam o que o Henrique Almeida (h3nc4) contribuiu direto para o AnyGest depois do fork (o Voltar revertido cancelável e a correção do atalho desta versão), do mesmo jeito que os Créditos do HyperGesture nomeiam o que foi portado do AnyGest.
- O aviso do Play Protect no README agora cobre também o caso comum: uma tela "Este app parece seguro" com botão Instalar, que só precisa de um toque, sem mexer na chave do Play Protect. Com print.

## 3.1.0

### Added

- Vibration strength: light, normal or strong, chosen under "Gesture settings" next to the on/off switch. Normal is the system vibration AnyGest always used, so nothing changes unless you pick another. Light and strong drive the vibrator directly with their own length and power (the system's haptic presets feel the same on HyperOS), which adds the VIBRATE permission; Android grants it at install and never asks.
- Back up and restore the settings: "Export" saves them to a small text file you choose, "Import" reads one back, on this phone or another. Values out of range or unknown in the file are skipped, never applied.

### Changed

- A back swipe brought back to the edge before lifting the finger is now cancelled instead of going back, and pushing out again re-arms it, like the system's own Back. The belly follows the finger and recedes when it is cancelled. Taken from HyperGesture ([h3nc4/HyperGesture](https://github.com/h3nc4/HyperGesture), commit `c8b2e6f`, "cancel reverted back swipe"), adapted to AnyGest.

### Removed

- The SonarQube scan and everything that fed it: the CI job, `sonar-project.properties`, the Renovate rules, the SonarLint setup in the Dev Container and the `sonar` call in the pre-push hook. It pointed at the original author's private SonarQube server, which this project has no token for, so it failed every day. Lint, unit tests, coverage report and the debug build still run. Nothing in the app changes.

### Português

#### Adicionado

- Força da vibração: leve, normal ou forte, escolhida em "Configurações dos gestos", ao lado do liga/desliga. Normal é a vibração do sistema que o AnyGest sempre usou, então nada muda se você não escolher outra. Leve e forte acionam o vibrador direto, com duração e potência próprias (as vibrações prontas do sistema ficam iguais no HyperOS), o que acrescenta a permissão VIBRATE; o Android concede na instalação e não pergunta nada.
- Backup e restauração das configurações: "Exportar" salva num arquivo de texto pequeno que você escolhe, "Importar" lê de volta, neste ou em outro celular. Valores fora do limite ou desconhecidos no arquivo são ignorados, nunca aplicados.

#### Alterado

- O Voltar pela borda que é trazido de volta para a borda antes de soltar o dedo agora é cancelado, em vez de voltar, e empurrar de novo o rearma, como o Voltar do próprio sistema. A "barriga" acompanha o dedo e recolhe quando cancelado. Trazido do HyperGesture ([h3nc4/HyperGesture](https://github.com/h3nc4/HyperGesture), commit `c8b2e6f`, "cancel reverted back swipe"), adaptado ao AnyGest.

#### Removido

- A análise SonarQube e tudo que dependia dela: o job de CI, o `sonar-project.properties`, as regras do Renovate, a configuração do SonarLint no Dev Container e a chamada `sonar` no hook de pre-push. Ela apontava para o servidor SonarQube privado do autor original, para o qual este projeto não tem token, e por isso falhava todo dia. Lint, testes unitários, relatório de cobertura e a build de debug continuam rodando. O app não muda.

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
