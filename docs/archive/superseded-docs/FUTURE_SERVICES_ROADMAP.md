# Future Services Roadmap

Avaliação em 03/11/2026. Serviços não aprovados ficam como pesquisa; não criar módulos ou infraestrutura vazios.

## Portfólio de oportunidades

Notas 0–100 com os pesos da missão. Segurança/manutenção altas significam menor risco/custo.

| Serviço | Valor | Diferenciação | Viabilidade | Comercial | Segurança | Manutenção | Total | Fase/decisão |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| Storage Forecast local | 21 | 15 | 18 | 9 | 14 | 5 | 82 | Próxima entrega do núcleo, condicionado a histórico mínimo e validação |
| Similaridade visual local | 21 | 16 | 13 | 11 | 10 | 3 | 74 | Protótipo revisável; sem apagar automaticamente |
| Compartilhamento/transferência local | 17 | 10 | 17 | 8 | 13 | 4 | 69 | Pesquisa posterior; preferir Sharesheet/Quick Share existentes |
| Gerenciador de documentos via SAF | 20 | 8 | 15 | 10 | 11 | 4 | 68 | Candidata; começa por downloads/documentos escolhidos |
| Compressão de mídia com original preservado | 16 | 12 | 12 | 10 | 9 | 3 | 62 | Backlog até benchmark de qualidade, espaço temporário e tempo |
| Cofre privado criptografado | 18 | 14 | 9 | 12 | 4 | 2 | 59 | Não implementar no MVP; key management/recuperação tornam risco alto |
| Backup em nuvem próprio | 20 | 7 | 7 | 12 | 7 | 1 | 54 | Rejeitar por agora: backend, custos recorrentes, abuso e responsabilidade |

Oportunidade com score alto não é autorização automática: riscos críticos de dados, Play ou suporte anulam a pontuação.

## Sequência

### Núcleo

Concluir mídia e limpeza segura, cobrir todas as páginas/volumes em hash global com limite de recursos, executar consentimento e reconciliação em aparelhos reais, criar history de análises mínimo.

### Próximo produto

Storage Forecast deve usar somente amostras locais de `StatFs`, com requisitos em [MIDIASTORE_PRODUCT_STRATEGY.md](MIDIASTORE_PRODUCT_STRATEGY.md). Sem histórico suficiente, mostrar “ainda não há dados”, nunca uma estimativa fabricada. Amostras não significam uso exato por aplicativo; explicar limitações.

### Expansão

- **Smart File Manager:** árvore/categorias construídas por MediaStore e SAF; sem tentar entrar em áreas privadas de outros apps.
- **Media Intelligence:** semelhança visual e baixa qualidade como filtros para revisão humana; expor confiança, prévia e custo de processamento.
- **Transferência:** usar APIs/integrações do Android que já estão disponíveis; não construir protocolo de rede próprio sem demanda demonstrada e modelagem de segurança.
- **Otimização:** materializar cópia em arquivo separado, validar decodificação/tamanho e deixar original intacto até confirmação explícita.
- **Vault/backup:** só com threat model, design de chaves/recuperação, política de exclusão, avaliação legal, orçamento operacional e teste de restauração.

## Relação com a marca

Midiastore permanece o produto de armazenamento/mídia. Serviços independentes podem receber marca própria quando houver equipe, proposta e ciclo de vida independentes. Reutilizar contratos/dados locais por interfaces; não transformar o app em launcher de ferramentas aleatórias nem comprar domínios/branding antes da validação.

## Priorização revisada após o mercado — 04/11/2026

1. Estabilizar e validar Phone Cleaner: CI do HEAD, varredura, SHA-256, revalidação, consentimento e prova pós-limpeza.
2. Completar um ledger de evidências para explicar cobertura, motivo de cada sugestão, original preservado e bytes realmente recuperados.
3. Criar Storage History/Forecast local depois que houver amostras suficientes; sem previsão antes de pelo menos três observações ao longo de sete dias.
4. Prototipar similaridade visual apenas após corpus consentido, benchmark de falso positivo, limite de recursos e revisão humana. SD Maid já oferece recurso perceptual.
5. Adicionar Downloads/documentos via SAF com seleção explícita; não converter o app em gerenciador universal no primeiro release.
6. Automatização analítica com WorkManager somente após opt-in e teste de bateria; nunca excluir conteúdo pessoal automaticamente.

Cofre, nuvem/backup, transferência e IA local permanecem em pesquisa futura. Backup exige segurança, recuperação, custo recorrente e operação contínua; não implementar na etapa de recuperação.
