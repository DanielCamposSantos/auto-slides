# Auto PDF

O **Auto PDF** transforma informações de documentos PDF em imagens prontas para serem usadas em apresentações. A aplicação permite abrir um ou mais PDFs, marcar visualmente as áreas relevantes de cada página e gerar os recortes que serão inseridos em um slide personalizado.

É uma ferramenta pensada para agilizar a criação de apresentações quando o conteúdo de origem já está em PDFs — evitando capturas de tela manuais e mantendo cada recorte organizado por documento e página.

## Funcionalidade principal

O fluxo central do Auto PDF é:

1. Selecionar um ou mais arquivos PDF.
2. Visualizar as páginas renderizadas em alta resolução.
3. Desenhar áreas de seleção sobre os trechos que devem virar imagens.
4. Remover ou ajustar as seleções antes da finalização.
5. Usar os recortes gerados para compor um slide personalizado.

Cada seleção representa um **print de uma área do PDF**. As áreas são agrupadas por PDF e por página, o que facilita conferir tudo antes de montar a apresentação.

## Recursos

- Importação de múltiplos PDFs pelo seletor de arquivos ou por arrastar e soltar.
- Visualização das páginas do PDF na aplicação.
- Criação de várias áreas de recorte por página.
- Exclusão rápida de uma seleção.
- Organização das seleções em árvore por documento e página.
- Processamento temporário dos arquivos, sem alterar os PDFs originais.

## Como baixar o executável

O Auto PDF será distribuído como um executável para Windows (`.exe`) pela página de [Releases do projeto](https://github.com/DanielCamposSantos/auto-pdf/releases).

1. Acesse a página de **Releases**.
2. Abra a versão mais recente.
3. Em **Assets**, baixe o arquivo com extensão `.exe`.
4. Execute o arquivo baixado e siga as instruções do Windows.

> Caso o Windows exiba um aviso de segurança, confira se o arquivo foi baixado diretamente da página oficial de Releases antes de prosseguir.

## Uso rápido

1. Abra o Auto PDF.
2. Clique para selecionar os PDFs ou arraste os arquivos para a área indicada.
3. Confira a lista de documentos e clique em **Continuar**.
4. Nas páginas exibidas, clique e arraste para delimitar cada área que deve ser capturada.
5. Revise a árvore de seleções e remova as áreas desnecessárias pelo botão `×`.
6. Finalize para utilizar os recortes no seu slide personalizado.

## Tecnologias

- Java 25
- JavaFX
- Apache PDFBox
- Apache POI
- Gradle

## Desenvolvimento local

Para executar o projeto a partir do código-fonte, é necessário ter o JDK 25 instalado.

```powershell
git clone https://github.com/DanielCamposSantos/auto-pdf.git
cd auto-pdf
.\gradlew.bat run
```

Para executar os testes:

```powershell
.\gradlew.bat test
```

## Licença

Este repositório ainda não possui uma licença definida.
