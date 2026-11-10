# Changelog

Changes in AnyGest ([VictorEsidoro/AnyGest](https://github.com/VictorEsidoro/AnyGest)), based on [HyperGesture](https://github.com/h3nc4/HyperGesture) by Henrique Almeida ([h3nc4](https://github.com/h3nc4)). Up to 2.0.0 the project was published as a fork named HyperGesture.

Versões em português logo abaixo de cada versão.

## 4.4.2

### Fixed

- A finger or palm resting on an edge while holding the phone (watching a video, scrolling with the other hand) left every edge without gestures for up to 2 seconds. A touch that stays still and never becomes a gesture now gives the other edges back after half a second, and no longer takes them again if it twitches. A press released within 2 seconds is still passed on to the app underneath.

### Changed

- The service log tells an app update apart from the system killing the app: "service started after an app update" instead of the "unclean stop" line every time a new version was installed.
- When gesture navigation is put back, the log says whether the button bar was AnyGest's own doing (a pause) or the system's, and for the system's it names the app that was in front.
- When the edge strips are released by the safety check, the log adds the last touch seen, the screen orientation and the app in front.
- The log keeps 30 lines instead of 20.
- README: the edge animation, the landscape double swipe, the side edges, Pause 10 min and Copy log are now described.

### Português

#### Corrigido

- Um dedo ou a palma apoiados numa borda ao segurar o celular (vendo um vídeo, rolando com a outra mão) deixavam todas as bordas sem gestos por até 2 segundos. Um toque que fica parado e não vira gesto agora devolve as outras bordas depois de meio segundo, e não as toma de novo se tremer. Um toque solto em até 2 segundos continua sendo repassado ao app de baixo.

#### Alterado

- O registro do serviço separa uma atualização do app de o sistema ter encerrado o app: "service started after an app update" no lugar da linha de "unclean stop" a cada versão nova instalada.
- Quando a navegação por gestos é recolocada, o registro diz se a barra de botões tinha sido mostrada pelo próprio AnyGest (uma pausa) ou pelo sistema, e no segundo caso informa o app que estava na frente.
- Quando as faixas das bordas são liberadas pela trava de segurança, o registro traz o último toque visto, a orientação da tela e o app na frente.
- O registro guarda 30 linhas em vez de 20.
- README: a animação nas bordas, o deslize duplo em paisagem, as bordas laterais, o Pausar 10 min e o Copiar log agora estão descritos.

## 4.4.1

### Fixed

- Pausing for an app on the exceptions list (or switching the gestures off) cleared the "hide the bar" setting but left gesture navigation on, so the button bar never came back. It now switches to the button bar too, and the gestures put gesture navigation back when they return.

### Changed

- In Diagnostics the Copy log, Refresh and View source buttons are now at the top, above the details.
- The pause notes say what the bar and the navigation mode were when a pause began.

### Português

#### Corrigido

- Pausar para um app da lista de exceções (ou desligar os gestos) tirava o "esconder a barra", mas deixava a navegação por gestos ligada, e a barra de botões nunca voltava. Agora ela também muda para a barra de botões, e os gestos recolocam a navegação por gestos quando voltam.

#### Alterado

- Em Diagnóstico, os botões Copiar log, Atualizar e Ver código-fonte ficam no topo, acima dos detalhes.
- As anotações de pausa dizem como estavam a barra e o modo de navegação quando a pausa começou.

## 4.4.0

### Fixed

- When a swipe's end never reached the edge strips, the gestures stayed dead for 5 seconds before the safety release. It now takes 2 seconds, and only a touch being passed on to the app keeps the longer wait.

### Added

- **Animation on the edges** (Gesture settings, off by default). When you start a swipe from an edge, a shape rises from it under your finger, follows you along the edge and sinks back when you let go. From the bottom it carries a Home icon, from the sides the Back arrow. Pick the style: **Lens** (a smooth bump in the edge), **Lens with arrow** (the arrow from the logo instead of the icon: up from the bottom, back toward the edge on the sides), or a small **light dot** / **dark dot** that hugs the edge, follows your finger and opens up a little when the gesture is about to fire. With one on, it replaces the plain back indicator on the sides. It is only drawn while a swipe is under way, never takes touches, and leaves the screen before any touch is passed on to the app.

### Português

#### Corrigido

- Quando o fim de um deslize não chegava às faixas das bordas, os gestos ficavam parados por 5 segundos até a liberação de segurança. Agora leva 2 segundos, e só um toque sendo repassado ao app mantém a espera maior.

#### Adicionado

- **Animação nas bordas** (ajustes dos gestos, desligada por padrão). Ao começar um deslize por uma borda, uma forma sobe dela por baixo do seu dedo, acompanha você ao longo da borda e volta para dentro quando você solta. Embaixo ela leva o ícone de Início, nas laterais a seta de Voltar. Escolha o estilo: **Lente** (uma curva suave na borda), **Lente com seta** (a seta do logo no lugar do ícone: para cima embaixo, de volta para a borda nas laterais) ou um pequeno **ponto claro** / **ponto escuro** que fica colado na borda, acompanha o dedo e se abre um pouco quando o gesto está para disparar. Ligada, ela substitui o indicador simples de voltar das laterais. Só é desenhada enquanto o deslize acontece, não recebe toques e sai da tela antes de qualquer toque ser repassado ao app.

## 4.3.0

### Added

- **Swipe up twice to leave a landscape app** (Gesture settings, on by default). With the phone on its side, the first bottom swipe (Home, Recents, previous or next app) only shows a small pill, and a second swipe within 2 seconds goes ahead, as HyperOS does in games. It stops an accidental swipe from throwing you out of a game or a video. Turn it off in Gesture settings to go back to the old behaviour. Based on HyperGesture by Henrique Almeida (h3nc4).

### Fixed

- **Restore defaults** in Gesture settings also wiped things that live on other screens: the exception app list, app switching and Circle to Search. It now restores only the values that screen shows. Based on HyperGesture by Henrique Almeida (h3nc4).
- The settings file no longer breaks the app if it gets damaged (a power cut halfway through a write, say): it reads as empty and the defaults apply. Based on work by Rohith-E-S on HyperGesture.
- The accessibility service rebuilds its settings reader when it is connected again on the same instance, instead of keeping one that was already cancelled.
- A touch replayed to the app underneath is no longer cut short after 400 samples (under 2 seconds on a 240 Hz screen); it now keeps up to 4000.

### Changed

- **Copy log** now copies a short report with the version, device, Android and HyperOS, navigation mode, service state and ADB mode, followed by the recorded failures and pause notes, so one paste says what the phone is. Layout inspired by Rohith-E-S's HyperGesture fork.

### Português

#### Adicionado

- **Deslizar duas vezes para sair de um app deitado** (ajustes dos gestos, ligado por padrão). Com o celular deitado, o primeiro deslize da borda de baixo (Início, Recentes, app anterior ou seguinte) só mostra uma pílula pequena, e um segundo deslize em até 2 segundos executa, como a HyperOS faz em jogos. Evita que um deslize sem querer tire você de um jogo ou vídeo. Desligue nos ajustes dos gestos para voltar ao comportamento antigo. Baseado no HyperGesture, de Henrique Almeida (h3nc4).

#### Corrigido

- **Restaurar padrões** nos ajustes dos gestos também apagava coisas de outras telas: a lista de apps de exceção, a troca de apps e o Circle to Search. Agora restaura só os valores daquela tela. Baseado no HyperGesture, de Henrique Almeida (h3nc4).
- O arquivo de ajustes não derruba mais o app se for danificado (uma queda de energia no meio da gravação, por exemplo): ele é lido como vazio e valem os padrões. Baseado em trabalho de Rohith-E-S no HyperGesture.
- O serviço de acessibilidade recria o leitor de ajustes quando é conectado de novo na mesma instância, em vez de manter um que já estava cancelado.
- Um toque repassado ao app por baixo não é mais cortado depois de 400 amostras (menos de 2 segundos numa tela de 240 Hz); agora guarda até 4000.

#### Alterado

- **Copiar log** agora copia um relatório curto com versão, aparelho, Android e HyperOS, modo de navegação, estado do serviço e Modo ADB, seguido das falhas registradas e das anotações da pausa, e uma colagem já diz como é o aparelho. Formato inspirado no fork do HyperGesture de Rohith-E-S.

## 4.2.0

### Added

- **Side edges length and position** (Gestures section). The two side strips can now be shortened (30 to 100% of the screen height) and sit at the top, middle or bottom of the edge, so back swipes stop clashing with side menus or buttons near the top of apps.
- **Extra click when the gesture fires** (off by default, under Vibrate). Arming keeps its light tick and firing adds a firmer click, so the two read as two steps. It follows the vibration strength you chose.
- **Pause 10 min** button on the keep-alive notification (ADB mode). The gestures go off and come back by themselves ten minutes later, handy for a game or a video. Switching on by hand cancels the countdown.

### Português

#### Adicionado

- **Comprimento e posição das bordas laterais** (seção Gestos). As duas bordas laterais agora podem ser encurtadas (de 30 a 100% da altura da tela) e ficar no topo, no meio ou embaixo, para o voltar não brigar com menus laterais ou botões perto do topo dos apps.
- **Clique extra quando o gesto é executado** (desligado por padrão, em Vibrar). Armar mantém o toque leve e disparar acrescenta um clique mais firme, e os dois se distinguem. Segue a força de vibração escolhida.
- Botão **Pausar 10 min** na notificação de manter vivo (modo ADB). Os gestos desligam e voltam sozinhos dez minutos depois, bom para jogo ou vídeo. Ligar na mão cancela a contagem.

## 4.1.1

### Changed

- Less battery while apps are on the exception list: AnyGest used to re-read the last 30 seconds of app-switch events every 1.5 seconds, and, during a pause, everything since the pause began every second. It now remembers where it stopped and reads only what is new. Same result, far less work.
- While gestures are paused for an app, the watcher no longer wakes up with the screen off. It sleeps, and checks once as soon as the screen turns on.

### Português

#### Alterado

- Menos bateria com apps na lista de exceções: o AnyGest relia os últimos 30 segundos de trocas de app a cada 1,5 s e, durante uma pausa, tudo desde o início da pausa a cada segundo. Agora ele lembra onde parou e lê só o que é novo. Mesmo resultado, bem menos trabalho.
- Com os gestos pausados para um app, o vigia não acorda mais com a tela apagada. Ele dorme e confere uma vez assim que a tela acende.

## 4.1.0

### Added

- **Shortcut without gestures** for apps on the exception list (ADB mode and usage access). From the list, an app opens with the gestures on and the pause comes a moment later; some apps, such as banking apps, check for accessibility services the moment they open and can notice AnyGest in that moment. The shortcut places an icon on the home screen, the app's own with a small AnyGest badge, that switches the gestures off **before** opening the app, so it never sees AnyGest. Not needed for apps that already work fine with the list alone. They come back when you leave the app, as with any pause. Your launcher must accept shortcuts from apps (Nova does).

### Português

#### Adicionado

- **Atalho sem gestos** para os apps da lista de exceções (Modo ADB e acesso de uso). Pela lista, o app abre com os gestos ligados e a pausa vem um instante depois; alguns apps, como os de banco, conferem os serviços de acessibilidade logo ao abrir e podem perceber o AnyGest nesse instante. O atalho coloca um ícone na tela inicial, o do próprio app com um pequeno selo do AnyGest, que desliga os gestos **antes** de abrir o app, então ele nunca vê o AnyGest. Não é necessário para apps que já funcionam bem só com a lista. Eles voltam quando você sai do app, como em qualquer pausa. O launcher precisa aceitar atalhos de apps (o Nova aceita).

## 4.0.0

### Changed

- New home screen. The app had grown into one long list of cards; it now opens on a search box, the **First steps** checklist and five sections: **Gestures**, **Bar and shortcuts**, **Alerts and app**, **ADB mode** and **Diagnostics and credits**. Each section opens its own screen with a back arrow, and Android's back gesture returns home. Every card is still there and works as before; only where it lives changed.
- Diagnostics and Credits open already expanded in their own section.
- Livelier look: white cards on a blue-grey ground (dark cards on a darker ground in the dark theme), card icons in the accent colour, section icons on tinted tiles and a bolder title.
- The **AnyGest in Recents** and **Keep gestures alive** cards now explain the same thing the same way: swiping AnyGest away in Recents stops the gestures until the app is opened or the shortcut panel is pulled down, and either hiding it from Recents or Keep gestures alive (which protects it even while it shows there) avoids that.
- Each GitHub release now also carries the APK with its version in the name (`AnyGest-4.0.0.apk`, for example). The plain `anygest.apk` stays, so the README's download link keeps working.

### Added

- **Search box** on the home screen. It finds any feature by its name, by related words (for example "nubank" leads to Exception apps) or by its section, ignoring accents and case and taking several words. Tapping a result opens the right section, scrolls to the card and makes it glow for a moment; back returns to the results.

### Português

#### Alterado

- Nova tela inicial. O app tinha virado uma lista comprida de cards; agora ele abre numa caixa de busca, nos **Primeiros passos** e em cinco seções: **Gestos**, **Barra e atalhos**, **Avisos e app**, **Modo ADB** e **Diagnóstico e créditos**. Cada seção abre a própria tela com uma seta de voltar, e o gesto de voltar do Android retorna ao início. Todos os cards continuam lá e funcionam como antes; só mudou onde cada um mora.
- Diagnóstico e Créditos já abrem expandidos na seção deles.
- Visual mais vivo: cards brancos sobre um fundo cinza-azulado (cards escuros sobre um fundo mais escuro no tema escuro), ícones dos cards na cor de destaque, ícones das seções sobre quadrinhos coloridos e um título mais forte.
- Os cards **AnyGest nos Recentes** e **Manter os gestos vivos** agora explicam a mesma coisa do mesmo jeito: deslizar o AnyGest nos Recentes para os gestos até o app ser aberto ou o painel do atalho ser puxado, e esconder dos Recentes ou ligar Manter os gestos vivos (que protege o app mesmo aparecendo lá) evita isso.
- Cada release no GitHub agora traz também o APK com a versão no nome (`AnyGest-4.0.0.apk`, por exemplo). O `anygest.apk` de sempre continua, então o link de download do README segue funcionando.

#### Adicionado

- **Caixa de busca** na tela inicial. Ela encontra qualquer função pelo nome, por palavras relacionadas (por exemplo, "nubank" leva a Apps em exceção) ou pela seção, ignorando acentos e maiúsculas e aceitando várias palavras. Ao tocar num resultado, o app abre a seção certa, rola até o card e o destaca por um instante; voltar retorna aos resultados.

## 3.6.0

### Added

- ADB mode: Android now wakes AnyGest whenever the list of enabled accessibility services changes, even with the app's process dead and without any polling. With *Switch the gestures back on by itself* on, AnyGest then does what opening the app would do: switches the gestures back on (shows the button bar first if it was left hidden), except after the tile's off switch or a pause for an exception app. Before, this only happened when the app was opened, the tile panel pulled down or the phone rebooted. To switch the gestures off on purpose, use the Control Center tile, or turn that switch off first.
- ADB mode: the "gestures stopped" notification (opt-in alerts) has a **Switch back on** button that switches the gestures on directly, with no settings screen.
- Diagnostics: a **Copy log** button copies the app version and the log to the clipboard, ready to paste.
- **Keep gestures alive** card (off by default): swiping AnyGest away in Recents ends the app and the gestures, and nothing can bring them back by itself. With this on, AnyGest runs as a foreground service, which HyperOS leaves running. Android requires a notification for that; it is silent, at the lowest importance, and its icon is empty so nothing shows in the status bar. In ADB mode it has a **Pause** / **Switch on** button, like the tile, and the card has a **Hide notification** shortcut to the system page of that one notification, so the others (such as the stopped-gestures alert) stay on.

### Fixed

- After the app was closed from Recents, the Control Center tile could not switch the gestures back on: Android kept AnyGest in the list of enabled services but marked it as stopped, and writing the same list again changed nothing. Switching on now takes AnyGest out of the list and puts it back half a second later, which makes Android bind the service again. While it starts (a few seconds), the tile shows it as on and a tap on it does nothing, so a tap right after pulling the panel down no longer switches it off again.

### Português

#### Adicionado

- Modo ADB: o Android agora acorda o AnyGest sempre que a lista de serviços de acessibilidade ligados muda, mesmo com o processo do app morto e sem nenhuma verificação periódica. Com *Religar os gestos sozinho* ligado, o AnyGest faz o mesmo que ao abrir o app: religa os gestos (mostrando antes a barra de botões, se ela tiver ficado escondida), exceto depois do desligamento pelo atalho ou de uma pausa por app em exceção. Antes isso só acontecia ao abrir o app, puxar o painel do atalho ou reiniciar o celular. Para desligar os gestos de propósito, use o atalho do Centro de controle, ou desligue antes essa chave.
- Modo ADB: a notificação "gestos pararam" (avisos opcionais) ganhou o botão **Ligar de novo**, que religa os gestos direto, sem abrir tela de configurações.
- Diagnóstico: o botão **Copiar log** copia a versão do app e o registro para a área de transferência, pronto para colar.
- Card **Manter os gestos vivos** (desligado por padrão): deslizar o AnyGest nos Recentes encerra o app e os gestos, e nada consegue trazê-los de volta sozinho. Com isso ligado, o AnyGest roda como serviço em primeiro plano, que a HyperOS deixa rodando. O Android exige uma notificação para isso; ela é silenciosa, de prioridade mínima, e o ícone é vazio, então nada aparece na barra de status. No modo ADB ela tem o botão **Pausar** / **Ligar**, como o atalho, e o card tem o atalho **Esconder notificação** para a página do sistema só dessa notificação, para que as outras (como o aviso de gestos parados) continuem ligadas.

#### Corrigido

- Depois de fechar o app nos Recentes, o atalho do Centro de controle não conseguia religar os gestos: o Android mantinha o AnyGest na lista de serviços ligados, mas marcado como parado, e gravar a mesma lista de novo não mudava nada. Ao ligar, o AnyGest agora sai da lista e volta meio segundo depois, o que faz o Android acionar o serviço de novo. Enquanto ele sobe (alguns segundos), o atalho aparece como ligado e um toque nele não faz nada, então um toque logo depois de puxar o painel não desliga mais de novo.

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
