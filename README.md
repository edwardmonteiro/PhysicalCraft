# PhysicalCraft

**Explore. Experimente. Descubra.** Um jogo Android local de exploração e mistérios de física.

[Baixar APK](https://github.com/edwardmonteiro/PhysicalCraft/releases/download/v0.1.1/PhysicalCraft-v0.1.1.apk) · [Builds](https://github.com/edwardmonteiro/PhysicalCraft/actions/workflows/android.yml)

## Jogar no celular

1. Instale `PhysicalCraft-v0.1.1.apk` da página Releases. Requer Android 12+ e ARM64.
2. Comece a expedição. Use o controle esquerdo para andar e arraste à direita para olhar.
3. Siga a coluna verde até a ruína. Perto dela, toque em **Investigar ruína**.
4. Leia o objetivo, ajuste o instrumento e execute. A simulação mede o resultado.
5. Registre a descoberta para abrir a próxima ruína. O menu permite voltar à ruína atual.

`↑` pula; `+` e `−` alteram a superfície sob a mira. O caderno reúne os princípios já desbloqueados.

## O que está implementado

- Renderizador Android nativo OpenGL ES, escrito em Java. Sem Unity, Godot ou outro editor de engine.
- Terreno procedural por regiões de 16×16, criado em segundo plano e descartado quando distante.
- Coordenadas relativas à câmera no desenho para reduzir tremores de precisão. O mapa é virtualmente extenso; não é matematicamente infinito (índices inteiros limitados).
- Mundo de superfície em blocos: caminhar, pular, adicionar/remover camadas, colisão com terreno e alterações persistidas.
- Ruínas sucessivas e 13 modelos de física. Dois mistérios por tema, seguidos de expedições de revisão.
- Simulações instrumentais em diagramas 2D dentro das ruínas, sobre um mundo explorável 3D.
- Campanha procedural embarcada. Funciona em modo avião, sem modelo ou conta.
- Gemma via LiteRT-LM, executado em CPU local, com importação pelo seletor Android.
- Fases da IA são JSON: tema, história, semente, parâmetro, passo da solução e ambiente. O runtime valida e implanta a missão, incluindo a paleta do mundo.
- Geração automática após uma descoberta quando o modelo está instalado e a opção está ligada.
- Nome e ícone próprios; interface paisagem com margens para recorte e barras do sistema.

## Gemma: instalar uma vez, gerar offline

O APK inclui o runtime LiteRT-LM, mas **não inclui os pesos do Gemma**.

1. No menu **Gemma**, abra a página oficial vinculada.
2. Faça login no Hugging Face e aceite os termos Gemma, quando solicitado.
3. Baixe [`gemma3-1b-it-int4.litertlm`](https://huggingface.co/litert-community/Gemma3-1B-IT/blob/main/gemma3-1b-it-int4.litertlm), cerca de 584 MB.
4. Volte ao jogo e escolha **Importar arquivo .litertlm**. Reserve espaço para o download e a cópia interna.
5. Aguarde a cópia e a inicialização de validação. Só então o modelo substitui o anterior.
6. Use **Gerar e implantar novo mistério**, ou avance com geração automática ativada.

A aplicação não tem permissão de internet. O download inicial ocorre no navegador. Após a importação, inferência, geração, mundo e progresso são locais. Modelos `.task`/`.gguf` não são aceitos por este runtime.

Modelos pequenos podem emitir JSON inválido ou histórias incoerentes. Uma saída inválida preserva a missão atual e mostra um aviso. As equações, respostas e limites vêm do código, nunca da saída do modelo. A validação garante o domínio numérico e uma solução alcançável; não garante a qualidade pedagógica da prosa gerada. A geração pode levar tempo no CPU. O runtime é liberado após cada geração para devolver memória ao jogo.

## Progressão

| Capítulo | Modelo |
|---|---|
| Forças | F = ma; sem atrito |
| Queda | t = √(2h/g); sem arrasto |
| Lançamentos | alcance a 45°, alturas iguais |
| Energia | mgh = mv²/2; rampa ideal |
| Alavancas | equilíbrio de torques |
| Flutuação | empuxo com volume submerso |
| Eletricidade | resistor ôhmico V = RI |
| Calor | Q = mcΔT, sem mudança de fase |
| Ondas | v = fλ |
| Óptica | lente fina convergente |
| Órbitas | órbita circular terrestre |
| Relatividade | fator de Lorentz em referenciais inerciais |
| Quântica | energia máxima no efeito fotoelétrico |

A trilha vai de conceitos básicos a **introduções** a assuntos avançados; não representa um curso completo de física universitária. As animações são diagramas ilustrativos. Os resultados numéricos usam as equações documentadas. Há um instrumento variável por experimento nesta versão.

## Desenvolver usando somente GitHub + Android

Edite ou receba alterações neste repositório. Cada push em `main` inicia `.github/workflows/android.yml`. O GitHub instala o SDK, testa, compila, assina e publica o APK. Também é possível abrir **Actions → Android APK → Run workflow** pelo navegador do celular. Baixe o APK em Releases e instale.

Os arquivos Java ficam em `app/src/main/java/com/edward/physicalcraft/`. O pipeline usa os executáveis oficiais do SDK Android; Android Studio e Gradle não são necessários neste projeto. As dependências são baixadas de versões fixas no Maven oficial e verificadas por SHA-256.

### Build local opcional

```sh
export JAVA_HOME=/caminho/jdk-21
export ANDROID_JAR=/caminho/sdk/platforms/android-36/android.jar
export ANDROID_BUILD_TOOLS=/caminho/sdk/build-tools/36.0.0
python3 fetch-deps.py
./test.sh
./build-sdk.sh
```

A chave em `.dev/` é **privada e somente de desenvolvimento**, mantida fora do Git. O CI gera uma chave temporária por execução quando uma chave estável não é fornecida. APKs com assinaturas diferentes não atualizam a mesma instalação. Antes de distribuir atualizações contínuas, configure uma chave estável como segredo privado de CI. Para distribuição de produção, use chave privada externa via `PHYSICALCRAFT_KEYSTORE`, `PHYSICALCRAFT_KEY_ALIAS`, `PHYSICALCRAFT_KEY_PASS` (sintaxe apksigner `file:/caminho/segredo`, por exemplo). Nunca coloque chaves de produção neste repositório.

## Validação e limites desta entrega

- Compilação Java/DEX e empacotamento ARM64 concluídos; assinatura APK verificada.
- Testes de referência para as 13 equações, 5.200 missões alcançáveis, persistência de blueprint e rejeição de contratos inválidos.
- Testes de continuidade e limites de terreno em 65.536 coordenadas.
- **v0.1.1 executada em Android 12 (API 31) emulado:** abertura, pelo menos 30 quadros renderizados, experimento, menu Gemma, salvamento e reabertura aprovados no [teste automatizado](https://github.com/edwardmonteiro/PhysicalCraft/actions/runs/37159945835). FPS, UX em celulares físicos e inferência Gemma ainda exigem teste no aparelho.
- Cavernas, água física, inventário de materiais, física de corpos rígidos 3D, multijogador e criação arbitrária de mecânicas por IA não fazem parte da v0.1.
- Árvores e monumentos são decoração; a colisão atual é com o terreno. Modelos podem falhar por incompatibilidade ou memória disponível.

## Referências e créditos

- [Android OpenGL ES](https://developer.android.com/develop/ui/views/graphics/opengl/environment)
- [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM), Apache 2.0 e avisos de terceiros em `deps/`.
- [Gemma 3 1B LiteRT](https://huggingface.co/litert-community/Gemma3-1B-IT), pesos sob licença Gemma, não redistribuídos aqui.
- Runtime Kotlin, coroutines e Gson: suas licenças e avisos estão nos JARs e em `THIRD_PARTY.md`.
- Terreno, renderizador, interface, ícone e regras desta versão são implementações do PhysicalCraft. Não é um fork de Cubes/Kubi/Craft.

### Correção 0.1.1

O pipeline agora exige inicialização e renderização em Android 12 emulado antes da publicação. Há diagnóstico local copiável para falhas de inicialização; nada é enviado automaticamente. Se o app fechar inesperadamente, abra novamente para ver o diagnóstico.
