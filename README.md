# Auto Slides

O **Auto Slides** é uma aplicação desktop para Windows desenvolvida para automatizar a criação de apresentações PowerPoint a partir de informações presentes em documentos PDF.

A aplicação permite importar um ou mais PDFs, visualizar suas páginas, selecionar visualmente as regiões relevantes e associar cada recorte a um espaço previamente configurado em um template PowerPoint.

Ao finalizar o processo, o Auto Slides recorta as imagens, posiciona cada uma automaticamente no local correspondente do template, monta a apresentação, gera um preview dos slides e permite exportar o arquivo `.pptx` final.

O objetivo é substituir o processo manual de:

> abrir PDF → localizar informação → tirar print → abrir PowerPoint → encontrar o slide correto → posicionar a imagem

por um fluxo visual e automatizado.

---

## Fluxo da aplicação

O fluxo principal é:

1. Configurar um template PowerPoint.
2. Selecionar um ou mais arquivos PDF.
3. Visualizar as páginas dos PDFs dentro da aplicação.
4. Selecionar visualmente as áreas que devem ser utilizadas.
5. Escolher, para cada recorte, o espaço da apresentação onde ele deverá ser inserido.
6. Revisar as seleções realizadas.
7. Finalizar o processamento.
8. Visualizar o preview da apresentação já preenchida.
9. Remover slides que não devem fazer parte da versão final, se necessário.
10. Exportar a apresentação pronta em formato `.pptx`.

---

## Seleção de áreas

Os PDFs são convertidos temporariamente em imagens de alta resolução utilizando o Apache PDFBox.

O usuário pode então clicar e arrastar sobre qualquer página para criar uma área de seleção.

Cada seleção representa um recorte que será utilizado na apresentação.

As seleções são organizadas seguindo a estrutura:

```text
PDF
└── Página
    └── Seleção
```

A interface lateral permite acompanhar facilmente quais áreas já foram selecionadas.

Também é possível excluir uma seleção antes de finalizar o processamento.

---

## Seleção do destino

Depois de criar uma área, o Auto Slides apresenta uma lista contendo todos os espaços configurados no template PowerPoint.

Por exemplo:

```text
Renda anual
Patrimônio
Fluxo financeiro
Capacidade de aporte
Investimentos
Aposentadoria
```

O usuário não precisa saber em qual número de slide aquele conteúdo está localizado.

Ao selecionar:

```text
Patrimônio
```

o Auto Slides já conhece internamente:

```text
slide
posição X
posição Y
largura
altura
identificador do espaço
```

Essas informações são obtidas diretamente do template.

### Indicador de utilização

Quando um destino já foi utilizado, a lista apresenta uma indicação visual.

Exemplo:

```text
Renda anual                ✓ 2
Patrimônio
Fluxo financeiro           ✓ 1
Investimentos
```

O número representa quantas imagens já foram atribuídas àquele destino.

---

## Duplicação automática de slides

Um mesmo destino pode ser selecionado mais de uma vez.

Quando isso acontece, o Auto Slides cria automaticamente uma nova instância do slide correspondente.

Exemplo:

```text
1º crop → Renda anual
```

utiliza o slide original.

```text
2º crop → Renda anual
```

gera automaticamente uma cópia do slide e insere a segunda imagem nela.

```text
3º crop → Renda anual
```

gera outra cópia.

A ordem é preservada de acordo com:

```text
PDF
→ página
→ ordem da seleção
```

Dessa forma, não é necessário duplicar slides manualmente durante a montagem da apresentação.

---

# Template PowerPoint

Para que o Auto Slides consiga montar a apresentação automaticamente, é necessário utilizar um arquivo `.pptx` configurado.

O template pode conter normalmente:

- títulos;
- textos;
- logotipos;
- cores;
- elementos gráficos;
- fundos;
- rodapés;
- imagens fixas;
- demais elementos visuais.

Somente os locais destinados às imagens geradas pelo Auto Slides precisam de configuração especial.

---

## Criando um espaço para imagens

No PowerPoint:

1. Abra o template.
2. Vá até o slide desejado.
3. Selecione:

```text
Inserir
→ Formas
→ Retângulo
```

4. Crie o retângulo exatamente no local onde a imagem deverá aparecer.
5. Ajuste seu tamanho para representar a área final da imagem.

O Auto Slides utiliza a posição e as dimensões dessa forma como referência.

---

## Nomeando os espaços

Depois de criar a forma, abra o **Painel de Seleção** do PowerPoint.

Dependendo da versão:

```text
Página Inicial
→ Organizar
→ Painel de Seleção
```

ou:

```text
Formato da Forma
→ Painel de Seleção
```

