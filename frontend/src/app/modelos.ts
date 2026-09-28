/** Tipos que espelham a API do backend. */

export interface Parte {
  nome: string | null;
  documento: string | null;
}

export interface Item {
  descricao: string | null;
  quantidade: number | null;
  valorUnitario: number | null;
  valorTotal: number | null;
}

export interface NotaFiscal {
  chaveAcesso: string | null;
  numero: string | null;
  serie: string | null;
  dataEmissao: string | null;
  emitente: Parte | null;
  destinatario: Parte | null;
  itens: Item[] | null;
  valorProdutos: number | null;
  valorFrete: number | null;
  valorDesconto: number | null;
  valorTotal: number | null;
}

export interface Boleto {
  linhaDigitavel: string | null;
  bancoImpresso: string | null;
  nomeBancoImpresso: string | null;
  beneficiario: Parte | null;
  pagador: Parte | null;
  vencimento: string | null;
  valorDocumento: number | null;
  nossoNumero: string | null;
}

export type Status = 'OK' | 'ALERTA' | 'ERRO';

export interface Verificacao {
  campo: string;
  status: Status;
  mensagem: string;
}

export interface Resultado {
  tipo: 'NOTA_FISCAL' | 'BOLETO' | 'OUTRO' | null;
  situacao: 'CONFERIDO' | 'REVISAR' | 'INCONSISTENTE';
  dados: { tipo: string; notaFiscal: NotaFiscal | null; boleto: Boleto | null; observacoes: string[] | null };
  verificacoes: Verificacao[];
  duracaoMs: number;
}

export interface Info {
  provedor: string;
  modelo: string | null;
}