Renomeie a forma utilizando o padrão:

```text
AUTO_SLOT::identificador::Nome exibido
```

Exemplo:

```text
AUTO_SLOT::renda-anual::Renda anual
```

Outro exemplo:

```text
AUTO_SLOT::patrimonio::Patrimônio
```

Ou:

```text
AUTO_SLOT::fluxo-financeiro::Fluxo financeiro
```

---

## Estrutura de um AUTO_SLOT

Considere:

```text
AUTO_SLOT::renda-anual::Renda anual
```

A estrutura é:

```text
AUTO_SLOT
    ↓
indica que a forma recebe uma imagem

renda-anual
    ↓
identificador interno

Renda anual
    ↓
nome apresentado ao usuário
```

O usuário vê apenas:

```text
Renda anual
```

Os demais dados são utilizados internamente pela aplicação.

---

## Exemplo de template

Um template poderia possuir:

```text
Slide 1
Capa

Slide 2
Introdução

Slide 3
Rendas e despesas
└── AUTO_SLOT::rendas-despesas::Rendas e despesas

Slide 4
Fluxo financeiro
└── AUTO_SLOT::fluxo-financeiro::Fluxo financeiro

Slide 5
Patrimônio
├── AUTO_SLOT::patrimonio-composicao::Composição do patrimônio
└── AUTO_SLOT::patrimonio-investimentos::Investimentos

Slide 6
Aposentadoria
└── AUTO_SLOT::aposentadoria::Aposentadoria
```

Slides sem `AUTO_SLOT` continuam normalmente na apresentação e não recebem imagens automaticamente.

---

## Leitura automática do template

Quando um template é selecionado, o Auto Slides utiliza o Apache POI para analisar o arquivo `.pptx`.

São identificados:

- quantidade de slides;
- tamanho da apresentação;
- títulos;
- formas;
- espaços `AUTO_SLOT`;
- posição dos espaços;
- largura;
- altura.

A aplicação gera uma configuração interna equivalente a:

```text
Template
└── Slides
    └── Slots
        ├── identificador
        ├── nome
        ├── posição X
        ├── posição Y
        ├── largura
        └── altura
```

O usuário não precisa editar manualmente essa configuração.

---

# Processamento das imagens

Ao finalizar a seleção:

```text
PDF
↓
página renderizada
↓
área selecionada
↓
crop
```

Os crops são gerados em uma pasta temporária.

O Auto Slides mantém a relação entre:

```text
crop
+
PDF de origem
+
página
+
ordem da seleção
+
slide de destino
+
AUTO_SLOT
```

Essas informações são utilizadas durante a montagem automática da apresentação.

---

# Geração do PowerPoint

A apresentação final é construída utilizando o Apache POI.

O processo ocorre aproximadamente assim:

```text
Template PPTX
        ↓
leitura dos slides
        ↓
leitura dos AUTO_SLOT
        ↓
crops selecionados
        ↓
associação crop → destino
        ↓
duplicação automática quando necessária
        ↓
inserção das imagens
        ↓
PowerPoint preenchido
```

A imagem é inserida respeitando as dimensões definidas pelo template.

---

# Preview da apresentação

Depois da geração, o Auto Slides exibe uma tela de preview contendo os slides já preenchidos.

O preview mostra o resultado real da apresentação, incluindo as imagens inseridas.

O usuário pode navegar pelos slides antes de exportar o arquivo final.

---

## Remoção de slides

Durante o preview, slides que não devem fazer parte da apresentação podem ser removidos.

A remoção inicialmente é apenas visual.

Isso permite desfazer a operação antes da exportação definitiva.

Também é possível restaurar uma remoção utilizando a funcionalidade de desfazer.

Os slides são efetivamente removidos apenas quando o arquivo final é exportado.

---

# Exportação

Ao concluir a revisão, o usuário pode exportar a apresentação.

O resultado é um arquivo:

```text
.pptx
```

compatível com Microsoft PowerPoint e demais aplicativos capazes de abrir apresentações Office Open XML.

---

# Persistência do template

O Auto Slides mantém registrado o último template utilizado.

Assim, normalmente o usuário precisa configurar o PowerPoint apenas na primeira utilização.

Nas próximas execuções, o template anteriormente selecionado é reutilizado automaticamente, desde que o arquivo continue disponível.

O template também pode ser substituído posteriormente através da tela de configurações.

---

# Navegação

A tela de seleção possui recursos adicionais para facilitar a navegação em documentos extensos.

### Scroll tradicional

A roda do mouse permite navegar normalmente pelas páginas.

### Navegação pelo botão central

O botão central ativa uma navegação semelhante ao auto-scroll de navegadores.

A velocidade depende da distância entre o cursor e o ponto inicial.

### Navegação com o botão direito

Também é possível manter o botão direito pressionado e arrastar rapidamente a área de visualização.

Esse modo possui velocidade maior e é útil para percorrer documentos longos.

---

# Arquivos temporários

Durante o processamento, o Auto Slides cria um workspace temporário contendo:

- páginas renderizadas;
- crops;
- configurações intermediárias;
- apresentação em processamento.

Os arquivos PDF originais nunca são modificados.

Os dados temporários são removidos ao encerrar ou limpar o workspace da aplicação.

---

# Recursos

- Importação de múltiplos PDFs.
- Drag and drop.
- Renderização de PDFs em alta resolução.
- Seleção visual de áreas.
- Várias seleções por página.
- Exclusão individual de áreas.
- Árvore de organização por PDF e página.
- Template PowerPoint configurável.
- Detecção automática de espaços `AUTO_SLOT`.
- Lista simplificada de destinos.
- Indicador de destinos já utilizados.
- Duplicação automática de slides.
- Recorte automático das imagens.
- Inserção automática dos crops no PowerPoint.
- Preview da apresentação preenchida.
- Remoção de slides antes da exportação.
- Desfazer remoção.
- Exportação do `.pptx` final.
- Persistência do último template utilizado.
- Interface desktop JavaFX personalizada.
- Barra de janela personalizada.
- Popups e alertas personalizados.

---

# Tecnologias

O projeto utiliza:

- Java 25
- JavaFX
- Gradle
- Apache PDFBox
- Apache POI
- Jackson
- Lombok
- Log4j
- Ikonli

---

# Arquitetura geral

O fluxo principal pode ser representado como:

```text
PDF
 │
 ▼
Apache PDFBox
 │
 ▼
páginas renderizadas
 │
 ▼
seleções do usuário
 │
 ▼
crops
 │
 ▼
SelectionAssignment
 │
 ├── origem
 │
 └── destino
 │
 ▼
TemplateLayout
 │
 ▼
Apache POI
 │
 ▼
PowerPoint preenchido
 │
 ▼
Preview
 │
 ▼
Exportação
```

---

# Download

O Auto Slides pode ser distribuído como instalador para Windows através da página de Releases:

[Releases do projeto](https://github.com/DanielCamposSantos/auto-slides/releases)

Para instalar:

1. Acesse a página de **Releases**.
2. Abra a versão desejada.
3. Em **Assets**, baixe o instalador `.exe`.
4. Execute o arquivo.
5. Siga as instruções do instalador.
6. Abra o Auto Slides através do Menu Iniciar ou do atalho criado.

O instalador inclui o runtime necessário para a aplicação, portanto o usuário final não precisa instalar manualmente o Java.

> Dependendo das políticas de segurança do Windows, um aviso pode ser exibido para executáveis ainda não assinados digitalmente. Confirme sempre que o arquivo foi obtido diretamente da página oficial do projeto.

---

# Desenvolvimento local

Para executar o projeto a partir do código-fonte é necessário ter o **JDK 25** instalado.

Clone o repositório:

```powershell
git clone https://github.com/DanielCamposSantos/auto-slides.git
```

Entre no diretório:

```powershell
cd auto-slides
```

Execute:

```powershell
.\gradlew.bat run
```

Ou:

```powershell
.\gradlew run
```

---

## Build

Para compilar:

```powershell
.\gradlew clean build
```

Para gerar a distribuição da aplicação:

```powershell
.\gradlew clean installDist
```

A distribuição será criada em:

```text
build/install/auto-slides/
```

---

# Empacotamento para Windows

O projeto pode ser empacotado utilizando o `jpackage` disponível no JDK.

Para criar instaladores Windows, também é necessário possuir o WiX Toolset compatível configurado no ambiente.

O processo de distribuição utiliza:

```text
Gradle
↓
installDist
↓
jpackage
↓
runtime Java
↓
instalador Windows
```

---

# Objetivo do projeto

O Auto Slides foi criado para automatizar tarefas repetitivas de preparação de apresentações baseadas em relatórios.

Embora seu primeiro caso de uso seja relacionado a relatórios financeiros, a arquitetura foi criada para que qualquer template PowerPoint configurado com `AUTO_SLOT` possa utilizar o mesmo mecanismo.

Assim, o projeto não depende de um formato específico de relatório.

O usuário define:

```text
o que recortar
```

e o template define:

```text
onde inserir
```

O Auto Slides executa automaticamente o restante do processo.

---

# Licença

Este repositório ainda não possui uma licença definida.